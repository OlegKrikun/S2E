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
import android.util.Log;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.RootToolsException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class App extends Application {

    static final String PREFERENCE_NAME_EXTENDED_INFORMATION = "show_extended_information";
    static final String PREFERENCE_NAME_BUSYBOX = "busybox";
    static final String PREFERENCE_NAME_THEME = "setting_themes";

    static final String THEME_DARK = "dark";
    static final String THEME_LIGHT = "light";

    private static App instance;
    private static SharedPreferences prefs;
    private static Resources res;

    //Directory-file separator
    public static final char SEPARATOR = '/';
    //Tag for logs
    public static final String TAG = "S2E";
    //Timeout for shell request
    private static final int SHELL_TIMEOUT = 60000;

    private PartitionsSet partitions;
    private TargetSet targets;

    private boolean ice;

    static App getInstance() {
        return instance;
    }

    static SharedPreferences getPrefs() {
        return prefs;
    }

    static Resources getRes() {
        return res;
    }

    //Send command to shell
    //if return code equals 1, return null
    public static List<String> sendShell(String str) {
        try {
            List<String> output = RootTools.sendShell(str, SHELL_TIMEOUT);
            if (!output.get(output.size() - 1).equals("1")) return output;
            else Log.e(TAG, "Error in shell: " + str + "; return code '1'");
        } catch (IOException e) {
            Log.e(TAG, "Error in shell: " + str + "; IOException");
        } catch (RootToolsException e) {
            Log.e(TAG, "Error in shell: " + str + "; RootToolsException");
        } catch (TimeoutException e) {
            Log.e(TAG, "Error in shell: " + str + "; TimeoutException");
        }
        return null;
    }

    //Dynamical convert size to MB or KB
    public static String convertSize(long size, String kb, String mb) {
        if (size == 0) return "--";
        else if (size >= 1024) return Long.toString(size / 1024L) + mb;
        else return size + kb;
    }

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
            activity.setTheme(R.style.Theme_Sherlock_Light_DarkActionBar_ForceOverflow);
        }
    }
}
