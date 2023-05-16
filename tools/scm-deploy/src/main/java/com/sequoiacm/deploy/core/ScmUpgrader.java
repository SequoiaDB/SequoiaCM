package com.sequoiacm.deploy.core;

import com.sequoiacm.deploy.command.SubOption;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.exception.UpgradeException;
import com.sequoiacm.deploy.installer.ServicesInstallPackManager;
import com.sequoiacm.deploy.module.BasicInstallConfig;
import com.sequoiacm.deploy.module.ConfigInfo;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallPackType;
import com.sequoiacm.deploy.module.NodeStatus;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.module.StatusInfo;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshMgr;
import com.sequoiacm.deploy.upgrader.ServiceUpgraderMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScmUpgrader {
    private static final Logger logger = LoggerFactory.getLogger(ScmUpgrader.class);
    private ScmUpgradeInfoMgr upgradeInfoMgr = ScmUpgradeInfoMgr.getInstance();
    private final ServicesInstallPackManager packManager = ServicesInstallPackManager.getInstance();
    private BasicInstallConfig installConfig = upgradeInfoMgr.getInstallConfig();

    public void upgrade(boolean dryrun) throws Exception {
        Map<InstallPackType, String> installPackToNewVersion = upgradeInfoMgr
                .getInstallPackToNewVersion();

        // 1. 当配置文件命令行指定了服务，但指定的主机上都不存在该服务，则打印在集群中不存在的服务
        List<ServiceType> noExistServices = upgradeInfoMgr.getNoExistServices();
        if (!noExistServices.isEmpty()) {
            logger.warn("The following specified service do not exist in the cluster:{}",
                    noExistServices);
        }

        // 2. 当服务的当前版本与升级后的版本一致或着更高时，则不符合升级要求，打印这些主机上的服务，不作升级
        Map<ServiceType, Map<HostInfo, String>> unableUpgradeServiceHost = upgradeInfoMgr
                .getUnableUpgradeServiceHost();
        if (!unableUpgradeServiceHost.isEmpty()) {
            logger.warn(
                    "The current version of the following services is higher or the same than the new version,not be upgraded");
            for (ServiceType service : ServiceType.getAllTyepSortByPriority()) {
                Map<HostInfo, String> hostToVersion = unableUpgradeServiceHost.get(service);
                if (hostToVersion == null) {
                    continue;
                }
                for (Map.Entry<HostInfo, String> entry : hostToVersion.entrySet()) {
                    HostInfo host = entry.getKey();
                    String oldversion = entry.getValue();
                    logger.warn("[" + host.getHostName() + "]:" + service + ":" + oldversion
                            + " to " + installPackToNewVersion.get(service.getInstllPack()));
                }
            }
        }

        Map<ServiceType, Map<HostInfo, String>> upgradeServiceHost = upgradeInfoMgr
                .getUpgradeServiceHost();

        // 3. 当需要升级的服务为空,直接告警退出
        if (upgradeServiceHost.isEmpty()) {
            logger.warn("No service need to upgrade");
            return;
        }

        // 4. 不为空，打印需升级的服务
        logger.info("Prepare to upgrade services in this order");
        int progress = 0;
        for (ServiceType service : ServiceType.getAllTyepSortByPriority()) {
            Map<HostInfo, String> hostVersion = upgradeServiceHost.get(service);
            if (hostVersion == null) {
                continue;
            }
            progress += hostVersion.size();
            for (Map.Entry<HostInfo, String> entry : hostVersion.entrySet()) {
                HostInfo host = entry.getKey();
                String oldVersion = entry.getValue();
                logger.info("[" + host.getHostName() + "]:" + service + ":" + oldVersion + " to "
                        + installPackToNewVersion.get(service.getInstllPack()));
            }
        }
        progress += upgradeInfoMgr.getUpgradeHostService().keySet().size();

        // set global config
        progress++;

        // 5. 让用户确定是否升级
        boolean isUnattended = CommonConfig.getInstance()
                .getSubOptionValue(SubOption.UNATTENDED) != null;

        if (!isUnattended && !CommonUtils.confirmExecute("upgrade")) {
            return;
        }

        // 6.准备升级脚本和tar包
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date());
        upgradeInfoMgr.getConfigInfo().setUpgradePackPath(
                upgradeInfoMgr.getConfigInfo().getUpgradePackPath() + "/upgrade-" + timestamp);
        String packPath = null;
        if (!dryrun) {
            packPath = prepareUpgradePackage();
        }

        int currentProgress = 0;
        logger.info("Upgrading service{}...({}/{})", dryrun ? "(Dry Run Mode)" : "",
                currentProgress++, progress);
        for (HostInfo host : upgradeInfoMgr.getUpgradeHostService().keySet()) {
            logger.info("Upgrading Service: sending upgrade package to {} ({}/{})",
                    host.getHostName(), currentProgress++, progress);
            if (dryrun) {
                continue;
            }
            sendUpgradePackage(host, packPath);
        }

        // 7. 升级前准备
        String backupPath = upgradeInfoMgr.getConfigInfo().getBackupPath() + "/backup-" + timestamp;
        upgradeInfoMgr.getConfigInfo().setBackupPath(backupPath);
        File upgradeStatusFile;
        UpgradeStatusWriter writer = null;
        if (!dryrun) {
            upgradeStatusFile = createUpgradeStatusFile(timestamp);
            writer = new UpgradeStatusWriter(new FileWriter(upgradeStatusFile));
            writer.writeBeforeUpgrade(installConfig,
                    upgradeInfoMgr.getUpgradeHostService().keySet(),
                    upgradeInfoMgr.getConfigInfo());
        }
        else {
            upgradeStatusFile = new File(CommonConfig.getInstance().getUpgradeStatusDirPath()
                    + "/upgrade_status_" + timestamp);
        }
        ServiceUpgraderMgr upgraderMgr = ServiceUpgraderMgr.getInstance();
        try {
            for (ServiceType serviceType : ServiceType.getAllTyepSortByPriority()) {
                Map<HostInfo, String> hostVersion = upgradeServiceHost.get(serviceType);
                if (hostVersion == null) {
                    continue;
                }
                for (Map.Entry<HostInfo, String> entry : hostVersion.entrySet()) {
                    HostInfo host = entry.getKey();
                    String oldVersion = entry.getValue();
                    logger.info("Upgrading Service: upgrading {} on {} ({}/{})", serviceType,
                            host.getHostName(), currentProgress++, progress);
                    if (dryrun) {
                        continue;
                    }
                    List<NodeStatus> nodeStatusList = upgraderMgr.getNodeStatus(host, serviceType);
                    String newVersion = installPackToNewVersion.get(serviceType.getInstllPack());
                    StatusInfo statusInfo = new StatusInfo(host.getHostName(), serviceType,
                            nodeStatusList, backupPath, oldVersion, newVersion);
                    writer.writeStatusRecord(statusInfo);
                    try {
                        upgraderMgr.upgrade(statusInfo);
                    }
                    catch (Exception e) {
                        logger.info(
                                "If you want to rollback successfully upgraded services,you need to specify the upgrade status file,file location:"
                                        + upgradeStatusFile.getAbsolutePath());
                        throw e;
                    }
                }
            }
        }
        finally {
            if (writer != null) {
                writer.close();
            }
        }
        logger.info("Setting Global Config...({}/{})", currentProgress++, progress);
        if (!dryrun && ScmUpgradeInfoMgr.getInstance().getGlobalConfig() != null) {
            new ScmGlobalConfigSetter(upgradeInfoMgr.getGlobalConfig(),
                    upgradeInfoMgr.getConfigInfo().getScmGateway(),
                    upgradeInfoMgr.getConfigInfo().getScmUser(),
                    upgradeInfoMgr.getConfigInfo().getScmPassword()).setGlobalConfigSilence();
        }

        logger.info("Upgrade service success, generate upgrade status file success, file location:"
                + upgradeStatusFile.getAbsolutePath());
    }

    private String prepareUpgradePackage() {
        List<String> upgradeFileList = new ArrayList<>();
        File upgradeScript = new File(CommonConfig.getInstance().getLocalUpgradeScript());
        if (!upgradeScript.exists()) {
            throw new UpgradeException(
                    "fail to find upgrade script:path=" + upgradeScript.getAbsolutePath());
        }
        upgradeFileList.add(upgradeScript.getName());
        for (InstallPackType installPackType : InstallPackType.values()) {
            if (installPackType == InstallPackType.NON_SERVICE) {
                List<File> nonServiceDirs = packManager.getNonServiceFiles();
                for (File nonServiceFile : nonServiceDirs) {
                    upgradeFileList.add(nonServiceFile.getName());
                }
            }
            else {
                File installPack = packManager.getServicePack(installPackType);
                upgradeFileList.add("package/" + installPack.getName());
            }
        }
        return CommonUtils.packDirs(CommonConfig.getInstance().getBasePath(), "upgrade.tar.gz",
                upgradeFileList).getAbsolutePath();
    }

    private void sendUpgradePackage(HostInfo host, String packPath) throws Exception {
        Ssh ssh = null;
        try {
            String upgradePackPath = upgradeInfoMgr.getConfigInfo().getUpgradePackPath();
            String backupPath = upgradeInfoMgr.getConfigInfo().getBackupPath();
            ssh = SshMgr.getInstance().getSsh(host);
            String group = ssh.getUserEffectiveGroup(installConfig.getInstallUser());
            // 1. 检查 upgradePackPath 是否已经存在
            boolean isExists = ssh.sudoExec("ls " + upgradePackPath, 0, 2) == 0;
            if (isExists) {
                throw new IllegalArgumentException(
                        "failed to send upgrade package, dir already exist: host="
                                + host.getHostName() + ", dir=" + upgradePackPath);
            }

            ssh.sudoExec("mkdir -p " + upgradePackPath);

            // 2. 传送升级包到host临时目录 并解压到upgradePackPath，改变upgradePackPath所属用户和用户组
            ssh.scp(packPath, ssh.getScpTmpPath());
            ssh.sudoExec("tar -xf " + ssh.getScpTmpPath() + "/" + new File(packPath).getName()
                    + " -C " + upgradePackPath);
            ssh.changeOwner(upgradePackPath, installConfig.getInstallUser(), group);

            // 3. 授予upgradePackPath下升级脚本执行权限
            String upgradeScriptPath = upgradePackPath + "/"
                    + new File(CommonConfig.getInstance().getLocalUpgradeScript()).getName();
            ssh.sudoSuExec(installConfig.getInstallUser(), "chmod +x " + upgradeScriptPath, null);

            // 4. 创建新的备份目录
            ssh.sudoExec("mkdir -p " + backupPath);
            ssh.changeOwner(backupPath, installConfig.getInstallUser(), group);
        }
        finally {
            CommonUtils.closeResource(ssh);
        }
    }

    private File createUpgradeStatusFile(String timestamp) throws Exception {
        String upgradeStatusDirPath = CommonConfig.getInstance().getUpgradeStatusDirPath();
        File upgradeStatusDir = new File(upgradeStatusDirPath);
        if (!upgradeStatusDir.exists() && !upgradeStatusDir.mkdir()) {
            throw new UpgradeException(
                    "failed to create upgrade status dir,dir=" + upgradeStatusDir);
        }
        String upgradeStatusFilePath = upgradeStatusDirPath + "/upgrade_status_" + timestamp;
        File upgradeStatusFile = new File(upgradeStatusFilePath);
        if (!upgradeStatusFile.createNewFile()) {
            throw new UpgradeException(
                    "failed to create upgrade status file,file=" + upgradeStatusFilePath);
        }
        return upgradeStatusFile;
    }

    private static class UpgradeStatusWriter {
        private BufferedWriter bfw;

        public UpgradeStatusWriter(Writer writer) {
            this.bfw = new BufferedWriter(writer);
        }

        private void writeInstallConfig(BasicInstallConfig installConfig) throws Exception {
            bfw.write("[" + ConfFileDefine.SEACTION_INSTALLCONFIG + "]" + "\n");
            bfw.write(ConfFileDefine.INSTALLCONFIG_PATH + ",  ");
            bfw.write(ConfFileDefine.INSTALLCONFIG_USER + "\n");
            bfw.write(installConfig.getInstallPath() + ",  ");
            bfw.write(installConfig.getInstallUser() + "\n");
        }

        private void writeHostInfo(Set<HostInfo> hosts) throws Exception {
            bfw.write("[" + ConfFileDefine.SEACTION_HOST + "]" + "\n");
            bfw.write(ConfFileDefine.HOST_HOSTNAME + ",  ");
            bfw.write(ConfFileDefine.HOST_SSH_PORT + ",  ");
            bfw.write(ConfFileDefine.HOST_USER + ",  ");
            bfw.write(ConfFileDefine.HOST_PASSWORD + ",  ");
            bfw.write(ConfFileDefine.HOST_JAVA_NOME + "\n");
            for (HostInfo host : hosts) {
                bfw.write(host.getHostName() + ",  ");
                bfw.write(host.getPort() + ",  ");
                bfw.write(host.getUserName() + ",  ");
                bfw.write(host.getPassword() + ",  ");
                bfw.write(host.getJavaHome() + "\n");
            }
        }

        private void writeConfigInfo(ConfigInfo configInfo) throws Exception {
            bfw.write("[" + ConfFileDefine.SEACTION_CONFIG + "]" + "\n");
            bfw.write(ConfFileDefine.CONFIG_UPGRADE_PACK_PATH + "="
                    + configInfo.getUpgradePackPath() + "\n");
            bfw.write(ConfFileDefine.CONFIG_BACKUP_PATH + "=" + configInfo.getBackupPath() + "\n");
        }

        private void writeStatusHeader() throws Exception {
            bfw.write("[" + ConfFileDefine.SEACTION_STATUS + "]" + "\n");
            bfw.write(ConfFileDefine.STATUS_HOSTNAME + ",  ");
            bfw.write(ConfFileDefine.STATUS_SERVICE + ",  ");
            bfw.write(ConfFileDefine.STATUS_NODE_STATUS + ",  ");
            bfw.write(ConfFileDefine.STATUS_BACKUP + ",  ");
            bfw.write(ConfFileDefine.STATUS_OLD_VERSION + ",  ");
            bfw.write(ConfFileDefine.STATUS_NEW_VERSION);
        }

        public void writeBeforeUpgrade(BasicInstallConfig installConfig, Set<HostInfo> hostInfos,
                ConfigInfo configInfo) throws Exception {
            writeInstallConfig(installConfig);
            writeHostInfo(hostInfos);
            writeConfigInfo(configInfo);
            writeStatusHeader();
            bfw.flush();
        }

        public void writeStatusRecord(StatusInfo statusInfo) throws Exception {
            String serviceBackupPath;
            if (ServiceType.NON_SERVICE == statusInfo.getType()) {
                serviceBackupPath = statusInfo.getBackupPath() + "/"
                        + statusInfo.getType().getType() + "/backup";
            }
            else if (InstallPackType.CLOUD == statusInfo.getType().getInstllPack()) {
                serviceBackupPath = statusInfo.getBackupPath() + "/"
                        + statusInfo.getType().getInstllPack().getUntarDirName() + "/"
                        + statusInfo.getType().getType() + "/backup";
            }
            else {
                serviceBackupPath = statusInfo.getBackupPath() + "/"
                        + statusInfo.getType().getInstllPack().getUntarDirName() + "/backup";
            }
            StringBuilder serviceStatusStr = new StringBuilder();
            for (int i = 0; i < statusInfo.getNodeStatus().size(); i++) {
                NodeStatus nodeStatus = statusInfo.getNodeStatus().get(i);
                serviceStatusStr.append(nodeStatus.getPort()).append(":")
                        .append(nodeStatus.isStart() ? "start" : "stop");
                if (i < statusInfo.getNodeStatus().size() - 1) {
                    serviceStatusStr.append(";");
                }
            }
            String statusRecord = statusInfo.getHostName() + ",  " + statusInfo.getType().getType()
                    + ",  " + serviceStatusStr + ",  " + serviceBackupPath + ",  "
                    + statusInfo.getOldVersion() + ",  " + statusInfo.getNewVersion();
            bfw.write("\n" + statusRecord);
            bfw.flush();
        }

        public void close() throws Exception {
            bfw.close();
        }
    }
}
