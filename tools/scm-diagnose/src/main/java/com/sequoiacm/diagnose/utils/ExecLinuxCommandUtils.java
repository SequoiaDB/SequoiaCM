package com.sequoiacm.diagnose.utils;

import com.sequoiacm.diagnose.common.ExecRes;
import com.sequoiacm.diagnose.execption.LogCollectException;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class ExecLinuxCommandUtils {
    private static final Logger logger = LoggerFactory.getLogger(ExecLinuxCommandUtils.class);

    public static ExecRes execCommand(String command) throws IOException {
        int exitCode;
        String stdOut, stdErr;
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
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

    public static boolean unzip(String tarName, String outputPath)
            throws ScmToolsException, IOException {
        File tarFile = new File(tarName);
        if (!tarFile.exists()) {
            return false;
        }
        File outputFile = new File(outputPath);
        if (!outputFile.isDirectory()) {
            return false;
        }
        String command = "tar -zxvf " + tarName + " -C " + outputPath;
        ExecRes execRes = ExecLinuxCommandUtils.execCommand(command);
        if (execRes.getExitCode() != 0) {
            throw new ScmToolsException(execRes.getStdErr(), LogCollectException.SHELL_EXEC_ERROR);
        }
        return true;
    }

    public static boolean zipFile(String tarName, String tarPath, List<String> logFile)
            throws IOException, ScmToolsException {
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
        String command = "tar zcvf " + tarName + " -C " + tarPath + builder;
        ExecRes execRes = ExecLinuxCommandUtils.execCommand(command);
        if (execRes.getExitCode() != 0) {
            throw new ScmToolsException(execRes.getStdErr(), LogCollectException.SHELL_EXEC_ERROR);
        }
        return true;
    }
}
