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

import android.util.Log;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.RootToolsException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Helper {

    public static final String TAG = "S2E";
    public static final String S2E_DIR = "/data/data/ru.krikun.s2e";

    private static final String S2E_CONFIG_DIR = "/data/local/s2e_config";
    private static final String SCRIPT_STATUS_DIR = S2E_DIR + "/status";

    //Send command to shell
    //if return code equals 1, return null
    public static List<String> sendShell(String str) {
        try {
            List<String> output = RootTools.sendShell(str, 5000);
            if (!output.get(output.size() - 1).equals("1")) return output;
            else Log.e(TAG, "Error in shell: " + str + "; return code '1'");
        } catch (IOException e) {
            Log.e(TAG, "Error in shell: " + str + "; IOException");
        } catch (RootToolsException e) {
            Log.e(TAG, "Error in shell: " + str + "; RootToolsException");
        } catch (TimeoutException e) {
            Log.e(TAG, "Error in shell: " + str + "; TimeoutException");
        }
        return null;
    }

    //Dynamical convert size to MB or KB
    public static String convertSize(int size, String kb, String mb) {
        if (size == 0) return "--";
        else if (size >= 1024) return Integer.toString(size / 1024) + mb;
        else return size + kb;
    }

    //Check file exists
    public static boolean checkFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    //Compare sizes of partitions and dir
    public static boolean compareSizes(int size, int free) {
        return size != 0 && free != 0 && size < free;
    }

    //Create file
    private static void createFile(String filePath) {
        sendShell("busybox touch " + filePath);
    }

    //Delete file if file exists
    private static void deleteFile(String filePath) {
        if (checkFileExists(filePath)) sendShell("busybox rm " + filePath);
    }

    //Write to file if file exists
    private static void writeToFile(String filePath, String string) {
        if (checkFileExists(filePath)) sendShell("busybox echo " + string + " > " + filePath);
    }

    //Create dir
    private static void createDir(String path) {
        sendShell("busybox mkdir " + path);
    }

    //Create status file
    //if status file dir not exists, then create this dir
    public static void createStatusFile(String target) {
        if (!checkFileExists(SCRIPT_STATUS_DIR)) createDir(SCRIPT_STATUS_DIR);
        createFile(SCRIPT_STATUS_DIR + "/" + target);
    }

    //Check config dir exists and create this if needed
    public static boolean checkConfigDir() {
        if (!checkFileExists(S2E_CONFIG_DIR)) {
            createDir(S2E_CONFIG_DIR);
            return checkFileExists(S2E_CONFIG_DIR);
        } else return true;
    }

    //Create mount file
    public static void createMountFile(String fileName) {
        createFile(S2E_CONFIG_DIR + "/." + fileName);
    }

    //Delete mount file
    public static void deleteMountFile(String fileName) {
        deleteFile(S2E_CONFIG_DIR + "/." + fileName);
    }

    //Create ReadAhead file
    public static void createReadAheadFile() {
        createFile(S2E_CONFIG_DIR + "/.read_ahead");
    }

    //Delete ReadAhead file
    public static void deleteReadAheadFile() {
        deleteFile(S2E_CONFIG_DIR + "/.read_ahead");
    }

    //Write value to ReadAhead file
    public static void writeReadAheadValue(String value) {
        writeToFile(S2E_CONFIG_DIR + "/.read_ahead", value);
    }

    public static boolean checkStatusFileExists(String target) {
        File status = new File(Helper.SCRIPT_STATUS_DIR, target);
        return status.exists();
    }
}
