package com.sequoiacm.diagnose.collect;

import com.sequoiacm.diagnose.command.ScmLogCollect;
import com.sequoiacm.diagnose.common.CollectResult;
import com.sequoiacm.diagnose.common.Services;
import com.sequoiacm.diagnose.config.CollectConfig;
import com.sequoiacm.diagnose.execption.CollectException;
import com.sequoiacm.diagnose.utils.ExecLinuxCommandUtils;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class LocalLogCollector extends LogCollector {
    private static final Logger logger = LoggerFactory.getLogger(LocalLogCollector.class);
    private String hostAddress = "localhost";

    @Override
    public CollectResult call() throws Exception {
        try {
            this.start();
        }
        catch (Exception e) {
            return new CollectResult(-1, "local host collect log failed," + e.getMessage(), e);
        }
        return new CollectResult(0, "local host collect log successful");
    }

    @Override
    public void start() throws ScmToolsException, IOException {
        System.out.println("[INFO ] local host start log collect");
        if (!isScmInstallPath()) {
            System.out.println("[WARN ] local host " + CollectConfig.getInstallPath()
                    + " no scm is installed");
            logger.warn("local host " + CollectConfig.getInstallPath() + " no scm is installed");
            return;
        }
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            throw new ScmToolsException("local get ip failed", CollectException.SYSTEM_ERROR, e);
        }

        for (String serviceName : CollectConfig.getServiceList()) {

            if (Services.Daemon.getServiceName().equals(serviceName)) {
                collectDaemon();
                continue;
            }

            // service install path :gateway-----opt/sequoiacm/sequoiacm-cloud/log/gateway
            String servicePath = CollectConfig.getInstallPath() + File.separator
                    + CollectConfig.getServerMap().get(serviceName);
            if (!isExistDir(servicePath)) {
                logger.info("local host don't install " + serviceName);
                continue;
            }

            List<String> serviceNodeFile = lsSubDirectory(servicePath);
            // node not exist
            if (serviceNodeFile.size() < 1) {
                continue;
            }

            File destServiceFile = new File(CollectConfig.getOutputPath() + File.separator
                    + ScmLogCollect.currentCollectPath + File.separator + serviceName);
            FileUtils.forceMkdir(destServiceFile);

            // node path: opt/sequoiacm/sequoiacm-cloud/log/gateway/8080
            for (String nodeDir : serviceNodeFile) {
                String nodePath = servicePath + File.separator + nodeDir;

                // ls maxLogCount Logfile
                List<String> logFiles = lsCopyLogFile(nodePath, serviceName,
                        CollectConfig.getMaxLogCount());
                if (logFiles.size() < 1) {
                    continue;
                }
                copyNoteLogfiles(destServiceFile.getAbsolutePath(), hostAddress + "_" + nodeDir,
                        nodePath, logFiles);
            }
        }
        System.out.println("[INFO ] local host log collect finished");
    }

    private boolean isScmInstallPath() throws ScmToolsException {
        if (!isExistDir(CollectConfig.getInstallPath())
                || lsSubDirectory(CollectConfig.getInstallPath()).size() < 1) {
            return false;
        }
        boolean isScmInstallPath = false;
        for (Services value : Services.values()) {
            String installPath = CollectConfig.getInstallPath() + File.separator
                    + value.getServiceInstallPath();
            if (isExistDir(installPath)) {
                isScmInstallPath = true;
                break;
            }
        }
        return isScmInstallPath;
    }

    private void copyNoteLogfiles(String destServicePath, String destNodePath, String srcDir,
            List<String> logFiles) throws IOException, ScmToolsException {
        String outputPath = destServicePath + File.separator + destNodePath;
        File outputFile = new File(outputPath);
        FileUtils.forceMkdir(outputFile);
        if (CollectConfig.isNeedZipCopy()) {
            String tarName = outputPath + File.separator + destNodePath + ".tar.gz";
            boolean zipResult = ExecLinuxCommandUtils.zipFile(tarName, srcDir, logFiles);
            if (!zipResult) {
                throw new ScmToolsException("zipFile failed:,file=" + tarName,
                        CollectException.TAR_FILE_FAILED);
            }
        }
        else {
            for (String file : logFiles) {
                copyFile(srcDir + File.separator + file, outputPath + File.separator + file);
            }
        }
    }

    private void copyFile(String src, String dest) throws ScmToolsException, IOException {
        File srcFile = new File(src);
        if (!srcFile.exists()) {
            throw new ScmToolsException("copy file failed,srcFile not exist,path=" + src,
                    CollectException.FILE_NOT_FIND);
        }
        File destFile = new File(dest);
        FileUtils.forceMkdirParent(destFile);
        boolean createSuccessful = destFile.createNewFile();
        if (!createSuccessful) {
            throw new ScmToolsException(
                    "create file failed, file already exists,path=" + destFile.getAbsolutePath(),
                    CollectException.FILE_ALREADY_EXIST);
        }
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(src), "UTF-8"));
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest), "UTF-8"));
            String temp;
            while ((temp = br.readLine()) != null) {
                bw.write(temp);
                bw.newLine();
                bw.flush();
            }
        }
        catch (Exception e) {
            throw new ScmToolsException("copyFile file failed:,copy " + src + " to " + dest,
                    CollectException.COPY_FILE_FAILED, e);
        }
        finally {
            if (br != null) {
                br.close();
            }
            if (bw != null) {
                bw.close();
            }
        }
        logger.info("copyFile file successful: copy " + src + " to " + dest + " in local host");
    }

    private void collectDaemon() throws ScmToolsException, IOException {
        String daemonInstallPath = CollectConfig.getInstallPath() + File.separator
                + Services.Daemon.getServiceInstallPath();

        if (!isExistDir(daemonInstallPath)) {
            logger.info("local host don't install " + Services.Daemon.getServiceName());
            return;
        }

        List<String> logFiles = lsCopyLogFile(daemonInstallPath, Services.Daemon.getServiceName(),
                CollectConfig.getMaxLogCount());

        if (logFiles.size() < 1) {
            return;
        }

        File destDaemonDir = new File(CollectConfig.getOutputPath() + File.separator
                + ScmLogCollect.currentCollectPath + File.separator
                + Services.Daemon.getServiceName());
        FileUtils.forceMkdir(destDaemonDir);

        copyNoteLogfiles(destDaemonDir.getAbsolutePath(),
                hostAddress + "_" + Services.Daemon.getServiceName(),
                daemonInstallPath, logFiles);
    }

    private List<String> lsCopyLogFile(String path, String serviceName, int maxLogCount)
            throws ScmToolsException {
        File file = new File(path);
        List<String> logList = new ArrayList<>();
        if (!file.isDirectory()) {
            return logList;
        }
        File[] logFiles = file.listFiles();
        if (logFiles == null) {
            return logList;
        }
        Arrays.sort(logFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return (int) (f2.lastModified() - f1.lastModified());
            }
        });
        int logCount = 0;
        Services service = Services.getServices(serviceName);
        for (File logFile : logFiles) {
            String logFileName = logFile.getName();
            // Daemon will get all log files
            if (Services.Daemon.getServiceName().equals(serviceName)) {
                logList.add(logFileName);
                continue;
            }
            if (Pattern.matches("^error.*out$", logFileName)
                    || Pattern.matches(".*syserror.*", logFileName)) {
                logList.add(logFileName);
                continue;
            }
            if (Pattern.matches(service.getMatch(), logFileName)) {
                if (logCount == maxLogCount) {
                    continue;
                }
                logList.add(logFileName);
                logCount++;
            }

        }
        return logList;
    }

    private List<String> lsSubDirectory(String path) throws ScmToolsException {
        List<String> dirList = new ArrayList<>();
        if (!isExistDir(path)) {
            return dirList;
        }
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            return dirList;
        }
        for (File dirFile : files) {
            if (dirFile.isDirectory()) {
                dirList.add(dirFile.getName());
            }
        }
        return dirList;
    }

    private boolean isExistDir(String path) {
        File file = new File(path);
        return file.isDirectory();
    }

}
