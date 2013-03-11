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

import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

public class Settings extends SherlockPreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setting);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setOnPreferenceChange();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setMountState(boolean mounts_mode) {
        if (App.checkConfigDir()) {
            if (mounts_mode) App.createMountFile();
            else App.deleteMountFile();
        }
    }

    private void setOnPreferenceChange() {

        Preference mounts = findPreference("mounts_ext4");
        mounts.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object object) {
                boolean value = object.equals(true);
                setMountState(value);
                return true;
            }
        });

        Preference ReadAhead = findPreference("set_read_ahead");
        ReadAhead.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object object) {
                boolean value = object.equals(true);
                setReadAhead(value);
                return true;
            }
        });

        Preference ReadAheadValue = findPreference("read_ahead_values");
        ReadAheadValue.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object object) {
                String value = object.toString();
                setReadAheadValue(value);
                return true;
            }
        });

        Preference settingsTheme = findPreference("setting_themes");
        settingsTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object object) {
                Toast.makeText(getApplicationContext(), App.getRes().getString(R.string.setting_themes_summary_toast), 10).show();
                return true;
            }
        });
    }

    private void setReadAhead(boolean ReadAhead) {
        if (App.checkConfigDir()) {
            if (ReadAhead) {
                App.createReadAheadFile();
                String value = App.getPrefs().getString("read_ahead_values", null);
                if (value != null) App.writeReadAheadValue(value);
            } else App.deleteReadAheadFile();
        }
    }

    private void setReadAheadValue(String read_ahead_value) {
        if (App.checkConfigDir()) App.writeReadAheadValue(read_ahead_value);
    }
}
