package ru.krikun.s2e;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import ru.krikun.s2e.utils.ShellInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Main extends PreferenceActivity {

	private static final String SCRIPT_DIST = "/data/local/userinit.d/simple2ext";
	private static final String S2E_DIR = "/data/data/ru.krikun.s2e";
	private static final String SCRIPT_STATUS_DIR = S2E_DIR + "/status";
	private static final String BASH_MKDIR = "if [ ! -e /data/local/userinit.d ]; then install -m 777 -o 1000 -g 1000 -d /data/local/userinit.d; fi";
	private static final String BASH_CP_SCRIPT = "cp " + S2E_DIR + "/files/script01.sh " + SCRIPT_DIST;
	private static final String BASH_CHMOD_SCRIPT = "chmod 0777 " + SCRIPT_DIST;
	private static final String BASH_MD5SUM = "md5sum " + SCRIPT_DIST;

	private static boolean checkScript(String target_md5) {

		File target = new File(SCRIPT_DIST);
		if (target.exists()) {
			if (ShellInterface.isSuAvailable()) {
				String tmp_md5 = ShellInterface.getProcessOutput(BASH_MD5SUM);
                if (tmp_md5.length() >= 32) {
                    tmp_md5 = tmp_md5.substring(0, 32);
                    return tmp_md5.equals(target_md5);
				}
			}
		}
		return false;
	}

	private void runInstallScript() {

		final Resources res = getResources();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(res.getString(R.string.install_script_message))
			.setTitle(res.getString(R.string.install_script_title))
			.setCancelable(false)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setPositiveButton(res.getString(R.string.install_script_positive), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {

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
								ShellInterface.runCommand(BASH_MKDIR);
								ShellInterface.runCommand(BASH_CP_SCRIPT);
								ShellInterface.runCommand(BASH_CHMOD_SCRIPT);}
						}
					} catch (IOException e) { throw new RuntimeException(e);}

					if (!checkScript(res.getString(R.string.script_version_md5))) {
						showAlert(
                                res.getString(R.string.alert_script_message),
                                res.getString(R.string.alert_script_title),
                                res.getString(R.string.exit)
                        );
                    }
					else  showMain();
				}
			})
			.setNegativeButton(res.getString(R.string.exit), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Main.this.finish();
				}
			});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void showAlert(String message, String title, String exit ) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message)
				.setTitle(title)
				.setCancelable(false)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setNeutralButton(exit, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Main.this.finish();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void showMain() {

		Resources res = getResources();
		addPreferencesFromResource(R.xml.pr);

		Preference script_status = findPreference("script_status");
		script_status.setTitle(res.getString(R.string.script_installed));
		script_status.setSummary(
                res.getString(R.string.script_versions) + " " + res.getString(R.string.script_version)
        );
		script_status.setEnabled(false);

		for (String string : res.getStringArray(R.array.targets)) {
			Preference pref = findPreference(string);
			pref.setEnabled(true);

			File status = new File(SCRIPT_STATUS_DIR, string);
			if (status.exists()) {
				if (string.equals("app") || string.equals("app-private")) {
					pref.setSummary(res.getString(R.string.summary_pure_on));
					pref.setEnabled(false);
				} else {
					pref.setSummary(res.getString(R.string.summary_on));
				}
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Resources res = getResources();

        if (!checkScript(res.getString(R.string.script_version_md5))) {
            runInstallScript();
        } else {
            showMain();
        }

	}
}
