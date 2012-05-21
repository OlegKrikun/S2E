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
import android.preference.PreferenceManager;

public class App extends Application {

    static final String PREFERENCE_NAME_EXTENDED_INFORMATION = "show_extended_information";
    static final String PREFERENCE_NAME_BUSYBOX = "busybox";

    private static App instance;
    private static SharedPreferences prefs;
    private static Resources res;

    //Directory-file separator
    public static final char SEPARATOR = '/';

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
}
