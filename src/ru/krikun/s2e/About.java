package ru.krikun.s2e;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class About extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.about);
    }

}
