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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class Tasks {
    //Path to userinit dir
    private static final String PATH_USERINIT = "/data/local/userinit.d";
    //Path to bin dir
    private static final String PATH_BIN = "/data/local/bin";
    //Files names of script, tune2fs and e2fsck
    private static final String SCRIPT = "simple2ext";
    private static final String TUNE2FS = "tune2fs";
    private static final String E2FSCK = "e2fsck";
    private static final String LOGGER = "logger";
    //Shell command templates for make dir
    private static final String SHELL_MAKE_DIR = "busybox install -m 777 -o 1000 -g 1000 -d ";
    //Shell command templates for set permission to script
    private static final String SHELL_SET_PERMISSION = "busybox chmod 0777 ";
    //Shell command for get md5 sum
    private static final String SHELL_GET_MD5 = "busybox md5sum " + PATH_USERINIT + App.SEPARATOR + SCRIPT;
    //App home dir
    public static final String S2E_DIR = "/data/data/ru.krikun.s2e";
    //Path to S2E config dir (for ReadAhead and Mount feature)
    private static final String S2E_CONFIG_DIR = "/data/local/s2e_config";
    //Path to dir with status files
    private static final String SCRIPT_STATUS_DIR = S2E_DIR + "/status";

    private final App app;
    private ProgressDialog dialog;
    private final Main main;
    private String md5;

    private boolean root;
    private boolean supportedOS;
    private boolean scriptInstalled;
    private boolean toolsInstalled;

    public Tasks(Main main) {
        this.main = main;
        app = App.getInstance();

        //Show progress dialog
        dialog = new ProgressDialog(main);
        dialog.show();
    }

    void doInitialization() {
        new Checker().execute();
    }

    void doRefresh() {
        new Refresher().execute();
    }

    private class Checker extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            //Check API
//            if (loadAPI()) {
                supportedOS = true;
                //Check shell
                if (loadShell()) {
                    root = true;
                    //Check script
                    if (checkScript()) scriptInstalled = true;
                    //Check tools
                    if (toolsExists()) toolsInstalled = true;
                }
//            }
            return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(App.getRes().getString(R.string.checking));
            //Getting md5 from resources
            md5 = App.getRes().getString(R.string.script_md5);

            root = false;
            supportedOS = false;
            scriptInstalled = false;
            toolsInstalled = false;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //Exit if OS not supported
            if (!supportedOS) exit(App.getRes().getString(R.string.alert_version));
            //Exit if access to root not given or busybox not found
            else if (!root) exit(App.getRes().getString(R.string.alert_root));
            //If script not installed run Installer
            else if (!scriptInstalled || !toolsInstalled) new Installer().execute();
            //Run Loader
            else new Loader().execute();
        }
    }
    private class Installer extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (!scriptInstalled) {
                //Installing script to home dir
                extractFile(SCRIPT);
                //Check /data/local/userinit.d exists and create this if needed
                makeDirShell(PATH_USERINIT);
                //Copy script and set permission
                installFile(SCRIPT, PATH_USERINIT);
                //Check script again and set "Script Installed" if script exists
                if (checkScript())
                    scriptInstalled = true;
            }
            if (!toolsInstalled) {
                //Check /data/local/bin exists and create this if needed
                makeDirShell(PATH_BIN);
                //Install tune2fs to home, copy to bin and set permission
                installTool(TUNE2FS);
                //Install e2fsck to home, copy to bin and set permission
                installTool(E2FSCK);
                //Install logger to home, copy to bin and set permission
                installTool(LOGGER);
            }
            return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(App.getRes().getString(R.string.installing));
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

    // Check Api
    private boolean loadAPI() {
        if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD
                || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD_MR1) {
            return true;
        } else if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH
                || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            app.setIce(true);
            return true;
        }
        return false;
    }

    //Check access to root and busybox
    private boolean loadShell() {
        return RootTools.isAccessGiven() && isBusyboxAvailable();
    }

    //Get busybox available
    private boolean isBusyboxAvailable() {
        if(App.getPrefs().contains(App.PREFERENCE_NAME_BUSYBOX)) {
            return App.getPrefs().getBoolean(App.PREFERENCE_NAME_BUSYBOX,false);
        } else {
            boolean isBusyboxAvailable = RootTools.isBusyboxAvailable();
            if(isBusyboxAvailable) app.saveBooleanPreference(App.PREFERENCE_NAME_BUSYBOX, isBusyboxAvailable);
            return isBusyboxAvailable;
        }
    }
    //Check script exists and control md5
    private boolean checkScript() {
        return App.checkFileExists(PATH_USERINIT + App.SEPARATOR + SCRIPT) && checkScriptSum();
    }

    //Return md5sum of path
    private String getSum() {
        List<String> output = App.sendShell(SHELL_GET_MD5);
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
    //Check tools exists
    private boolean toolsExists(){
        return  App.checkFileExists(PATH_BIN + App.SEPARATOR + TUNE2FS) &&
                App.checkFileExists(PATH_BIN + App.SEPARATOR + E2FSCK) &&
                App.checkFileExists(PATH_BIN + App.SEPARATOR + LOGGER);
    }

    //Install tool to bin dir
    private void installTool(String toolName) {
        //Install tool to home
        extractFile(toolName);
        //Copy tool to bin and set permission
        installFile(toolName, PATH_BIN);
    }

    //Copy and set permission
    private void installFile(String fileName, String destinationDir) {
        String src = app.getFilesDir().getAbsolutePath() + App.SEPARATOR + fileName;
        String dest = destinationDir + App.SEPARATOR + fileName;
        RootTools.copyFile(src, dest, false, false);
        App.sendShell(SHELL_SET_PERMISSION + dest);
    }
    //Make dir via shell
    private void makeDirShell(String dirPath) {
        if(!App.checkFileExists(dirPath))
            App.sendShell(SHELL_MAKE_DIR + dirPath);
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
    //Create status file
    //if status file dir not exists, then create this dir
    public static void createStatusFile(String target) {
        if (!App.checkFileExists(SCRIPT_STATUS_DIR)) createDir(SCRIPT_STATUS_DIR);
        createFile(SCRIPT_STATUS_DIR + App.SEPARATOR + target);
    }

    //Check config dir exists and create this if needed
    public static boolean checkConfigDir() {
        if (!App.checkFileExists(S2E_CONFIG_DIR)) {
            createDir(S2E_CONFIG_DIR);
            return App.checkFileExists(S2E_CONFIG_DIR);
        } else return true;
    }

    //Create mount file
    public static void createMountFile() {
        createFile(S2E_CONFIG_DIR + "/.mounts_ext4");
    }

    //Delete mount file
    public static void deleteMountFile() {
        deleteFile(S2E_CONFIG_DIR + "/.mounts_ext4");
    }

    //Create ReadAhead file
    public static void createReadAheadFile() {
        createFile(S2E_CONFIG_DIR + "/.read_ahead");
    }

    //Delete ReadAhead file
    public static void deleteReadAheadFile() {
        deleteFile(S2E_CONFIG_DIR + "/.read_ahead");
    }

    //Write value to ReadAhead file
    public static void writeReadAheadValue(String value) {
        writeToFile(S2E_CONFIG_DIR + "/.read_ahead", value);
    }

    //Check status file
    public static boolean checkStatusFileExists(String target) {
        File status = new File(SCRIPT_STATUS_DIR, target);
        return status.exists();
    }

    //Create file
    private static void createFile(String filePath) {
        App.sendShell("busybox touch " + filePath);
    }

    //Delete file if file exists
    private static void deleteFile(String filePath) {
        if (App.checkFileExists(filePath)) App.sendShell("busybox rm " + filePath);
    }

    //Write to file if file exists
    private static void writeToFile(String filePath, String string) {
        if (App.checkFileExists(filePath)) App.sendShell("busybox echo " + string + " > " + filePath);
    }

    //Create dir
    private static void createDir(String path) {
        App.sendShell("busybox mkdir " + path);
    }
}
