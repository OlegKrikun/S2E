package ru.krikun.s2e;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import ru.krikun.s2e.utils.Helper;

public class Settings extends PreferenceActivity {

    private void setMountState(boolean mounts_mode) {
        if (Helper.checkConfigDir()) {
            if (mounts_mode) Helper.createMountFile("mounts_ext4");
            else Helper.deleteMountFile("mounts_ext4");
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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setting);

        setOnPreferenceChange();
    }

}
