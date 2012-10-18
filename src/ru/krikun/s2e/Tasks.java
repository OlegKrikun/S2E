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
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import com.stericson.RootTools.RootTools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class Tasks {

    private final App app;
    private ProgressDialog dialog;
    private final Main main;
    private final String md5;

    private boolean supportedOS;
    private boolean scriptInstalled;

    public Tasks(Main main) {
        this.main = main;
        app = App.getInstance();

        //Show progress dialog
        dialog = new ProgressDialog(main);
        dialog.show();

        //Getting md5 from resources
        md5 = App.getRes().getString(R.string.script_md5);
    }

    void doInitialization() {
        new CheckerAPI().execute();
    }

    void doRefresh() {
        new Refresher().execute();
    }

    //Checking API version
    private class CheckerAPI extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD
                    || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD_MR1) {
                supportedOS = true;
            } else if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH
                    || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                    || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
                app.setIce(true);
                supportedOS = true;
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(App.getRes().getString(R.string.checking_api));
            supportedOS = false;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //Exit if OS not supported
            if (!supportedOS) exit(App.getRes().getString(R.string.alert_version));
            //Run CheckerShell
            else new CheckerShell().execute();
        }
    }

    //Checking root and open shell
    private class CheckerShell extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            App.setShell(new Shell());
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(App.getRes().getString(R.string.checking_root));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //Exit if access to root not given or busybox not found
            if (!App.getShell().isRoot()) exit(App.getRes().getString(R.string.alert_root));
            else new Installer().execute();
        }
    }

    private class Installer extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (!checkScript()) {
                //Installing script to home dir
                extractFile(App.SCRIPT);
                //Check /data/local/userinit.d exists and create this if needed
                makeDirShell(App.PATH_USERINIT);
                //Copy script and set permission
                installFile(App.SCRIPT, App.PATH_USERINIT);
                //Check script again and set "Script Installed" if script exists
                if (checkScript())
                    scriptInstalled = true;
            } else {
                scriptInstalled = true;
            }
            if (!toolsExists()) {
                //Check /data/local/bin exists and create this if needed
                makeDirShell(App.PATH_BIN);
                //Install tune2fs to home, copy to bin and set permission
                installTool(App.TUNE2FS);
                //Install e2fsck to home, copy to bin and set permission
                installTool(App.E2FSCK);
                //Install logger to home, copy to bin and set permission
                installTool(App.LOGGER);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(App.getRes().getString(R.string.installing));
            scriptInstalled = false;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //Exit if script not installed
            if (!scriptInstalled) exit(App.getRes().getString(R.string.alert_script_title));
            //Run Loader
            else new Loader().execute();
        }
    }

    private class Loader extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            //Load targets set (sizes, statuses and etc)
            app.loadTargets();
            //Load partitions set
            app.loadPartitions();
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(App.getRes().getString(R.string.loading));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //Close progress dialog
            closeProgressDialog();
            //Add and update main view
            main.addView();
            main.updateView();
        }
    }

    private class Refresher extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            app.getPartitions().update();
            app.getTargets().updateSizes();
            app.getTargets().updateStatuses();
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(App.getRes().getString(R.string.refreshing));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //Close progress dialog
            closeProgressDialog();
            //Update main view
            main.updateView();
        }
    }

    //Check script exists and control md5
    private boolean checkScript() {
        return App.checkFileExists(App.PATH_USERINIT + App.SEPARATOR + App.SCRIPT) && checkScriptSum();
    }

    //Compare md5sum
    private boolean checkScriptSum() {
        String tmp_md5 = getSum();
        return tmp_md5 != null && tmp_md5.equals(md5);
    }

    //Return md5sum of path
    private String getSum() {
        List<String> output = App.getShell().run(App.SHELL_GET_MD5);
        if (output != null) {
            if (output.get(0).length() >= 32) return output.get(0).substring(0, 32);
        }
        return null;
    }

    //Check tools exists
    private boolean toolsExists() {
        return App.checkFileExists(App.PATH_BIN + App.SEPARATOR + App.TUNE2FS) &&
                App.checkFileExists(App.PATH_BIN + App.SEPARATOR + App.E2FSCK) &&
                App.checkFileExists(App.PATH_BIN + App.SEPARATOR + App.LOGGER);
    }

    //Install tool to bin dir
    private void installTool(String toolName) {
        //Install tool to home
        extractFile(toolName);
        //Copy tool to bin and set permission
        installFile(toolName, App.PATH_BIN);
    }

    //Copy and set permission
    private void installFile(String fileName, String destinationDir) {
        String src = app.getFilesDir().getAbsolutePath() + App.SEPARATOR + fileName;
        String dest = destinationDir + App.SEPARATOR + fileName;
        RootTools.copyFile(src, dest, false, false);
        App.getShell().run(App.SHELL_SET_PERMISSION + dest);
    }

    //Make dir via shell
    private void makeDirShell(String dirPath) {
        if (!App.checkFileExists(dirPath))
            App.getShell().run(App.SHELL_MAKE_DIR + dirPath);
    }

    //Extract file and copy to app home dir
    private void extractFile(String fileName) {
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

    private void exit(String reason) {
        closeProgressDialog();
        showAlert(reason);
        main.finish();
    }

    private void closeProgressDialog() {
        if (dialog.isShowing()) {
            try {
                dialog.dismiss();
                dialog = null;
            } catch (Exception e) {
                Log.e(App.TAG, "Error in dialog.dismiss()");
            }
        }
    }

    private void showAlert(String reason) {
        Toast.makeText(main, reason, 15).show();
        Log.e(App.TAG, reason);
    }
}
