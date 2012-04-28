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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

public class Main extends PreferenceActivity {

    private App app;

    private HashMap<String, Target> targets;

    private Partition partitionData;
    private Partition partitionCache;
    private Partition partitionExt;

    private int sizeToExt;
    private int sizeToData;

    private Resources res;

    private boolean showExtendedInformation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        res = getResources();
        app = App.getInstance();

        setTheme();
        super.onCreate(savedInstanceState);

        //Exit if OS not supported
        if (!app.isSupportedOS()) showAlert(res.getString(R.string.alert_version));
        //Exit if access to root not given or busybox not found
        else if (!app.isRoot()) showAlert(res.getString(R.string.alert_root));
        //Exit if script not installed
        else if (!app.isScriptInstalled()) showAlert(res.getString(R.string.alert_script_title));


        setContentView(R.layout.main_screen);
        addPreferencesFromResource(R.xml.main);
        setOnPreferenceClick();

        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExtendedInformationPref();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(getApplication())
                .inflate(R.menu.menu, menu);
        return (super.onPrepareOptionsMenu(menu));
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                Toast.makeText(Main.this, res.getString(R.string.refreshing), 5).show();
                refresh();
                break;
            case R.id.info:
                showInformation();
                break;
            case R.id.settings:
                Intent prefSettings = new Intent(getBaseContext(), Settings.class);
                startActivity(prefSettings);
                break;
            case R.id.myapps:
                Intent buy = new Intent(Intent.ACTION_VIEW);
                buy.setData(Uri.parse("market://search?q=pub:OlegKrikun"));
                startActivity(buy);
                break;
            case R.id.about:
                Intent prefAbout = new Intent(getBaseContext(), About.class);
                startActivity(prefAbout);
                break;
        }
        return (super.onOptionsItemSelected(item));
    }

    private String formatSummaryMoving(String targetPartition, String sourcesPartition) {
        return String.format(res.getString(R.string.move), sourcesPartition, targetPartition) + "\n" +
                res.getString(R.string.reboot_required);
    }

    private String formatSummaryStatic(String target, String partition) {
        return res.getString(R.string.location) + ": " + partition + "/" + target + "\n" +
                res.getString(R.string.size) + ": " +
                Helper.convertSize(targets.get(target).getSize(), res.getString(R.string.kb), res.getString(R.string.mb));
    }

    private String getPartitionString(Partition partition) {
        return "\n\t" + res.getString(R.string.size) + ": " +
                Helper.convertSize((int) partition.getSize(), res.getString(R.string.kb), res.getString(R.string.mb)) +
                "\n\t" + res.getString(R.string.used) + ": " +
                Helper.convertSize((int) partition.getUsed(), res.getString(R.string.kb), res.getString(R.string.mb)) +
                "\n\t" + res.getString(R.string.free) + ": " +
                Helper.convertSize((int) partition.getFree(), res.getString(R.string.kb), res.getString(R.string.mb));
    }

    private boolean checkFreeSpace(Target target, Partition partition, int sizeToPartition) {
        int freeSpace = (int) (partition.getFree() - sizeToPartition - 1024);
        return Helper.compareSizes(target.getSize(), freeSpace);
    }

    private boolean checkInformationProvider(Intent intent) {
        PackageManager packageManager = getPackageManager();
        List list = packageManager.queryIntentActivities(intent, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        return !list.isEmpty();
    }

    private void calcMovedSizes() {
        sizeToExt = 0;
        sizeToData = 0;

        for (Target target : targets.values()) {
            target.updateStatus();

            if (target.getStatus() == Target.TARGET_MOVE_TO_EXT) {
                sizeToExt += target.getSize();
                Log.d(Helper.TAG, "Found target: " + target.getTargetName() + " for move to EXT; Target size: "
                        + String.valueOf(target.getSize()) + "; All size to EXT: " + String.valueOf(sizeToExt));
            }
            if (target.getStatus() == Target.TARGET_MOVE_TO_DATA)
                sizeToData += target.getSize();
            Log.d(Helper.TAG, "Found target: " + target.getTargetName() + " for move to DATA; Target size: "
                    + String.valueOf(target.getSize()) + "; All size to EXT: " + String.valueOf(sizeToData));
        }
    }

    private void loadExtendedInformationPref() {
        showExtendedInformation = app.getPrefs().getBoolean("show_extended_information", true);
    }

    private void loadPartitionsSizes() {
        partitionData = new Partition("/data");
        partitionCache = new Partition("/cache");
        if (app.isICS()) partitionExt = new Partition("/sd-ext");
        else partitionExt = new Partition("/sd-ext", true);
    }

    private void loadTargets() {
        targets = new HashMap<String, Target>();

        for (String target : res.getStringArray(R.array.targets)) {
            targets.put(target, new Target(getBaseContext(), target));
        }
    }

    private void refresh() {
        loadTargets();
        loadPartitionsSizes();
        setTargetsState();
        setTitle();
    }

    private void setOnPreferenceClick() {
        for (String target : res.getStringArray(R.array.targets)) {
            Preference pref = findPreference(target);
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                public boolean onPreferenceClick(Preference preference) {
                    setTargetsState();
                    return true;
                }
            });
        }
    }

    private void setPreferenceOptions() {
        for (Target target : targets.values()) {
            Preference pref = findPreference(target.getTargetName());

            switch (target.getStatus()) {
                case Target.TARGET_MOVE_TO_EXT:
                    pref.setSummary(formatSummaryMoving(target.getExternal(), target.getInternal()));
                    break;
                case Target.TARGET_MOVE_TO_DATA:
                    pref.setSummary(formatSummaryMoving(target.getInternal(), target.getExternal()));
                    break;
                case Target.TARGET_MOVE_TO_CACHE:
                    pref.setSummary(formatSummaryMoving(target.getInternal(), target.getExternal()));
                    break;
                case Target.TARGET_ON_EXT:
                    pref.setSummary(formatSummaryStatic(target.getTargetName(), target.getExternal()));
                    pref.setEnabled(checkFreeSpace(target, partitionData, sizeToData));
                    break;
                case Target.TARGET_ON_DATA:
                    pref.setSummary(formatSummaryStatic(target.getTargetName(), target.getInternal()));
                    pref.setEnabled(checkFreeSpace(target, partitionExt, sizeToExt));
                    break;
                case Target.TARGET_ON_CACHE:
                    pref.setSummary(formatSummaryStatic(target.getTargetName(), target.getInternal()));
                    break;
            }
        }
    }

    private void setTargetsState() {
        calcMovedSizes();
        setPreferenceOptions();
    }

    private void setTheme() {
        if (!app.isICS()) {
            setTheme(android.R.style.Theme_NoTitleBar);
        } else {
            setTheme(android.R.style.Theme_Holo);
            getActionBar().setDisplayShowHomeEnabled(false);
        }
    }

    private void setTitle() {
        String label =
                "DATA: " + Helper.convertSize((int) partitionData.getFree(), res.getString(R.string.kb), res.getString(R.string.mb)) + "  " +
                        "EXT: " + Helper.convertSize((int) partitionExt.getFree(), res.getString(R.string.kb), res.getString(R.string.mb));
        if (app.isICS()) {
            getActionBar().setTitle(label);
        } else {
            TextView title_line = (TextView) findViewById(R.id.title_line);
            title_line.setText(label);
        }
    }

    private void showAlert(String text) {
        Toast.makeText(this, text, 10).show();
        Log.e(Helper.TAG, text);
        Main.this.finish();
    }

    private void showInformation() {
        if (showExtendedInformation) {
            Intent intentFreeSpace = new Intent(Intent.ACTION_MAIN);
            intentFreeSpace.setClassName("ru.krikun.freespace", "ru.krikun.freespace.activity.List");
            Intent intentFreeSpacePlus = new Intent(Intent.ACTION_MAIN);
            intentFreeSpacePlus.setClassName("ru.krikun.freespace.plus", "ru.krikun.freespace.plus.activity.List");

            if (checkInformationProvider(intentFreeSpacePlus)) startActivity(intentFreeSpacePlus);
            else if (checkInformationProvider(intentFreeSpace)) startActivity(intentFreeSpace);
            else showInformationProviderQuestion();
        } else showPartitionsSpaces();
    }

    private void showInformationProviderQuestion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(res.getString(R.string.fs_title))
                .setMessage(res.getString(R.string.fs_msg))
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(res.getString(R.string.fs_install), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent fs = new Intent(Intent.ACTION_VIEW);
                        fs.setData(Uri.parse("https://play.google.com/store/apps/details?id=ru.krikun.freespace"));
                        startActivity(fs);
                    }
                })
                .setNegativeButton(res.getString(R.string.fs_dont), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        showExtendedInformation = false;
                        app.saveBooleanPreference(
                                App.PREFERENCE_NAME_EXTENDED_INFORMATION,showExtendedInformation);
                        showPartitionsSpaces();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showPartitionsSpaces() {
        String message =
                "DATA:" + getPartitionString(partitionData) +
                        "\n\nSD-EXT:" + getPartitionString(partitionExt) +
                        "\n\nCACHE:" + getPartitionString(partitionCache);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
