package com.sequoiacm.diagnose.collect;

import com.sequoiacm.diagnose.command.ScmLogCollect;
import com.sequoiacm.diagnose.common.CollectResult;
import com.sequoiacm.diagnose.common.Services;
import com.sequoiacm.diagnose.config.CollectConfig;
import com.sequoiacm.diagnose.execption.CollectException;
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
    public CollectResult call() throws Exception {
        try {
            this.start();
            return new CollectResult(0, "remote host " + ssh.getHost() + " collect successful");
        }
        catch (Exception e) {
            return new CollectResult(-1,
                    "remote host " + ssh.getHost() + " collect failed:" + e.getMessage(), e);
        }
    }

    @Override
    public void start() throws ScmToolsException, IOException {
        System.out.println("[INFO ] remote host " + getSsh().getHost() + " start log collect");
        String tempCollectPath = TEMP_PATH + File.separator + CollectConfig.getResultDir();
        try {
            if (!isScmInstallPath()) {
                System.out.println("[WARN ] remote host " + ssh.getHost() + " "
                        + CollectConfig.getInstallPath() + " no scm is installed");
                logger.warn("remote host " + ssh.getHost() + " " + CollectConfig.getInstallPath()
                        + " no scm is installed");
                return;
            }
            ssh.rmDir(tempCollectPath);
            ssh.mkdir(tempCollectPath);
            remoteCollectLogFile();
            ssh.rmDir(tempCollectPath + "*");
            System.out
                    .println("[INFO ] remote host " + getSsh().getHost() + " log collect finished");
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
            if (e.getExitCode() == CollectException.FILE_NOT_FIND) {
                logger.info(CollectConfig.getServerMap().get(collectCurrentServiceName)
                        + " not install in remote host " + ssh.getHost());
                return false;
            }
            else {
                throw new ScmToolsException(e.getMessage(), e.getExitCode(), e);
            }
        }
        return true;
    }

    private boolean isScmInstallPath() throws ScmToolsException {
        try {
            ssh.checkExistDir(CollectConfig.getInstallPath());
            List<String> lsFiles = ssh.lsFile(CollectConfig.getInstallPath());
            if (lsFiles.size() < 1) {
                return false;
            }
        }
        catch (ScmToolsException e) {
            if (e.getExitCode() == CollectException.FILE_NOT_FIND) {
                return false;
            }
            throw new ScmToolsException(e.getMessage(), e.getExitCode(), e);
        }
        boolean isScmInstallPath = false;
        for (Services value : Services.values()) {
            String installPath = CollectConfig.getInstallPath() + File.separator
                    + value.getServiceInstallPath();
            try {
                ssh.checkExistDir(installPath);
                isScmInstallPath = true;
                break;
            }
            catch (ScmToolsException e) {
                if (e.getExitCode() == CollectException.FILE_NOT_FIND) {
                    continue;
                }
                else {
                    throw e;
                }
            }
        }
        return isScmInstallPath;
    }

    public void remoteCollectLogFile() throws ScmToolsException, IOException {
        for (String serverName : CollectConfig.getServiceList()) {
            collectCurrentServiceName = serverName;
            if (Services.Daemon.getServiceName().equals(serverName)) {
                collectDaemon();
                continue;
            }

            String serverInstallPath = CollectConfig.getInstallPath() + File.separator
                    + CollectConfig.getServerMap().get(serverName);
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
                List<String> logFiles = ssh.lsCopyLogFile(serverName, nodePath,
                        CollectConfig.getMaxLogCount());
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
        String tempService = TEMP_PATH + File.separator + CollectConfig.getResultDir()
                + File.separator + collectCurrentServiceName;
        String tempTarName = tempService + File.separator + tarDir + ".tar.gz";
        ssh.mkdir(tempService);
        ssh.zipFile(tempTarName, nodePath, logFiles);

        // copy
        String LocalCollectPath = CollectConfig.getOutputPath() + File.separator
                + ScmLogCollect.currentCollectPath + File.separator + collectCurrentServiceName
                + File.separator + tarDir;
        FileUtils.forceMkdir(new File(LocalCollectPath));
        String LocalTarName = LocalCollectPath + File.separator + tarDir + ".tar.gz";
        ssh.copyFileFromRemote(tempTarName, LocalTarName);

        // unzip
        if (!CollectConfig.isNeedZipCopy()) {
            boolean result = ExecLinuxCommandUtils.localUnzipNodeTar(LocalTarName,
                    LocalCollectPath);
            if (result) {
                File tarFile = new File(LocalTarName);
                if (!tarFile.delete()) {
                    logger.warn("file delete failed,path=" + tarFile.getAbsolutePath(),
                            CollectException.FILE_NOT_FIND);
                }
            }
        }
    }


    private void collectDaemon() throws ScmToolsException, IOException {
        String daemonInstallPath = CollectConfig.getInstallPath() + File.separator
                + Services.Daemon.getServiceInstallPath();
        boolean isInstall = checkServiceInstall(daemonInstallPath);
        if (!isInstall) {
            return;
        }
        List<String> logFiles = ssh.lsCopyLogFile(Services.Daemon.getServiceName(), daemonInstallPath,
                CollectConfig.getMaxLogCount());
        if (logFiles.size() < 1) {
            return;
        }
        copyNodeLogfiles(ssh.getHost() + "_" + Services.Daemon.getServiceName(), daemonInstallPath,
                logFiles);
    }

}
