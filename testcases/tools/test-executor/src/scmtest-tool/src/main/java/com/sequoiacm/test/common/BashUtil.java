package com.sequoiacm.test.common;

import com.sequoiacm.test.module.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class BashUtil {

    private final static Logger logger = LoggerFactory.getLogger(BashUtil.class);
    private static boolean isWindowsSystem = System.getProperty("os.name").contains("Windows");

    public static ExecResult exec(String command) throws IOException {
        int exitCode;
        String stdOut, stdErr;

        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            if (isWindowsSystem) {
                command = "cmd /c " + command;
            }
            logger.debug("Executing command locally, command:{}", command);
            process = runtime.exec(command);
            stdOut = readStringFromStream(process.getInputStream());
            stdErr = readStringFromStream(process.getErrorStream());
            exitCode = process.waitFor();
        }
        catch (IOException | InterruptedException e) {
            throw new IOException("Failed to execute command locally, command=" + command, e);
        }
        finally {
            if (process != null) {
                process.destroy();
            }
        }

        return new ExecResult(exitCode, stdOut, stdErr);
    }

    public static void searchProcessAndKill(String execCommand) {
        try {
            int pid = 0;
            ExecResult killResult = null;
            if (isWindowsSystem) {
                String searchCommand = "wmic process where name=\"cmd.exe\" get commandline,processid /value";
                String stdOut = exec(searchCommand).getStdOut().trim();

                // CommandLine=……
                // ProcessId=……
                //
                // CommandLine=……
                // ProcessId=……
                String s1 = StringUtil.subStringAfter(stdOut, execCommand);
                String s2 = StringUtil.subStringAfter(s1, "ProcessId=");
                String pidStr = StringUtil.subStringBefore(s2, System.lineSeparator());
                if (pidStr.length() > 0) {
                    pid = Integer.parseInt(pidStr);
                    logger.info("Killing the local testcase program, pid={}", pid);
                    killResult = exec("taskkill /pid " + pid + " /f /t ");
                }
            }
            else {
                ExecResult psResult = exec("ps -eo pid,cmd | grep -w \""
                        + StringUtil.subStringBefore(execCommand, " >")
                        + "\" | grep -v -e grep -e bash");
                String processDes = psResult.getStdOut().trim();
                String pidStr = processDes.substring(0, processDes.indexOf(" "));
                if (pidStr.length() > 0) {
                    pid = Integer.parseInt(pidStr);
                    logger.info("Killing the local testcase program, pid={}", pid);
                    killResult = exec("kill -9 " + pid);
                }
            }

            if (killResult != null) {
                if (killResult.getExitCode() == 0) {
                    logger.info("Kill the local test program success, pid={}", pid);
                }
                else {
                    throw new IOException(killResult.getStdErr());
                }
            }
        }
        catch (IOException e) {
            logger.error("Failed to check and kill the local testcase program, cause by: {}",
                    e.getMessage(), e);
        }
    }

    private static String readStringFromStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        finally {
            CommonUtil.closeResource(reader);
        }
        return sb.toString();
    }
}
