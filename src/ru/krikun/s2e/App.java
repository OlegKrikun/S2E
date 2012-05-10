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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import com.stericson.RootTools.RootTools;

import java.util.concurrent.TimeoutException;

public class App extends Application {

    public static final String PREFERENCE_NAME_EXTENDED_INFORMATION = "show_extended_information";
    private static final String PREFERENCE_NAME_BUSYBOX = "busybox";

    private static App instance;
    private TargetSet targets;
    private PartitionsSet partitions;

    private static SharedPreferences prefs;
    private static Resources res;

    private boolean root;
    private boolean ICS;
    private boolean supportedOS;
    private boolean scriptInstalled;

    static App getInstance() {
        return instance;
    }

    static SharedPreferences getPrefs() {
        return prefs;
    }

    static Resources getRes() {
        return res;
    }

    TargetSet getTargets() {
        return targets;
    }

    PartitionsSet getPartitions() {
        return partitions;
    }

    boolean isRoot() {
        return root;
    }

    void setRoot(boolean root) {
        this.root = root;
    }

    boolean isScriptInstalled() {
        return scriptInstalled;
    }

    void setScriptInstalled(boolean scriptInstalled) {
        this.scriptInstalled = scriptInstalled;
    }

    boolean isSupportedOS() {
        return supportedOS;
    }

    void setSupportedOS(boolean supportedOS) {
        this.supportedOS = supportedOS;
    }

    void setICS(boolean ICS) {
        this.ICS = ICS;
    }

    boolean isICS() {
        return ICS;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        res = getResources();
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    }

    public void loadAPI() {
        if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD
                || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD_MR1) {
            setSupportedOS(true);
        } else if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH
                || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            setICS(true);
            setSupportedOS(true);
        }
    }

    //Check access to root and busybox
    public void loadShell() {
        setRoot(isAccessGiven() && isBusyboxAvailable());
    }

    public void loadTargets() {
        targets = new TargetSet();
        targets.loadTargets(res.getStringArray(R.array.targets));
    }

    public void loadPartitions() {
        partitions = new PartitionsSet();
        partitions.loadPartitions(res.getStringArray(R.array.partitions), isICS());
    }

    //Save Boolean SharedPreference
    public void saveBooleanPreference(String namePreference, boolean valuePreference) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(namePreference, valuePreference);
        editor.commit();
    }

    //Check access
    private boolean isAccessGiven() {
        try {
            return RootTools.isAccessGiven();
        } catch (TimeoutException e) {
            return false;
        }
    }

    //Get busybox available
    private boolean isBusyboxAvailable() {
        if(prefs.contains(App.PREFERENCE_NAME_BUSYBOX)) {
            return prefs.getBoolean(App.PREFERENCE_NAME_BUSYBOX,false);
        } else {
            boolean isBusyboxAvailable = RootTools.isBusyboxAvailable();
            if(isBusyboxAvailable) saveBooleanPreference(App.PREFERENCE_NAME_BUSYBOX, isBusyboxAvailable);
            return isBusyboxAvailable;
        }
    }
}
