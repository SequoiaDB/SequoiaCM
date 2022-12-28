package com.sequoiacm.deploy.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.SequoiadbTableInitializer;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.deployer.ServiceDeployer;
import com.sequoiacm.deploy.deployer.ServiceDeployerMgr;
import com.sequoiacm.deploy.installer.ServiceInstallerMgr;
import com.sequoiacm.deploy.installer.ServicesInstallPackManager;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallPackType;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshMgr;

public class ScmDeployer {
    private static final Logger logger = LoggerFactory.getLogger(ScmDeployer.class);
    private ScmDeployInfoMgr deployConfMgr = ScmDeployInfoMgr.getInstance();
    private SshMgr sshFactory = SshMgr.getInstance();
    private SequoiadbTableInitializer dbInitializer = SequoiadbTableInitializer.getInstance();
    private final ServicesInstallPackManager packManager = ServicesInstallPackManager.getInstance();

    public void deploy(boolean dryrun) throws Exception {
        ServiceInstallerMgr serviceInstallerMgr = ServiceInstallerMgr.getInstance();
        ServiceDeployerMgr deployerMgr = ServiceDeployerMgr.getInstance();

        // 初始化需要安装的目录
        String packPath = null;
        if (!dryrun) {
            packPath = prepareInstallPackage();
        }

        int progress = 0;
        progress += (InstallPackType.values().length) * deployConfMgr.getHosts().size();
        for (ServiceType serviceType : ServiceType.values()) {
            List<NodeInfo> nodes = deployConfMgr.getNodesByServiceType(serviceType);
            if (nodes != null) {
                progress += nodes.size();
            }
        }
        // Sending install Package
        progress += deployConfMgr.getHosts().size();

        // Initializing Sdb
        progress++;

        int currentProgress = 0;
        logger.info("Deploying service{}...({}/{})", dryrun ? "(Dry Run Mode)" : "",
                currentProgress++, progress);

        logger.info("Deploying service: initializing metasource and auditsource ({}/{})",
                currentProgress++, progress);
        dbInitializer.doInitialize(dryrun);

        for (HostInfo host : deployConfMgr.getHosts()) {
            logger.info("Deploying service: sending install package to {} ({}/{})",
                    host.getHostName(), currentProgress++, progress);
            if (!dryrun) {
                createInstallUserAndPath(host);
                sendInstallPack(host, packPath);
            }
            for (InstallPackType installPack : InstallPackType.values()) {
                logger.info("Deploying service: installing {} on {} ({}/{})", installPack,
                        host.getHostName(), currentProgress++, progress);
                if (dryrun) {
                    continue;
                }
                serviceInstallerMgr.install(host, installPack);
            }
        }

        for (ServiceType serviceType : ServiceType.getAllTyepSortByPriority()) {
            List<NodeInfo> nodes = deployConfMgr.getNodesByServiceType(serviceType);
            if (nodes != null) {
                for (NodeInfo node : nodes) {
                    logger.info("Deploying service: create {}({}) node on {} ({}/{})", serviceType,
                            node.getPort(), node.getHostName(), currentProgress++, progress);
                    if (dryrun) {
                        continue;
                    }
                    ServiceDeployer d = deployerMgr.getDeployer(serviceType);
                    d.deploy(node);
                }
            }
        }
        logger.info("Deploy service success");
    }

    private void createInstallUserAndPath(HostInfo host) throws Exception {
        Ssh ssh = sshFactory.getSsh(host);
        try {
            String installUser = deployConfMgr.getInstallConfig().getInstallUser();
            int isUserExist = ssh.exec("id -u " + installUser, 0, 1).getExitCode();
            if (isUserExist == 1) {
                String installPasswd = deployConfMgr.getInstallConfig().getInstallUserPassword();
                if (installPasswd == null || installPasswd.length() <= 0) {
                    throw new IllegalArgumentException("user not exist in " + host.getHostName()
                            + ", please specify password to create install user:" + installUser);
                }
                String userGroup = deployConfMgr.getInstallConfig().getInstallUserGroup();
                int isUserGroupExist = ssh.sudoExec("grep \"^" + userGroup + ":\" /etc/group",0,
                        1);
                if (isUserGroupExist == 1) {
                    ssh.sudoExec("groupadd " + userGroup);
                }
                ssh.sudoExec("useradd " + installUser + " -m -g " + userGroup);
                ssh.sudoExec("echo " + deployConfMgr.getInstallConfig().getInstallUser() + ":"
                        + installPasswd + " | chpasswd");
            }
            else {
                String group = ssh.getUserEffectiveGroup(installUser);
                try {
                    deployConfMgr.getInstallConfig().resetInstallUserGroup(group);
                }
                catch (Exception e) {
                    throw new Exception("failed to reset install user group, host=" + host.getHostName()
                                    + ", user=" + installUser + ", reset group=" + group, e);
                }
            }

            ssh.sudoExec("mkdir -p " + deployConfMgr.getInstallConfig().getInstallPath());

            ssh.changeOwner(deployConfMgr.getInstallConfig().getInstallPath(), installUser,
                    deployConfMgr.getInstallConfig().getInstallUserGroup());

            int hasJavaHome = ssh.sudoSuExec(installUser, "grep JAVA_HOME ~/.bashrc", null, 0, 1);
            if (hasJavaHome == 1) {
                ssh.sudoSuExec(installUser,
                        "echo \"export JAVA_HOME=" + host.getJavaHome() + "\" >> ~/.bashrc", null);
                ssh.sudoSuExec(installUser,
                        "echo \"export PATH=\\$JAVA_HOME/bin:\\$PATH\" >> ~/.bashrc", null);
            }
        }
        finally {
            ssh.close();
        }
    }

    private void sendInstallPack(HostInfo host, String packPath) throws Exception {
        Ssh ssh = sshFactory.getSsh(host);
        try {
            String targetPath = ssh.getScpTmpPath()
                    + CommonConfig.getInstance().getRemoteInstallPackPath();
            ssh.sudoExec("rm -rf " + targetPath);
            ssh.sudoExec("mkdir -p " + targetPath);
            ssh.scp(packPath, targetPath);
            ssh.sudoExec("tar -xf '" + targetPath + "/" + new File(packPath).getName() + "' -C "
                    + targetPath);
        }
        finally {
            CommonUtils.closeResource(ssh);
        }
    }

    private String prepareInstallPackage() {
        List<String> installFileList = new ArrayList<>();
        for (InstallPackType installPackType : InstallPackType.values()) {
            if (InstallPackType.NON_SERVICE == installPackType) {
                List<File> nonServiceFiles = packManager.getNonServiceFiles();
                for (File nonServiceFile : nonServiceFiles) {
                    installFileList.add(nonServiceFile.getName());
                }
            }
            else {
                File servicePack = packManager.getServicePack(installPackType);
                installFileList.add("package/" + servicePack.getName());
            }
        }
        return CommonUtils.packDirs(CommonConfig.getInstance().getBasePath(), "install.tar.gz",
                installFileList).getAbsolutePath();
    }
}
