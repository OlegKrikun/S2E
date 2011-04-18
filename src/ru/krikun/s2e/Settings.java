package ru.krikun.s2e;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import ru.krikun.s2e.utils.Helper;

public class Settings extends PreferenceActivity {

    SharedPreferences prefs;

    private void setMountState(boolean mounts_mode) {
        if (Helper.checkConfigDir()) {
            if (mounts_mode) Helper.createMountFile("mounts_ext4");
            else Helper.deleteMountFile("mounts_ext4");
        }
    }

    private void setReadAhead(boolean ReadAhead) {
        if (Helper.checkConfigDir()) {
            if (ReadAhead) {
                Helper.createReadAheadFile();
                String value = prefs.getString("read_ahead_values", null);
                if(value != null) Helper.writeReadAheadValue(value);
            }
            else Helper.deleteReadAheadFile();
        }
    }

    private void setReadAheadValue(String read_ahead_value) {
        if (Helper.checkConfigDir()) Helper.writeReadAheadValue(read_ahead_value);
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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setting);

        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        setOnPreferenceChange();
    }

}
