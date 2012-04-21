package ru.krikun.s2e;

import android.util.Log;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.RootToolsException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Helper {
    private static final String MD5 = "329fa77b73a113193342c5a6e46f0241";
    private static final String SCRIPT_DIST = "/data/local/userinit.d/simple2ext";
    private static final String S2E_CONFIG_DIR = "/data/local/s2e_config";
    private static final String S2E_DIR = "/data/data/ru.krikun.s2e";
    private static final String SCRIPT_STATUS_DIR = S2E_DIR + "/status";

    public static final String TAG = "S2E";

    //Check root and busybox
    public static boolean checkRoot() {
        try {
            return RootTools.isRootAvailable() && RootTools.isBusyboxAvailable() && RootTools.isAccessGiven();
        } catch (TimeoutException e) {
            return false;
        }
    }

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

    //Return md5sum of path
    private static String getSum(String path) {
        List<String> output = sendShell("busybox md5sum " + path);
        if (output != null) {
            if (output.get(0).length() >= 32) return output.get(0).substring(0, 32);
        }
        return null;
    }

    //Compare md5sum
    private static boolean checkScriptSum() {
        String tmp_md5 = getSum(SCRIPT_DIST);
        return tmp_md5 != null && tmp_md5.equals(MD5);
    }

    //Check script exists
    private static boolean checkScriptExists() {
        return checkFileExists(SCRIPT_DIST);
    }

    //Check script exists and control md5
    public static boolean checkScript() {
        return checkScriptExists() && checkScriptSum();
    }

    //Final actions of script installation
    //Check /data/local/userinit.d exists and create this if needed
    //Copy script and set permission
    public static void copyScriptFinal() {
        sendShell("if [ ! -e /data/local/userinit.d ]; then busybox install -m 777 -o 1000 -g 1000 -d /data/local/userinit.d; fi");
        sendShell("busybox cp " + S2E_DIR + "/files/script01.sh " + SCRIPT_DIST);
        sendShell("busybox chmod 0777 " + SCRIPT_DIST);
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
