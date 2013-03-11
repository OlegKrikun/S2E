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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import java.io.File;

public class App extends Application {

    //Path to userinit dir
    static final String PATH_USERINIT = "/data/local/userinit.d";
    //Path to bin dir
    static final String PATH_BIN = "/data/local/bin";
    //Files names of script, tune2fs and e2fsck
    static final String SCRIPT = "simple2ext";
    static final String TUNE2FS = "tune2fs";
    static final String E2FSCK = "e2fsck";
    static final String LOGGER = "logger";
    //Shell command templates for make dir
    static final String SHELL_MAKE_DIR = "busybox install -m 777 -o 1000 -g 1000 -d ";
    //Shell command templates for set permission to script
    static final String SHELL_SET_PERMISSION = "busybox chmod 0777 ";
    //Shell command for get md5 sum
    static final String SHELL_GET_MD5 = "busybox md5sum " + PATH_USERINIT + App.SEPARATOR + SCRIPT;
    //App home dir
    private static final String S2E_DIR = "/data/data/ru.krikun.s2e";
    //Path to S2E config dir (for ReadAhead and Mount feature)
    private static final String S2E_CONFIG_DIR = "/data/local/s2e_config";
    //Path to dir with status files
    private static final String SCRIPT_STATUS_DIR = S2E_DIR + "/status";

    static final String PREFERENCE_NAME_EXTENDED_INFORMATION = "show_extended_information";
    private static final String PREFERENCE_NAME_THEME = "setting_themes";

    private static final String THEME_DARK = "dark";
    private static final String THEME_LIGHT = "light";

    private static App instance;
    private static SharedPreferences prefs;
    private static Resources res;

    private static Shell shell = null;

    //Directory-file separator
    public static final char SEPARATOR = '/';
    //Tag for logs
    public static final String TAG = "S2E";

    private PartitionsSet partitions;
    private TargetSet targets;

    private boolean ice = false;

    static App getInstance() {
        return instance;
    }

    static Shell getShell() {
        return shell;
    }

    public static void setShell(Shell shell) {
        App.shell = shell;
    }

    static SharedPreferences getPrefs() {
        return prefs;
    }

    static Resources getRes() {
        return res;
    }

//    }

    //Check file exists
    public static boolean checkFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    //Compare sizes of partitions and dir
    public static boolean compareSizes(long size, long free) {
        return size != 0 && free != 0 && size < free;
    }

    TargetSet getTargets() {
        return targets;
    }

    PartitionsSet getPartitions() {
        return partitions;
    }

    void setIce(boolean ice) {
        this.ice = ice;
    }

    boolean isIce() {
        return ice;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        res = getResources();
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    }

    void loadTargets() {
        targets = new TargetSet();
        targets.loadTargets(res.getStringArray(R.array.targets));
    }

    void loadPartitions() {
        partitions = new PartitionsSet();
        partitions.loadPartitions(res.getStringArray(R.array.partitions), isIce());
    }

    //Save Boolean SharedPreference
    void saveBooleanPreference(String namePreference, boolean valuePreference) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(namePreference, valuePreference);
        editor.commit();
    }

    //Set theme from settings
    static void setActivityTheme(Context activity) {
        if (prefs.getString(PREFERENCE_NAME_THEME, THEME_DARK).equals(THEME_LIGHT)) {
            activity.setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
        }
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
        delFile(S2E_CONFIG_DIR + "/.mounts_ext4");
    }

    //Create ReadAhead file
    public static void createReadAheadFile() {
        createFile(S2E_CONFIG_DIR + "/.read_ahead");
    }

    //Delete ReadAhead file
    public static void deleteReadAheadFile() {
        delFile(S2E_CONFIG_DIR + "/.read_ahead");
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
        shell.run("busybox touch " + filePath);
    }

    //Delete file if file exists
    private static void delFile(String filePath) {
        if (checkFileExists(filePath)) shell.run("busybox rm " + filePath);
    }

    //Write to file if file exists
    private static void writeToFile(String filePath, String string) {
        if (checkFileExists(filePath)) shell.run("busybox echo " + string + " > " + filePath);
    }

    //Create dir
    private static void createDir(String path) {
        shell.run("busybox mkdir " + path);
    }
}
