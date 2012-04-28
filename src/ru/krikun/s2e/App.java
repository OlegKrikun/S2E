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
import android.os.Build;
import android.preference.PreferenceManager;
import com.stericson.RootTools.RootTools;

import java.util.concurrent.TimeoutException;

public class App extends Application {

    public static final String PREFERENCE_NAME_EXTENDED_INFORMATION = "show_extended_information";
    public static final String PREFERENCE_NAME_BUSYBOX = "busybox";

    private static App instance;
    

    private SharedPreferences prefs;

    private boolean root;
    private boolean ICS;
    private boolean supportedOS;
    private boolean scriptInstalled;

    public static App getInstance() {
        return instance;
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    public boolean isRoot() {
        return root;
    }

    public boolean isScriptInstalled() {
        return scriptInstalled;
    }

    public boolean isSupportedOS() {
        return supportedOS;
    }

    public boolean isICS() {
        return ICS;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        setRoot();
        setAPI();
        installScript();
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
        if(prefs.contains(PREFERENCE_NAME_BUSYBOX)) return prefs.getBoolean(PREFERENCE_NAME_BUSYBOX,false);
        else {
            boolean isBusyboxAvailable = RootTools.isBusyboxAvailable();
            if(isBusyboxAvailable) saveBooleanPreference(PREFERENCE_NAME_BUSYBOX, isBusyboxAvailable);
            return isBusyboxAvailable;
        }
    }

    private void setAPI() {
        if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD
                || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD_MR1) {
            ICS = false;
            supportedOS = true;
        } else if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH
                || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            ICS = true;
            supportedOS = true;
        }
    }

    private void setRoot() {
        root =  isAccessGiven() && isBusyboxAvailable();
    }

    private void installScript() {
        if(root) {
            Installer installer = new Installer();
            scriptInstalled = installer.isInstalled();
        }
    }
}
