package com.sequoiacm.deploy.core;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.common.SequoiadbTableInitializer;
import com.sequoiacm.deploy.deployer.ServiceDeployer;
import com.sequoiacm.deploy.deployer.ServiceDeployerMgr;
import com.sequoiacm.deploy.installer.ServiceInstallerMgr;
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

    public void deploy(boolean dryrun) throws Exception {
        ServiceInstallerMgr serviceInstallerMgr = ServiceInstallerMgr.getInstance();
        ServiceDeployerMgr deployerMgr = ServiceDeployerMgr.getInstance();

        int progress = 0;
        Map<HostInfo, List<InstallPackType>> hostToServices = deployConfMgr.getServiceOnHost();
        for (Entry<HostInfo, List<InstallPackType>> entry : hostToServices.entrySet()) {
            List<InstallPackType> services = entry.getValue();
            progress += services.size();
        }
        for (ServiceType serviceType : ServiceType.values()) {
            List<NodeInfo> nodes = deployConfMgr.getNodesByServiceType(serviceType);
            if (nodes != null) {
                progress += nodes.size();
            }
        }
        // Initializing Sdb
        progress++;

        int currentProgress = 0;
        logger.info("Deploying service{}...({}/{})", dryrun ? "(Dry Run Mode)" : "",
                currentProgress++, progress);

        logger.info("Deploying service: initializing metasource and auditsource ({}/{})",
                currentProgress++, progress);
        dbInitializer.doInitialize(dryrun);

        for (Entry<HostInfo, List<InstallPackType>> entry : hostToServices.entrySet()) {
            HostInfo host = entry.getKey();
            if (!dryrun) {
                createInstallUserAndPath(host);
            }
            List<InstallPackType> services = entry.getValue();
            for (InstallPackType service : services) {
                logger.info("Deploying service: installing {} on {} ({}/{})", service,
                        host.getHostName(), currentProgress++, progress);
                if (dryrun) {
                    continue;
                }
                serviceInstallerMgr.install(host, service);
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

    private void createInstallUserAndPath(HostInfo host) throws IOException {
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
                int isUserGroupExist = ssh.sudoExec("grep \"^" + userGroup + ":\" /etc/group", 0,
                        1);
                if (isUserGroupExist == 1) {
                    ssh.sudoExec("groupadd " + userGroup);
                }
                ssh.sudoExec("useradd " + installUser + " -m -g " + userGroup);
                ssh.sudoExec("echo " + deployConfMgr.getInstallConfig().getInstallUser() + ":"
                        + installPasswd + " | sudo chpasswd");
            }
            else {
                String group = ssh.getUserEffectiveGroup(installUser);
                deployConfMgr.getInstallConfig().resetInstallUserGroup(group);
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

}
