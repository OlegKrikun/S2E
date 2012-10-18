package ru.krikun.s2e;

import com.stericson.RootTools.Command;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

class Shell {

    private static com.stericson.RootTools.Shell shell = null;
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
