package ru.krikun.s2e;

import android.os.StatFs;
import android.util.Log;
import ru.krikun.s2e.Helper;

import java.util.List;

public class Partition {

    private String path;

    private boolean root = false;
    private boolean loaded = false;

    private long size = 0;
    private long free = 0;
    private long used = 0;

    public boolean isLoaded() {
        return loaded;
    }

    public long getSize() {
        return size;
    }

    public long getFree() {
        return free;
    }

    public long getUsed() {
        return used;
    }

    public Partition(String path) {
        this.path = path;
        load();
    }

    public Partition(String path, boolean root) {
        this.root = root;
        this.path = path;
        load();
    }

    public void refresh() {
        size = 0;
        free = 0;
        used = 0;
        loaded = false;
        load();
    }

    private void load() {
        if (root) loadOverShell();
        else loadOverAPI();
    }

    private void loadOverShell() {
        List<String> output = Helper.sendShell("busybox df " + path);

        if (output != null) {
            String[] array = output.get(1).split("\\s+");
            if (array.length == 6) {
                // 1 - Size; 2 - Used; 3 - Free
                size = Integer.parseInt(array[1]);
                free = Integer.parseInt(array[3]);
                used = size - free;
                loaded = true;
            }
        }
    }

    private void loadOverAPI() {
        try {
            StatFs statFs = new StatFs(path);

            long blockSize = statFs.getBlockSize();
            size = (statFs.getBlockCount() * blockSize) / 1024L;
            free = (statFs.getAvailableBlocks() * blockSize) / 1024L;
            used = size - free;
            loaded = true;
        } catch (IllegalArgumentException er) {
            Log.e(Helper.TAG, "IllegalArgumentException");
        }
    }
}
