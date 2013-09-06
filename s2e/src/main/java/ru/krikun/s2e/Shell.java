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

import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

class Shell {

    private static com.stericson.RootTools.execution.Shell shell = null;
    private static boolean root;

    //Timeout for shell request
    private static final int SHELL_TIMEOUT = 60000;

    public boolean isRoot() {
        return root;
    }

    public Shell() {
        if (RootTools.isRootAvailable()) {
            try {
                shell = RootTools.getShell(true);
                root = RootTools.isAccessGiven();
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RootDeniedException e) {
                e.printStackTrace();
            }
        }
    }

    public List<String> run(String s) {
        Cmd command = new Cmd(s);
        if (shell != null) {
            try {
                shell.add(command).waitForFinish(SHELL_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return command.getResult();
    }

    private class Cmd extends Command {

        private final List<String> result = new ArrayList<String>();

        public List<String> getResult() {
            return result;
        }

        public Cmd(String... command) {
            super(0, command);
        }

        @Override
        public void output(int i, String s) {
            result.add(s);
        }
    }
}
