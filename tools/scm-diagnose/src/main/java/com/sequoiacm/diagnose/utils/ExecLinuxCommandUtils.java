package com.sequoiacm.diagnose.utils;

import com.sequoiacm.diagnose.common.ExecRes;
import com.sequoiacm.diagnose.execption.CollectException;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class ExecLinuxCommandUtils {
    private static final Logger logger = LoggerFactory.getLogger(ExecLinuxCommandUtils.class);

    public static ExecRes localExecuteCommand(String command) throws ScmToolsException {
        return localExecuteCommand(command, Arrays.asList(0));
    }

    public static ExecRes localExecuteCommand(String command, List<Integer> expectExitCode)
            throws ScmToolsException {
        try {
            return execCommand(command, expectExitCode);
        }
        catch (Exception e) {
            throw new ScmToolsException("local failed to exec shell command," + e.getMessage(),
                    CollectException.SHELL_EXEC_ERROR, e);
        }
    }

    private static ExecRes execCommand(String command, List<Integer> expectExitCode)
            throws IOException {
        int exitCode;
        String stdOut, stdErr;
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        String[] cmd = new String[3];
        cmd[0] = "/bin/sh";
        cmd[1] = "-c";
        cmd[2] = command;
        try {
            logger.debug("Executing command locally,command:{}", command);
            process = runtime.exec(cmd);
            stdOut = readStringFromStream(process.getInputStream());
            stdErr = readStringFromStream(process.getErrorStream());
            exitCode = process.waitFor();
        }
        catch (IOException | InterruptedException e) {
            throw new IOException("Failed to execute command locally,command=" + command, e);
        }
        finally {
            if (process != null) {
                process.destroy();
            }
        }
        if (!expectExitCode.contains(exitCode)) {
            throw new IOException("command:" + command + ",stderror:" + stdErr + ",stdout:" + stdOut
                    + ",exitCode:" + exitCode + ",expectExitCode:" + expectExitCode);
        }
        return new ExecRes(exitCode, stdOut, stdErr);
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
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (Exception e) {
                    logger.warn("Failed to close resource:{}", reader, e);
                }
            }
        }
        return sb.toString();
    }

    public static boolean unzip(String tarName, String outputPath) throws ScmToolsException {
        File tarFile = new File(tarName);
        if (!tarFile.exists()) {
            return false;
        }
        File outputFile = new File(outputPath);
        if (!outputFile.isDirectory()) {
            return false;
        }
        String command = "tar -zxvf " + tarName + " -C " + outputPath;
        ExecLinuxCommandUtils.localExecuteCommand(command);
        return true;
    }

    public static boolean zipFile(String tarName, String tarPath, List<String> logFile)
            throws ScmToolsException {
        if (logFile == null || logFile.isEmpty()) {
            return false;
        }

        File file = new File(tarPath);
        if (!file.isDirectory()) {
            return false;
        }

        StringBuilder builder = new StringBuilder();
        for (String logName : logFile) {
            builder.append(" " + logName);
        }
        String command = "tar --warning=no-file-changed -zcvf " + tarName + " -C " + tarPath
                + builder;
        ExecLinuxCommandUtils.localExecuteCommand(command, Arrays.asList(0, 1));
        return true;
    }

    public static boolean localUnzipNodeTar(String tarName, String tarOutputPath)
            throws ScmToolsException, IOException {
        File outputFile = new File(tarOutputPath);
        FileUtils.forceMkdir(outputFile);
        return ExecLinuxCommandUtils.unzip(tarName, tarOutputPath);
    }
}
