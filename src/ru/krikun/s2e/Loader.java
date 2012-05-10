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

    //Path to installed script
    private static final String SCRIPT_DIST =
            "/data/local/userinit.d/simple2ext";
    //Shell command for make userinit dir
    private static final String SHELL_MAKE_USERINIT_DIR =
            "if [ ! -e /data/local/userinit.d ]; then busybox install -m 777 -o 1000 -g 1000 -d /data/local/userinit.d; fi";
    //Shell command for copy script from app home to userinit.d
    private static final String SHELL_COPY_SCRIPT_TO_USERINIT_DIR =
            "busybox cp " + Helper.S2E_DIR + "/files/script01.sh " + SCRIPT_DIST;
    //Shell command for set permission to script
    private static final String SHELL_SET_PERMISSION =
            "busybox chmod 0777 " + SCRIPT_DIST;
    //Shell command for get md5 sum
    private static final String SHELL_GET_MD5 =
            "busybox md5sum " + SCRIPT_DIST;

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
                    copyScriptToHome();
                    copyScriptToUserInit();

                    //Check script, again
                    if (checkScript()) {

                        //Set "Script Installed"
                        app.setScriptInstalled(true);
                    }
                } else {

                    //Set "Script Installed"
                    app.setScriptInstalled(true);
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

    //Final actions of script installation
    //Check /data/local/userinit.d exists and create this if needed
    //Copy script and set permission
    private void copyScriptToUserInit() {
        Helper.sendShell(SHELL_MAKE_USERINIT_DIR);
        Helper.sendShell(SHELL_COPY_SCRIPT_TO_USERINIT_DIR);
        Helper.sendShell(SHELL_SET_PERMISSION);
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
        return Helper.checkFileExists(SCRIPT_DIST);
    }

    //Compare md5sum
    private boolean checkScriptSum() {
        String tmp_md5 = getSum();
        return tmp_md5 != null && tmp_md5.equals(md5);
    }

    //Extract script and copy to app home dir
    private void copyScriptToHome() {
        try {
            InputStream in = App.getRes().getAssets().open("script01.sh");
            FileOutputStream out = app.openFileOutput("script01.sh", 0);
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
