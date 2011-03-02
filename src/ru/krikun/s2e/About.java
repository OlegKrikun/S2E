package ru.krikun.s2e;

import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;


public class About extends PreferenceActivity {

    private Resources res;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        res = getResources();

        addPreferencesFromResource(R.xml.about);

        Preference script_status = findPreference("script_status");
        script_status.setTitle(
                res.getString(R.string.script_versions) + " " + res.getString(R.string.script_version)
        );
	}
}
