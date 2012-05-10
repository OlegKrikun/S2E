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

import java.util.HashMap;

public class PartitionsSet extends HashMap<String, Partition> {

    //Load partitions to set
    public void loadPartitions(String[] list, boolean isICS) {
        for (String name : list) {
            //For ext partition use shell request (if not ICS) 
            if (name.equals("sd-ext") && !isICS) {
                this.put(name, new Partition(name, true));
            } else {
                this.put(name, new Partition(name));
            }
        }
    }

    //Update partitions information's in set
    public void update() {
        for (Partition partition : this.values()) {
            partition.refresh();
        }
    }
}
