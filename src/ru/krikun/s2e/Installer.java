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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Installer {

    private static final String MD5 =
            "329fa77b73a113193342c5a6e46f0241";
    private static final String SCRIPT_DIST =
            "/data/local/userinit.d/simple2ext";
    private static final String SHELL_MAKE_USERINIT_DIR =
            "if [ ! -e /data/local/userinit.d ]; then busybox install -m 777 -o 1000 -g 1000 -d /data/local/userinit.d; fi";
    private static final String SHELL_COPY_SCRIPT_TO_USERINIT_DIR =
            "busybox cp " + Helper.S2E_DIR + "/files/script01.sh " + SCRIPT_DIST;
    private static final String SHELL_SET_PERMISSION =
            "busybox chmod 0777 " + SCRIPT_DIST;
    private static final String SHELL_GET_MD5 =
            "busybox md5sum";

    private App app;
    private boolean installed;

    public boolean isInstalled() {
        return installed;
    }

    public Installer() {
        app = App.getInstance();

        if (!checkScript()) {
            copyScriptToHome();
            copyScriptToUserInit();
            if (checkScript()) installed = true;
        } else installed = true;
    }

    //Check script exists and control md5
    private boolean checkScript() {
        return checkScriptExists() && checkScriptSum();
    }

    //Final actions of script installation
    //Check /data/local/userinit.d exists and create this if needed
    //Copy script and set permission
    private void copyScriptToUserInit() {
        Helper.sendShell(SHELL_MAKE_USERINIT_DIR);
        Helper.sendShell(SHELL_COPY_SCRIPT_TO_USERINIT_DIR);
        Helper.sendShell(SHELL_SET_PERMISSION);
    }

    //Return md5sum of path
    private String getSum(String path) {
        List<String> output = Helper.sendShell(SHELL_GET_MD5 + " " + path);
        if (output != null) {
            if (output.get(0).length() >= 32) return output.get(0).substring(0, 32);
        }
        return null;
    }

    //Check script exists
    private boolean checkScriptExists() {
        return Helper.checkFileExists(SCRIPT_DIST);
    }

    //Compare md5sum
    private boolean checkScriptSum() {
        String tmp_md5 = getSum(SCRIPT_DIST);
        return tmp_md5 != null && tmp_md5.equals(MD5);
    }

    //Extract script and copy to app home dir
    private void copyScriptToHome() {
        try {
            InputStream in = app.getResources().getAssets().open("script01.sh");
            FileOutputStream out = app.openFileOutput("script01.sh", 0);
            if (in != null) {
                int size = in.available();
                byte[] buffer = new byte[size];
                in.read(buffer);
                out.write(buffer, 0, size);
                in.close();
                out.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
