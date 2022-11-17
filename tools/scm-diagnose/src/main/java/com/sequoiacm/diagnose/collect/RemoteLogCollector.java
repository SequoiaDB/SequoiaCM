package com.sequoiacm.diagnose.collect;

import com.sequoiacm.diagnose.command.ScmLogCollect;
import com.sequoiacm.diagnose.common.LogCollectResult;
import com.sequoiacm.diagnose.common.Services;
import com.sequoiacm.diagnose.config.LogCollectConfig;
import com.sequoiacm.diagnose.execption.LogCollectException;
import com.sequoiacm.diagnose.ssh.Ssh;
import com.sequoiacm.diagnose.utils.ExecLinuxCommandUtils;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RemoteLogCollector extends LogCollector {
    private Ssh ssh;

    public Ssh getSsh() {
        return ssh;
    }

    public void setSsh(Ssh ssh) {
        this.ssh = ssh;
    }

    private String collectCurrentServiceName = "";

    private static final Logger logger = LoggerFactory.getLogger(RemoteLogCollector.class);

    private String TEMP_PATH = "/tmp";

    @Override
    public LogCollectResult call() throws Exception {
        try {
            this.start();
            return new LogCollectResult(0, "remote host " + ssh.getHost() + " collect successful");
        }
        catch (ScmToolsException | IOException e) {
            return new LogCollectResult(-1,
                    "remote host " + ssh.getHost() + " collect failed :" + e.getMessage(),
                    (ScmToolsException) e);
        }
    }

    @Override
    public void start() throws ScmToolsException, IOException {
        System.out.println("[INFO ] remote host " + getSsh().getHost() + " start log collect");
        String tempCollectPath = TEMP_PATH + File.separator + LogCollectConfig.getResultDir();
        try {
            checkScmInstallPath();
            ssh.rmDir(tempCollectPath);
            ssh.mkdir(tempCollectPath);
            remoteCollectLogFile();
            ssh.rmDir(tempCollectPath + "*");
        }
        catch (ScmToolsException | IOException e) {
            throw e;
        }
        finally {
            ssh.disconnect();
        }
    }

    private boolean checkServiceInstall(String servicePath) throws ScmToolsException {
        try {
            ssh.checkExistDir(servicePath);
        }
        catch (ScmToolsException e) {
            if (e.getExitCode() == LogCollectException.FILE_NOT_FIND) {
                logger.info(LogCollectConfig.getServerMap().get(collectCurrentServiceName)
                        + " not install in remote host " + ssh.getHost());
                return false;
            }
            else {
                throw new ScmToolsException(e.getMessage(), e.getExitCode(), e);
            }
        }
        return true;
    }

    private void checkScmInstallPath() throws ScmToolsException {
        try {
            ssh.checkExistDir(LogCollectConfig.getInstallPath());
            List<String> lsFiles = ssh.lsFile(LogCollectConfig.getInstallPath());
            if (lsFiles.size() < 1) {
                throw new ScmToolsException(
                        "scm not install in remote host " + ssh.getHost() + " ,install path="
                                + LogCollectConfig.getInstallPath(),
                        LogCollectException.SCM_NOT_EXIST_ERROR);
            }
        }
        catch (ScmToolsException e) {
            if (e.getExitCode() == LogCollectException.FILE_NOT_FIND) {
                throw new ScmToolsException(
                        "scm install path is not exist or not install in remote host "
                                + ssh.getHost() + " ,install path="
                                + LogCollectConfig.getInstallPath(),
                        LogCollectException.SCM_NOT_EXIST_ERROR);
            }
            throw new ScmToolsException(e.getMessage(), e.getExitCode(), e);
        }
    }

    public void remoteCollectLogFile() throws ScmToolsException, IOException {
        for (String serverName : LogCollectConfig.getServiceList()) {
            collectCurrentServiceName = serverName;
            if (Services.Daemon.getServiceName().equals(serverName)) {
                collectDaemon();
                continue;
            }

            String serverInstallPath = LogCollectConfig.getInstallPath() + File.separator
                    + LogCollectConfig.getServerMap().get(serverName);
            // serviceInstallPath exist
            boolean isInstall = checkServiceInstall(serverInstallPath);
            if (!isInstall) {
                continue;
            }

            //ls node
            List<String> serviceNodes = ssh.lsFile(serverInstallPath);
            if (serviceNodes.size() < 1) {
                continue;
            }

            for (String nodeDir : serviceNodes) {
                String nodePath = serverInstallPath + File.separator + nodeDir;
                List<String> logFiles = ssh.lsCopyLogFile(collectCurrentServiceName, nodePath,
                        LogCollectConfig.getMaxLogCount());
                if (logFiles.size() < 1) {
                    continue;
                }
                copyNodeLogfiles(ssh.getHost() + "_" + nodeDir, nodePath, logFiles);
            }
        }
    }

    private void copyNodeLogfiles(String tarDir, String nodePath, List<String> logFiles)
            throws ScmToolsException, IOException {

        // zip
        String tempService = TEMP_PATH + File.separator + LogCollectConfig.getResultDir()
                + File.separator + collectCurrentServiceName;
        String tempTarName = tempService + File.separator + tarDir + ".tar.gz";
        ssh.mkdir(tempService);
        ssh.zipFile(tempTarName, nodePath, logFiles);

        // copy
        String LocalCollectPath = LogCollectConfig.getOutputPath() + File.separator
                + ScmLogCollect.currentCollectPath + File.separator + collectCurrentServiceName
                + File.separator + tarDir;
        FileUtils.forceMkdir(new File(LocalCollectPath));
        String LocalTarName = LocalCollectPath + File.separator + tarDir + ".tar.gz";
        ssh.copyFileFromRemote(tempTarName, LocalTarName);

        // unzip
        if (!LogCollectConfig.isNeedZipCopy()) {
            localUnzipNodeTar(LocalTarName, LocalCollectPath);
        }
    }

    public void localUnzipNodeTar(String tarName, String tarOutputPath)
            throws ScmToolsException, IOException {
        File outputFile = new File(tarOutputPath);
        FileUtils.forceMkdir(outputFile);

        boolean unzipResult = ExecLinuxCommandUtils.unzip(tarName, tarOutputPath);
        if (unzipResult) {
            File tarFile = new File(tarName);
            if (!tarFile.delete()) {
                logger.warn("file delete failed,path=" + tarFile.getAbsolutePath(),
                        LogCollectException.FILE_NOT_FIND);
            }
        }
    }

    private void collectDaemon() throws ScmToolsException, IOException {
        String daemonInstallPath = LogCollectConfig.getInstallPath() + File.separator
                + Services.Daemon.getServiceInstallPath();
        boolean isInstall = checkServiceInstall(daemonInstallPath);
        if (!isInstall) {
            return;
        }
        List<String> logFiles = ssh.lsCopyLogFile(Services.Daemon.getServiceName(), daemonInstallPath,
                LogCollectConfig.getMaxLogCount());
        if (logFiles.size() < 1) {
            return;
        }
        copyNodeLogfiles(ssh.getHost() + "_" + Services.Daemon.getServiceName(), daemonInstallPath,
                logFiles);
    }

}
