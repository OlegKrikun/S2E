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

import android.os.StatFs;
import android.util.Log;

import java.util.List;

class Partition {

    private final String path;

    private boolean root;

    private long size = 0;
    private long free = 0;
    private long used = 0;

    long getFree() {
        return free;
    }

    long getSize() {
        return size;
    }

    long getUsed() {
        return used;
    }

    public Partition(String name) {
        path = "/" + name;
        load();
    }

    public Partition(String name, boolean root) {
        this.root = root;
        path = "/" + name;
        load();
    }

    void refresh() {
        size = 0;
        free = 0;
        used = 0;
        load();
    }

    private void load() {
        if (root) loadOverShell();
        else loadOverAPI();
    }

    private void loadOverAPI() {
        try {
            StatFs statFs = new StatFs(path);

            long blockSize = statFs.getBlockSize();
            size = (statFs.getBlockCount() * blockSize) / 1024L;
            free = (statFs.getAvailableBlocks() * blockSize) / 1024L;
            used = size - free;
        } catch (IllegalArgumentException er) {
            Log.e(App.TAG, "IllegalArgumentException");
        }
    }

    private void loadOverShell() {
        List<String> output = App.sendShell("busybox df " + path);

        if (output != null) {
            String[] array = output.get(1).split("\\s+");
            if (array.length == 6) {
                // 1 - Size; 2 - Used; 3 - Free
                size = Long.parseLong(array[1]);
                free = Long.parseLong(array[3]);
                used = size - free;
            }
        }
    }
}
