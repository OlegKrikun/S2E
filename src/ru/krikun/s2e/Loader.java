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
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class Loader extends AsyncTask<Void, Void, Void> {
    //Path to userinit dir
    private static final String PATH_USERINIT = "/data/local/userinit.d";
    //Path to bin dir
    private static final String PATH_BIN = "/data/local/bin";
    //Files names of script, tune2fs and e2fsck
    private static final String SCRIPT = "simple2ext";
    private static final String TUNE2FS = "tune2fs";
    private static final String E2FSCK = "e2fsck";
    //Shell command templates for make dir
    private static final String SHELL_MAKE_DIR = "busybox install -m 777 -o 1000 -g 1000 -d ";
    //Shell commands templates for copy tools from app home to bin
    private static final String SHELL_COPY_FILE = "busybox cp ";
    //Shell command templates for set permission to script
    private static final String SHELL_SET_PERMISSION = "busybox chmod 0777 ";
    //Shell command for get md5 sum
    private static final String SHELL_GET_MD5 = "busybox md5sum " + PATH_USERINIT + "/" + SCRIPT;

    private final App app;
    private ProgressDialog dialog;
    private final Main main;
    private final String md5;


    public Loader(Main main) {
        this.main = main;
        app = App.getInstance();

        //Getting md5 from resources
        md5 = App.getRes().getString(R.string.script_md5);
        //Create ProgressDialog
        dialog = new ProgressDialog(main);
    }

    @Override
    protected Void doInBackground(Void... voids) {

        //Check API
        app.loadAPI();
        if (app.isSupportedOS()) {

            //Check shell
            app.loadShell();
            if (app.isRoot()) {

                //Check script
                if (!checkScript()) {
                    //Installing script to home dir
                    copyFileToHome(SCRIPT);
                    //Check /data/local/userinit.d exists and create this if needed
                    makeDirShell(PATH_USERINIT);
                    //Copy script and set permission
                    copyFileShell(SCRIPT, PATH_USERINIT);
                    //Check script again and set "Script Installed" if script exists
                    if (checkScript())
                        app.setScriptInstalled(true);
                } else {
                    //Set "Script Installed"
                    app.setScriptInstalled(true);
                }

                //Check /data/local/bin exists and create this if needed
                makeDirShell(PATH_BIN);
                //Install tune2fs to home, copy to bin and set permission
                installTool(TUNE2FS);
                //Install e2fsck to home, copy to bin and set permission
                installTool(E2FSCK);

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
        if (dialog.isShowing()) {
            try {
                dialog.dismiss();
                dialog = null;
            } catch (Exception e) {
                Log.e(Helper.TAG, "Error in dialog.dismiss()");
            }
        }
//        //Exit if OS not supported
//        if (!app.isSupportedOS()) main.showAlert(App.getRes().getString(R.string.alert_version));
//        //Exit if access to root not given or busybox not found
//        else if (!app.isRoot()) main.showAlert(App.getRes().getString(R.string.alert_root));
//        //Exit if script not installed
//        else if (!app.isScriptInstalled()) main.showAlert(App.getRes().getString(R.string.alert_script_title));
//        //Load main view
//        else main.onTaskFinished();
    }

    //Check script exists and control md5
    private boolean checkScript() {
        return Helper.checkFileExists(PATH_USERINIT + "/" + SCRIPT) && checkScriptSum();
    }

    //Return md5sum of path
    private String getSum() {
        List<String> output = Helper.sendShell(SHELL_GET_MD5);
        if (output != null) {
            if (output.get(0).length() >= 32) return output.get(0).substring(0, 32);
        }
        return null;
    }

    //Compare md5sum
    private boolean checkScriptSum() {
        String tmp_md5 = getSum();
        return tmp_md5 != null && tmp_md5.equals(md5);
    }

    //Install tool to bin dir
    private void installTool(String toolName) {
        if (!Helper.checkFileExists(PATH_BIN + "/" + toolName)) {
            //Install tool to home
            copyFileToHome(toolName);
            //Copy tool to bin and set permission
            copyFileShell(toolName, PATH_BIN);
        }
    }

    //Copy tools to bin and set permission
    private void copyFileShell(String fileName, String destinationDir) {
        Helper.sendShell(SHELL_COPY_FILE + main.getFilesDir().getAbsolutePath() + "/" + fileName + " " + destinationDir + "/" + fileName);
        Helper.sendShell(SHELL_SET_PERMISSION  + destinationDir + "/" + fileName);
    }

    //Make dir via shell
    private void makeDirShell(String dirPath) {
        if(!Helper.checkFileExists(dirPath))
            Helper.sendShell(SHELL_MAKE_DIR + dirPath);
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
