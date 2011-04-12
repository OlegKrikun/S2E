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

    private HashMap<String, Integer> jobSizes;
	private HashMap<String, Boolean> jobStatuses;
	private HashMap<String, Boolean> jobValues;

    private int[] spacesData;
    private int[] spacesExt;
    private int[] spacesCache;

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
        if (tmp_md5 != null) {
           return tmp_md5.equals(res.getString(R.string.script_version_md5));
        }
        return false;
    }

    private boolean checkScriptExists() {
        File target = new File(SCRIPT_DIR);
        return target.exists();
    }

    private boolean checkScript() {
        return checkScriptExists() && checkScriptSum();
    }

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
        jobStatuses = new HashMap<String, Boolean>();

        for (String target : res.getStringArray(R.array.targets)) {
            jobStatuses.put(target, checkStatus(target));
        }
    }

	private void setupValues() {
        jobValues = new HashMap<String, Boolean>();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        for (String target : res.getStringArray(R.array.targets)) {
            jobValues.put(target, prefs.getBoolean(target, false));
        }
    }

    private void setupSizes() {
        jobSizes = new HashMap<String, Integer>();

        for (String target : res.getStringArray(R.array.targets)) {
            jobSizes.put(target, getSize(getPath(target)));
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

	private boolean compareSizes( int target, int partition) {
		return target != 0 && partition != 0 && target < partition;
	}

	private void setTargetSummary(Preference pref, String target, String partition) {
		pref.setSummary(
				res.getString(R.string.location) + ": /" + partition + "/" + target + "\n" +
						res.getString(R.string.size) + ": " + convertSize(jobSizes.get(target)));
	}

	private void setMovingSummary(Preference pref, String targetPartition, String sourcesPartition) {
		pref.setSummary(
                String.format(res.getString(R.string.move), sourcesPartition, targetPartition) + "\n" +
						res.getString(R.string.reboot_required));
	}

	private void setTargetState() {

        setupValues();

        int sumToExt = 0;
		int sumToData = 0;

		for (String target : res.getStringArray(R.array.targets)) {
			Preference pref = findPreference(target);
			if (!target.equals("download")) {
				if (jobValues.get(target) && !jobStatuses.get(target)) {
					setMovingSummary(pref, "/sd-ext", "/data");
					sumToExt += jobSizes.get(target);
				}
				else if (!jobValues.get(target) && jobStatuses.get(target)) {
					setMovingSummary(pref, "/data", "/sd-ext");
					sumToData += jobSizes.get(target);
				}
			} else {
				if (jobValues.get(target) && !jobStatuses.get(target)) setMovingSummary(pref, "/sd-ext", "/cache");
				else if (!jobValues.get(target) && jobStatuses.get(target)) setMovingSummary(pref, "/cache", "/sd-ext");
			}
		}

		int freeSpace;
		for (String target : res.getStringArray(R.array.targets)) {
			Preference pref = findPreference(target);
			if (!target.equals("download")) {
				if (!jobValues.get(target) && !jobStatuses.get(target)) {

					setTargetSummary(pref, target, "data");
					freeSpace = spacesExt[2] - sumToExt - 1024;
					if (!compareSizes(jobSizes.get(target), freeSpace)) pref.setEnabled(false);
                    else pref.setEnabled(true);

				} else if (jobValues.get(target) && jobStatuses.get(target)) {

					setTargetSummary(pref, target, "sd-ext");
					freeSpace = spacesData[2] - sumToData - 1024;
					if (!compareSizes(jobSizes.get(target), freeSpace)) pref.setEnabled(false);
                    else pref.setEnabled(true);

				}
			} else {
				if (!jobValues.get(target) && !jobStatuses.get(target)) setTargetSummary(pref, target, "cache");
				else if (jobValues.get(target) && jobStatuses.get(target)) setTargetSummary(pref, target, "sd-ext");
			}
		}
	}

	private void setOnPreferenceChange(){

		for (String target : res.getStringArray(R.array.targets)) {
			Preference pref = findPreference(target);
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                public boolean onPreferenceClick(Preference preference) {
                    setTargetState();
                    return true;
                }

            });
        }
	}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        res = getResources();
        setTitle(R.string.label);
        addPreferencesFromResource(R.xml.main);

        setupStatuses();

        setOnPreferenceChange();

        if (!checkScript() && !installScript()) showAlertScript();
    }

    @Override
    public void onResume() {
        super.onResume();


        setupPartitionsInfo();
        setupSpacePref();

		setupValues();
        setupSizes();

        Preference partitions = findPreference("partitions");
        partitions.setSummary(
                "Data: " +  convertSize(spacesData[2]) + "  " +
                "Ext: " +  convertSize(spacesExt[2]) + "  " +
                "Cache: " +  convertSize(spacesCache[2])
        );

		setTargetState();
    }
}
