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

package ru.krikun.s2e.old;

import java.util.HashMap;

public class TargetSet extends HashMap<String, Target> {

    //Update sizes for all targets
    void updateSizes() {
         for (Target target : this.values()) {
             target.updateSizes();
         }
    }

    //Update status for all targets
    void updateStatuses() {
        for (Target target : this.values()) {
            target.updateStatus();
        }
    }

    //Load target
    void loadTargets(String[] list) {
        for (String target : list) {
            this.put(target, new Target(target));
        }
    }
}
