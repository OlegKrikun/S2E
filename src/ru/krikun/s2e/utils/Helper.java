package ru.krikun.s2e.utils;

import java.io.File;

public class Helper {

    private static final String MD5 = "772bda67d41504ce182707085fc82828";
    private static final String SCRIPT_DIST = "/data/local/userinit.d/simple2ext";
    private static final String S2E_CONFIG_DIR = "/data/local/s2e_config";
    private static final String S2E_DIR = "/data/data/ru.krikun.s2e";
    private static final String SCRIPT_STATUS_DIR = S2E_DIR + "/status";

    public static int[] getPartitionsInfo(String partition) {

        int[] intArray = new int[3];

        if (ShellInterface.isSuAvailable()) {

            String tmp = ShellInterface.getProcessOutput("busybox df /" + partition + " | busybox grep /" + partition);
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

    public static boolean checkStatus(String target) {
        File status = new File(Helper.SCRIPT_STATUS_DIR, target);
        return status.exists();
    }

    public static String getPath(String target) {
        if (target.equals("download")) return "/cache/" + target;
        else return "/data/" + target;
    }

    public static int getSize(String path) {
        if (ShellInterface.isSuAvailable()) {
            String output = ShellInterface.getProcessOutput("busybox du -s " + path);
            String[] array = output.split("[\\s]");
            if (!array[0].equals("")) {
                return Integer.parseInt(array[0]);
            }
        }
        return 0;
    }

    public static String convertSize(int size, String kb, String mb) {
        if (size == 0) return "--";
        else if (size >= 1024) return Integer.toString(size / 1024) + mb;
        else return size + kb;
    }

    public static void runReboot() {
        if (ShellInterface.isSuAvailable()) ShellInterface.runCommand("busybox sync && busybox umount /sd-ext && reboot");
    }

    public static boolean checkFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    private static String getSum(String target) {
        if (ShellInterface.isSuAvailable()) {
            String tmp_md5 = ShellInterface.getProcessOutput("busybox md5sum " + target);
            if (tmp_md5.length() >= 32) {
                return tmp_md5.substring(0, 32);
            }
        }
        return null;
    }

    private static boolean checkScriptSum() {
        String tmp_md5 = getSum(SCRIPT_DIST);
        if (tmp_md5 != null) {
           return tmp_md5.equals(MD5);
        }
        return false;
    }

    private static boolean checkScriptExists() {
        return checkFileExists(SCRIPT_DIST);
    }

    public static boolean checkScript() {
        return checkScriptExists() && checkScriptSum();
    }

    public static void copyScriptFinal() {
        if (ShellInterface.isSuAvailable()) {
            ShellInterface.runCommand("if [ ! -e /data/local/userinit.d ]; then busybox install -m 777 -o 1000 -g 1000 -d /data/local/userinit.d; fi");
            ShellInterface.runCommand("busybox cp " + S2E_DIR + "/files/script01.sh " + SCRIPT_DIST);
            ShellInterface.runCommand("busybox chmod 0777 " + SCRIPT_DIST);
        }
    }

	public static boolean compareSizes( int target, int partition) {
		return target != 0 && partition != 0 && target < partition;
	}

    public static boolean checkConfigDir() {
        if (!checkFileExists(S2E_CONFIG_DIR)) {
            if (ShellInterface.isSuAvailable()) ShellInterface.runCommand("busybox mkdir " + S2E_CONFIG_DIR);
            return checkFileExists(S2E_CONFIG_DIR);
        } else return true;
    }

    private static void createFile(String filePath) {
        if (ShellInterface.isSuAvailable()) ShellInterface.runCommand("busybox touch " + filePath);
    }

    private static void deleteFile(String filePath) {
        if (checkFileExists(filePath)) {
            if (ShellInterface.isSuAvailable()) ShellInterface.runCommand("busybox rm " + filePath);
        }
    }

    private static void writeToFile(String filePath, String string) {
        if (checkFileExists(filePath)) {
            if (ShellInterface.isSuAvailable()) ShellInterface.runCommand("busybox echo " + string + " > " + filePath);
        }
    }

    public static void createMountFile(String fileName) {
        String mountFile = S2E_CONFIG_DIR + "/." + fileName;
        createFile(mountFile);
    }

    public static void deleteMountFile(String fileName) {
        String mountFile = S2E_CONFIG_DIR + "/." + fileName;
        deleteFile(mountFile);
    }

    public static void createReadAheadFile() {
        String readAheadFile = S2E_CONFIG_DIR + "/.read_ahead";
        createFile(readAheadFile);
    }

    public static void deleteReadAheadFile() {
        String readAheadFile = S2E_CONFIG_DIR + "/.read_ahead";
        deleteFile(readAheadFile);
    }

    public static void writeReadAheadValue(String value) {
        String readAheadFile = S2E_CONFIG_DIR + "/.read_ahead";
        writeToFile(readAheadFile, value);
    }
}
