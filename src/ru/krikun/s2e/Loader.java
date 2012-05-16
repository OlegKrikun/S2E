/*
 * Copyright (C) 2012 OlegKrikun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.krikun.s2e;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class Loader extends AsyncTask<Void, Void, Void> {
    //Path to bin dir
    private static final String PATH_BIN =
            "/data/local/bin";
    //Path to installed script
    private static final String PATH_SCRIPT =
            "/data/local/userinit.d/simple2ext";
    //Path to tune2fs and e2fsck
    private static final String PATH_TUNE2FS =
            PATH_BIN + "/tune2fs";
    private static final String PATH_E2FSCK =
            PATH_BIN + "/e2fsck";
    //Shell command for make userinit dir
    private static final String SHELL_MAKE_USERINIT_DIR =
            "if [ ! -e /data/local/userinit.d ]; then busybox install -m 777 -o 1000 -g 1000 -d /data/local/userinit.d; fi";
    //Shell command for make bin dir
    private static final String SHELL_MAKE_BIN_DIR =
            "busybox install -m 777 -o 1000 -g 1000 -d /data/local/bin";
    //Shell command for copy script from app home to userinit.d
    private static final String SHELL_COPY_SCRIPT_TO_USERINIT_DIR =
            "busybox cp " + Helper.S2E_DIR + "/files/script01.sh " + PATH_SCRIPT;
    //Shell commands for copy tune2fs and e2fsck from app home to bin
    private static final String SHELL_COPY_TUNE2FS_TO_BIN_DIR =
            "busybox cp " + Helper.S2E_DIR + "/files/tune2fs " + PATH_TUNE2FS;
    private static final String SHELL_COPY_E2FSCK_TO_BIN_DIR =
            "busybox cp " + Helper.S2E_DIR + "/files/e2fsck " + PATH_E2FSCK;
    //Shell command for set permission to script
    private static final String SHELL_SET_PERMISSION =
            "busybox chmod 0777 ";
    //Shell command for get md5 sum
    private static final String SHELL_GET_MD5 =
            "busybox md5sum " + PATH_SCRIPT;

    private final App app;
    private final ProgressDialog dialog;
    private final Main main;
    private final String md5;


    public Loader(Main main) {
        this.main = main;

        app = App.getInstance();

        //Getting md5 from resources
        md5 = App.getRes().getString(R.string.script_md5);

        dialog = new ProgressDialog(main);
    }

    @Override
    protected Void doInBackground(Void... voids) {

        //Check API
        app.loadAPI();

        //Stop if API not supported
        if (app.isSupportedOS()) {

            //Check shell
            app.loadShell();

            //Stop if root, busybox not available or access not given
            if (app.isRoot()) {

                //Check script
                if (!checkScript()) {

                    //Installing script
                    copyFileToHome("script01.sh");
                    //Final actions of script installation
                    //Check /data/local/userinit.d exists and create this if needed
                    //Copy script and set permission
                    Helper.sendShell(SHELL_MAKE_USERINIT_DIR);
                    Helper.sendShell(SHELL_COPY_SCRIPT_TO_USERINIT_DIR);
                    Helper.sendShell(SHELL_SET_PERMISSION  + PATH_SCRIPT);

                    //Check script, again
                    if (checkScript()) {

                        //Set "Script Installed"
                        app.setScriptInstalled(true);
                    }
                } else {

                    //Set "Script Installed"
                    app.setScriptInstalled(true);
                }

                //Check /data/local/bin exists and create this if needed
                if(!Helper.checkFileExists(PATH_BIN))
                    Helper.sendShell(SHELL_MAKE_BIN_DIR);

                if (!Helper.checkFileExists(PATH_TUNE2FS)) {
                    //Install tune2fs
                    copyFileToHome("tune2fs");
                    //Copy tune2fs and set permission
                    Helper.sendShell(SHELL_COPY_TUNE2FS_TO_BIN_DIR);
                    Helper.sendShell(SHELL_SET_PERMISSION  + PATH_TUNE2FS);
                }

                if (!Helper.checkFileExists(PATH_E2FSCK)) {
                    //Install e2fsck
                    copyFileToHome("e2fsck");
                    //Copy e2fsck and set permission
                    Helper.sendShell(SHELL_COPY_E2FSCK_TO_BIN_DIR);
                    Helper.sendShell(SHELL_SET_PERMISSION  + PATH_E2FSCK);
                }

                //Load targets set (sizes, statuses and etc)
                app.loadTargets();

                //Load partitions set
                app.loadPartitions();
            }
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //Set message to progress dialog
        dialog.setMessage(App.getRes().getString(R.string.loading));
        //Show progress dialog
        dialog.show();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        //Close progress dialog
        if (dialog.isShowing()) dialog.dismiss();
        //Exit if OS not supported
        if (!app.isSupportedOS()) main.showAlert(App.getRes().getString(R.string.alert_version));
        //Exit if access to root not given or busybox not found
        else if (!app.isRoot()) main.showAlert(App.getRes().getString(R.string.alert_root));
        //Exit if script not installed
        else if (!app.isScriptInstalled()) main.showAlert(App.getRes().getString(R.string.alert_script_title));
        //Load main view
        else main.onTaskFinished();
    }

    //Check script exists and control md5
    private boolean checkScript() {
        return checkScriptExists() && checkScriptSum();
    }

//    //Final actions of script installation
//    //Check /data/local/userinit.d exists and create this if needed
//    //Copy script and set permission
//    private void copyScriptToUserInit() {
//        Helper.sendShell(SHELL_MAKE_USERINIT_DIR);
//        Helper.sendShell(SHELL_COPY_SCRIPT_TO_USERINIT_DIR);
//        Helper.sendShell(SHELL_SET_PERMISSION  + PATH_SCRIPT);
//    }

    //Final actions of tune2fs installation
    //Check /data/local/bin exists and create this if needed
    //Copy tune2fs and set permission
    private void copyTune2FsToBin() {
        Helper.sendShell(SHELL_MAKE_BIN_DIR);
        Helper.sendShell(SHELL_COPY_TUNE2FS_TO_BIN_DIR);
        Helper.sendShell(SHELL_SET_PERMISSION  + PATH_TUNE2FS);
    }

    //Return md5sum of path
    private String getSum() {
        List<String> output = Helper.sendShell(SHELL_GET_MD5);
        if (output != null) {
            if (output.get(0).length() >= 32) return output.get(0).substring(0, 32);
        }
        return null;
    }

    //Check script exists
    private boolean checkScriptExists() {
        return Helper.checkFileExists(PATH_SCRIPT);
    }

    //Compare md5sum
    private boolean checkScriptSum() {
        String tmp_md5 = getSum();
        return tmp_md5 != null && tmp_md5.equals(md5);
    }

    //Extract file and copy to app home dir
    private void copyFileToHome(String fileName) {
        try {
            InputStream in = App.getRes().getAssets().open(fileName);
            FileOutputStream out = app.openFileOutput(fileName, 0);
            if (in != null) {
                int size = in.available();
                byte[] buffer = new byte[size];
                in.read(buffer);
                out.write(buffer, 0, size);
                in.close();
                out.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
