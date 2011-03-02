package ru.krikun.s2e;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import ru.krikun.s2e.utils.ShellInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class Main extends PreferenceActivity {

    private Resources res;

    private static final String SCRIPT_DIR = "/data/local/userinit.d/simple2ext";
    private static final String S2E_DIR = "/data/data/ru.krikun.s2e";
    private static final String SCRIPT_STATUS_DIR = S2E_DIR + "/status";

    private HashMap<String, Boolean> statuses;
    private HashMap<String, Integer> sizes;

    private int[] spacesData;
    private int[] spacesExt;
    private int[] spacesCache;

    private void copyScript() {
        try {
            InputStream in = getAssets().open("script01.sh");
            FileOutputStream out = openFileOutput("script01.sh", 0);
            if (in != null) {
                int size = in.available();
                byte[] buffer = new byte[size];
                in.read(buffer);
                out.write(buffer, 0, size);
                in.close();
                out.close();
                if (ShellInterface.isSuAvailable()) {
                    ShellInterface.runCommand("if [ ! -e /data/local/userinit.d ]; then install -m 777 -o 1000 -g 1000 -d /data/local/userinit.d; fi");
                    ShellInterface.runCommand("cp " + S2E_DIR + "/files/script01.sh " + SCRIPT_DIR);
                    ShellInterface.runCommand("chmod 0777 " + SCRIPT_DIR);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getSum(String target) {
        if (ShellInterface.isSuAvailable()) {
            String tmp_md5 = ShellInterface.getProcessOutput("md5sum " + target);
            if (tmp_md5.length() >= 32) {
                return tmp_md5.substring(0, 32);
            }
        }
        return null;
    }

    private boolean checkScriptSum() {
        String tmp_md5 = getSum(SCRIPT_DIR);
        return !tmp_md5.equals(null) && tmp_md5.equals(res.getString(R.string.script_version_md5));
    }

    private boolean checkScriptExists() {
        File target = new File(SCRIPT_DIR);
        return target.exists();
    }

    private boolean checkScript() {
        return checkScriptExists() && checkScriptSum();
    }

    private boolean installScript() {
        copyScript();
        return checkScript();
    }

    private void showAlert(String message, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(title)
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNeutralButton(res.getString(R.string.exit), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Main.this.finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showAlertScript() {
        showAlert(res.getString(R.string.alert_script_message), res.getString(R.string.alert_script_title));
    }

    private boolean checkStatus(String target) {
        File status = new File(SCRIPT_STATUS_DIR, target);
        return status.exists();
    }

    private static int getSize(String path) {
        if (ShellInterface.isSuAvailable()) {
            String output = ShellInterface.getProcessOutput("du -s " + path);
            String[] array = output.split("[\\s]");
            if (!array[0].equals("")) {
                return Integer.parseInt(array[0]);
            }
        }
        return 0;
    }

    public int[] getPartitionsInfo(String partition) {

        int[] intArray = new int[3];

        if (ShellInterface.isSuAvailable()) {

            String tmp = ShellInterface.getProcessOutput("df /" + partition + " | grep /" + partition);
            if (!tmp.equals(null)) {
                String[] array = tmp.split("\\s+");
                if (array.length == 6) {
                    intArray[0] = Integer.parseInt(array[1]);
                    intArray[1] = Integer.parseInt(array[2]);
                    intArray[2] = Integer.parseInt(array[3]);
                }
            }
        }
        return intArray;
    }

    private String convertSize(int size) {
        if (size == 0) return "--";
        else if (size >= 1024) return Integer.toString(size / 1024) + res.getString(R.string.mb);
        else return size + res.getString(R.string.kb);
    }

    private String getPath(String target) {
        if (target.equals("download")) return "/cache/" + target;
        else return "/data/" + target;
    }

    private void setupStatuses() {
        statuses = new HashMap<String, Boolean>();

        for (String target : res.getStringArray(R.array.targets)) {
            statuses.put(target, checkStatus(target));
        }
    }

    private void setupSizes() {
        sizes = new HashMap<String, Integer>();

        for (String target : res.getStringArray(R.array.targets)) {
            sizes.put(target, getSize(getPath(target)));
        }
    }

    private void setupPartitionsInfo() {
        // 0 - Size; 1 - Used; 2 - Free
        spacesData = getPartitionsInfo("data");
        spacesExt = getPartitionsInfo("sd-ext");
        spacesCache = getPartitionsInfo("cache");
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(getApplication())
                .inflate(R.menu.menu, menu);
        return (super.onPrepareOptionsMenu(menu));
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent prefSettings = new Intent(getBaseContext(), Settings.class);
                startActivity(prefSettings);
                break;
            case R.id.about:
                Intent prefAbout = new Intent(getBaseContext(), About.class);
                startActivity(prefAbout);
                break;
        }
        return (super.onOptionsItemSelected(item));
    }

    private void setupSpacePref() {

        for (String partition : res.getStringArray(R.array.partitions)) {
            int[] array = new int[3];
            String pref = null;
            if (partition.equals("data")) {
                array = spacesData;
                pref = "space_data";
            }
            else if (partition.equals("sd-ext")) {
                array = spacesExt;
                pref = "space_ext";
            }
            else if (partition.equals("cache")) {
                array = spacesCache;
                pref = "space_cache";
            }

            Preference preference = findPreference(pref);
            preference.setSummary(
                    res.getString(R.string.size) + ": " +  convertSize(array[0]) + "\n" +
                    res.getString(R.string.used) + ": " +  convertSize(array[1]) + "\n" +
                    res.getString(R.string.free) + ": " +  convertSize(array[2])
            );
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        res = getResources();
        addPreferencesFromResource(R.xml.main);

        if (!checkScript() && !installScript()) showAlertScript();

        setupStatuses();
    }

    @Override
    public void onResume() {
        super.onResume();

        setupSizes();
        setupPartitionsInfo();
        setupSpacePref();

        Preference partitions = findPreference("partitions");
        partitions.setSummary(
                "Data: " +  convertSize(spacesData[2]) + "  " +
                "Ext: " +  convertSize(spacesExt[2]) + "  " +
                "Cache: " +  convertSize(spacesCache[2])
        );

        for (String target : res.getStringArray(R.array.targets)) {
            Preference pref = findPreference(target);

            if (statuses.get(target)) {
                if (target.equals("app") || target.equals("app-private")) pref.setEnabled(false);
                pref.setSummary(
                        res.getString(R.string.location) + ": /sd-ext/" + target + "\n" +
                                res.getString(R.string.size) + ": " + convertSize(sizes.get(target)));
            } else {
                if (target.equals("download")) pref.setSummary(
                        res.getString(R.string.location) + ": /cache/" + target + "\n" +
                                res.getString(R.string.size) + ": " + convertSize(sizes.get(target)));
                else pref.setSummary(
                        res.getString(R.string.location) + ": /data/" + target + "\n" +
                                res.getString(R.string.size) + ": " + convertSize(sizes.get(target)));
            }
        }
    }
}
