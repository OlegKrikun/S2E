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
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.util.Log;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.List;

public class Main extends SherlockPreferenceActivity {

    private App app;
    private int sizeToExt;
    private int sizeToData;
    private boolean showExtendedInformation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        App.setActivityTheme(this);

        new Tasks(this).doInitialization();

        super.onCreate(savedInstanceState);

        app = App.getInstance();

        setContentView(R.layout.main_screen);
        getSupportActionBar().setTitle(R.string.app_label);
        getSupportActionBar().setSubtitle("DATA: --  EXT: --");
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExtendedInformationPref();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.info:
                showInformation();
                break;
            case R.id.viewlog:
                Intent logViewer = new Intent(getBaseContext(), LogViewer.class);
                startActivity(logViewer);
                break;
            case R.id.refresh:
                new Tasks(this).doRefresh();
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

    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(getApplication())
                .inflate(R.menu.menu, menu);

        return (super.onPrepareOptionsMenu(menu));
    }

    void addView() {
        addPreferencesFromResource(R.xml.main);
        setOnPreferenceClick();
    }

    void updateView() {
        setTargetsState();
        setTitle();
    }

    //Dynamical convert size to MB or KB
    private StringBuilder convertSize(long size) {

        StringBuilder stringBuilder = new StringBuilder();

        if (size == 0) {

            stringBuilder.append("--");

        } else if (size >= 1024) {

            stringBuilder.append(size / 1024L);
            stringBuilder.append(App.getRes().getString(R.string.mb));

        } else {

            stringBuilder.append(size);
            stringBuilder.append(App.getRes().getString(R.string.kb));
        }

        return stringBuilder;
    }

    private StringBuilder formatSummaryMoving(String targetPartition, String sourcesPartition) {
        return new StringBuilder()
                .append(String.format(App.getRes().getString(R.string.move), sourcesPartition, targetPartition))
                .append("\n")
                .append(App.getRes().getString(R.string.reboot_required));
    }

    private StringBuilder formatSummaryStatic(String target, String partition) {
        return new StringBuilder()
                .append(App.getRes().getString(R.string.location))
                .append(": ")
                .append(partition)
                .append(App.SEPARATOR)
                .append(target)
                .append("\n")
                .append(App.getRes().getString(R.string.size))
                .append(": ")
                .append(convertSize(app.getTargets().get(target).getSize()));
    }

    private StringBuilder getPartitionString(Partition partition) {
        return new StringBuilder()
                .append("\n\t")
                .append(App.getRes().getString(R.string.size))
                .append(": ")
                .append(convertSize(partition.getSize()))
                .append("\n\t")
                .append(App.getRes().getString(R.string.used))
                .append(": ")
                .append(convertSize(partition.getUsed()))
                .append("\n\t")
                .append(App.getRes().getString(R.string.free))
                .append(": ")
                .append(convertSize(partition.getFree()));
    }

    private boolean checkFreeSpace(long targetSize, long partitionFree, long sizeToPartition) {
        long freeSpace = (partitionFree - sizeToPartition - 1024L);
        return App.compareSizes(targetSize, freeSpace);
    }

    private boolean checkInformationProvider(Intent intent) {
        PackageManager packageManager = getPackageManager();
        List list = packageManager.queryIntentActivities(intent, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        return !list.isEmpty();
    }

    private void calcMovedSizes() {
        sizeToExt = 0;
        sizeToData = 0;

        for (Target target : app.getTargets().values()) {

            target.updateStatus();

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Found target: ");
            stringBuilder.append(target.getTargetName());

            switch (target.getStatus()) {

                case Target.TARGET_MOVE_TO_EXT:

                    sizeToExt += target.getSize();

                    stringBuilder.append(" for move to EXT; Target size: ");
                    stringBuilder.append(target.getSize());
                    stringBuilder.append("; All size to EXT: ");
                    stringBuilder.append(sizeToExt);

                    break;

                case Target.TARGET_MOVE_TO_DATA:

                    sizeToData += target.getSize();

                    stringBuilder.append(" for move to DATA; Target size: ");
                    stringBuilder.append(String.valueOf(target.getSize()));
                    stringBuilder.append("; All size to EXT: ");
                    stringBuilder.append(String.valueOf(sizeToData));

                    break;
            }

            Log.d(App.TAG, stringBuilder.toString());
        }
    }

    private void loadExtendedInformationPref() {
        showExtendedInformation = App.getPrefs().getBoolean("show_extended_information", true);
    }

    private void setOnPreferenceClick() {
        for (String target : App.getRes().getStringArray(R.array.targets)) {
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
        for (Target target : app.getTargets().values()) {
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
                    pref.setEnabled(checkFreeSpace(target.getSize(), app.getPartitions().get("data").getFree(), sizeToData));
                    break;
                case Target.TARGET_ON_DATA:
                    pref.setSummary(formatSummaryStatic(target.getTargetName(), target.getInternal()));
                    pref.setEnabled(checkFreeSpace(target.getSize(), app.getPartitions().get("sd-ext").getFree(), sizeToExt));
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

    private void setTitle() {

        StringBuilder label = new StringBuilder()
                .append("DATA: ")
                .append(convertSize(app.getPartitions().get("data").getFree()))
                .append("  ")
                .append("EXT: ")
                .append(convertSize(app.getPartitions().get("sd-ext").getFree()));
        getSupportActionBar().setSubtitle(label);
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
        builder.setTitle(App.getRes().getString(R.string.fs_title))
                .setMessage(App.getRes().getString(R.string.fs_msg))
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(App.getRes().getString(R.string.fs_install), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent fs = new Intent(Intent.ACTION_VIEW);
                        fs.setData(Uri.parse("https://play.google.com/store/apps/details?id=ru.krikun.freespace"));
                        startActivity(fs);
                    }
                })
                .setNegativeButton(App.getRes().getString(R.string.fs_dont), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        showExtendedInformation = false;
                        app.saveBooleanPreference(
                                App.PREFERENCE_NAME_EXTENDED_INFORMATION, showExtendedInformation);
                        showPartitionsSpaces();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showPartitionsSpaces() {
        StringBuilder message = new StringBuilder()
                .append("DATA:")
                .append(getPartitionString(app.getPartitions().get("data")))
                .append("\n\nSD-EXT:")
                .append(getPartitionString(app.getPartitions().get("sd-ext")))
                .append("\n\nCACHE:")
                .append(getPartitionString(app.getPartitions().get("cache")));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setCancelable(true)
                .setNeutralButton(App.getRes().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
