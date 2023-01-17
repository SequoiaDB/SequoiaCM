package com.sequoiacm.deploy.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.deploy.module.BasicInstallConfig;
import com.sequoiacm.deploy.module.VersionFileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.command.SubOption;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.exception.RollbackException;
import com.sequoiacm.deploy.module.ConfigInfo;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallConfig;
import com.sequoiacm.deploy.module.JavaVersion;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.module.StatusInfo;
import com.sequoiacm.deploy.parser.ScmConfParser;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshExecRes;
import com.sequoiacm.deploy.ssh.SshMgr;

public class ScmUpgradeStatusInfoMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmUpgradeStatusInfoMgr.class);
    private static volatile ScmUpgradeStatusInfoMgr instance;
    private SshMgr sshMgr;

    private BasicInstallConfig installConfig;
    private List<StatusInfo> availableStatusInfos;
    private Map<ServiceType, List<StatusInfo>> availableServiceToStatus;
    private List<HostInfo> hosts;
    private Map<String, HostInfo> hostInfoMap = new HashMap<>();
    private ConfigInfo configInfo;

    private boolean isCheck;
    private CommonConfig commonConf;

    private List<ServiceType> noExistsInStatusService = new ArrayList<>();
    private List<StatusInfo> noExistsServiceStatus = new ArrayList<>();;
    private List<StatusInfo> InConsistentVersionStatus = new ArrayList<>();
    private List<StatusInfo> noExistBackupStatus = new ArrayList<>();

    public static ScmUpgradeStatusInfoMgr getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ScmUpgradeStatusInfoMgr.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ScmUpgradeStatusInfoMgr();
            return instance;
        }
    }

    private ScmUpgradeStatusInfoMgr() {
        this.sshMgr = SshMgr.getInstance();
        this.commonConf = CommonConfig.getInstance();
        this.hosts = new ArrayList<>();
        try {
            init();
        }
        catch (Exception e) {
            throw new IllegalArgumentException("upgrade status file error:" + e.getMessage(), e);
        }
    }

    private synchronized void init() throws Exception {
        logger.info("Parsing the upgrade status file...");
        ScmConfParser parser = new ScmConfParser(commonConf.getUpgradeStatusFilePath());
        initInstallConfig(parser);
        initHostInfo(parser);
        initConfigInfo(parser);
        initStatusInfo(parser);
        logger.info("Parse the upgrade status file success");
    }

    private void initInstallConfig(ScmConfParser parser) {
        List<BasicInstallConfig> installConfigs = parser.getSeactionWithCheck(
                ConfFileDefine.SEACTION_INSTALLCONFIG, BasicInstallConfig.CONVERTER);
        CommonUtils.assertTrue(installConfigs.size() == 1,
                "only need one installconfig:" + installConfigs.toString());
        this.installConfig = installConfigs.get(0);
    }

    private void initHostInfo(ScmConfParser parser) {
        List<HostInfo> hosts = parser.getSeactionWithCheck(ConfFileDefine.SEACTION_HOST,
                HostInfo.CONVERTER);
        for (HostInfo host : hosts) {
            hostInfoMap.put(host.getHostName(), host);
        }
    }

    private void initConfigInfo(ScmConfParser parser) {
        this.configInfo = parser.getKeyValueSeaction(ConfFileDefine.SEACTION_CONFIG,
                ConfigInfo.CONVERTER);
    }

    private void initStatusInfo(ScmConfParser parser) {
        availableStatusInfos = parser.getSeactionWithCheck(ConfFileDefine.SEACTION_STATUS,
                StatusInfo.CONVERTER);

        // 1. 判断命令行是否指定hostname，存在则根据hostName过滤upgrade_status记录
        filterStatusWithHost();

        // 2. 判断命令行是否指定serviceName，存在则根据serviceName过滤upgrade_status记录
        filterStatusWithService();
    }

    private void filterStatusWithHost() {
        String commandLineHostNames = commonConf.getSubOptionValue(SubOption.HOST);
        if (commandLineHostNames != null) {
            Map<String, List<StatusInfo>> hostToStatus = new HashMap<>();
            for (StatusInfo statusInfo : availableStatusInfos) {
                List<StatusInfo> statusInfoList = hostToStatus.get(statusInfo.getHostName());
                if (statusInfoList == null) {
                    statusInfoList = new ArrayList<>();
                    hostToStatus.put(statusInfo.getHostName(), statusInfoList);
                }
                statusInfoList.add(statusInfo);
            }

            availableStatusInfos.clear();
            for (String hostName : commandLineHostNames.split(",")) {
                List<StatusInfo> statusInfoList = hostToStatus.get(hostName);
                if (statusInfoList == null) {
                    throw new IllegalArgumentException("no such host info:" + hostName);
                }
                availableStatusInfos.addAll(statusInfoList);
            }
        }
    }

    private void filterStatusWithService() {
        String commandLineServiceNames = commonConf.getSubOptionValue(SubOption.SERVICE);
        if (commandLineServiceNames != null) {
            Map<ServiceType, List<StatusInfo>> serviceToStatus = new HashMap<>();
            for (StatusInfo statusInfo : availableStatusInfos) {
                List<StatusInfo> statusInfoList = serviceToStatus.get(statusInfo.getType());
                if (statusInfoList == null) {
                    statusInfoList = new ArrayList<>();
                    serviceToStatus.put(statusInfo.getType(), statusInfoList);
                }
                statusInfoList.add(statusInfo);
            }
            availableStatusInfos.clear();
            for (String type : commandLineServiceNames.split(",")) {
                ServiceType serviceType = ServiceType.getTypeWithCheck(type);
                List<StatusInfo> statusInfoList = serviceToStatus.get(serviceType);
                if (statusInfoList == null) {
                    noExistsInStatusService.add(serviceType);
                }
                else {
                    availableStatusInfos.addAll(statusInfoList);
                }
            }
        }
    }

    public void check() {
        if (isCheck) {
            return;
        }
        logger.info("Checking the upgrade status file... (0/2)");
        logger.info("Checking the upgrade status file: host(1/2)");
        checkHostIsReachable();
        checkInstallPath();
        checkJavaHome();
        logger.info("Checking the upgrade status file: status (2/2)");
        checkStatus();
        logger.info("Check the configuration success");
        isCheck = true;
    }

    private void checkHostIsReachable() {
        for (HostInfo host : hosts) {
            Ssh ssh = null;
            try {
                ssh = sshMgr.getSsh(host);
            }
            catch (Exception e) {
                throw new IllegalArgumentException("host is unreachable:" + host.getHostName(), e);
            }

            try {
                try {
                    ssh.sudoExec("echo user_is_sudoer > /dev/null");
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("this user may not be sudoer:username="
                            + host.getUserName() + ", host=" + host.getHostName(), e);
                }
                if (!ssh.isSftpAvailable()) {
                    throw new IllegalArgumentException(
                            "sftp service may not be available, host=" + host.getHostName());
                }
            }
            finally {
                CommonUtils.closeResource(ssh);
            }
        }
    }

    private void checkInstallPath() {
        String installPath = installConfig.getInstallPath();
        for (HostInfo host : hosts) {
            Ssh ssh = null;
            try {
                ssh = sshMgr.getSsh(host);
                int isExist = ssh.sudoExec("ls " + installPath, 0, 2);
                if (isExist == 2) {
                    throw new IllegalArgumentException(
                            "the scm is not installed on " + host.getHostName());
                }
            }
            catch (Exception e) {
                throw new IllegalArgumentException("failed to check installPath, host="
                        + host.getHostName() + ",causeby=" + e.getMessage(), e);
            }
            finally {
                CommonUtils.closeResource(ssh);
            }
        }
    }

    private void checkJavaHome() {
        for (HostInfo host : hosts) {
            if (host.getJavaHome() == null || host.getJavaHome().trim().length() == 0) {
                Ssh ssh = null;
                try {
                    ssh = sshMgr.getSsh(host);
                    String javaHome = ssh.searchEnv("JAVA_HOME");
                    host.resetJavaHome(javaHome);
                }
                catch (IOException e) {
                    throw new IllegalArgumentException("failed to search JAVA_HOME, host="
                            + host.getHostName() + ", causeby=" + e.getMessage(), e);
                }
                finally {
                    CommonUtils.closeResource(ssh);
                }
            }
            checkJavaVersion(host);
        }

    }

    private void checkJavaVersion(HostInfo host) {
        Ssh ssh = null;
        try {
            ssh = sshMgr.getSsh(host);
            SshExecRes versionRes = ssh.exec(host.getJavaHome() + "/bin/java -version");
            String versionStr = versionRes.getStdErr().substring(
                    versionRes.getStdErr().indexOf("\"") + 1,
                    versionRes.getStdErr().lastIndexOf("\""));
            String majorAndMinorVersion = versionStr.substring(0, versionStr.lastIndexOf("."));
            JavaVersion javaVersion = new JavaVersion(majorAndMinorVersion);
            if (!javaVersion.isGte(commonConf.getRequireJavaVersion())) {
                throw new IllegalArgumentException(
                        "java version is not expected, host=" + host.getHostName()
                                + ", expectedJavaVersion=" + commonConf.getRequireJavaVersion()
                                + ", actual=" + versionRes.getStdErr());
            }
        }
        catch (Exception e) {
            throw new IllegalArgumentException("failed to check java version, host="
                    + host.getHostName() + ", causeby=" + e.getMessage(), e);
        }
        finally {
            CommonUtils.closeResource(ssh);
        }
    }

    private void checkStatus() {
        List<StatusInfo> finalStatusInfo = new ArrayList<>();
        for (StatusInfo statusInfo : availableStatusInfos) {
            HostInfo hostInfo = getHostInfoWithCheck(statusInfo.getHostName());
            Ssh ssh = null;
            try {
                ssh = sshMgr.getSsh(hostInfo);
                // 1. 获取该服务在该host上当前版本
                SshExecRes versionFileRes = ssh.sudoExecRes("ls " + installConfig.getInstallPath()
                        + "/" + statusInfo.getType().getJarFileSuffix(), 0, 2);
                if (versionFileRes.getExitCode() != 0) {
                    noExistsServiceStatus.add(statusInfo);
                    continue;
                }
                String versionFilePath = versionFileRes.getStdOut().replace("\n", "");
                String currentVersion = VersionFileType.getVersion(versionFilePath);

                // 2. 获取后判断版本是否跟statusInfo中新版本一致，不一致则记录到InConsistentVersionStatus
                if (!statusInfo.getNewVersion().equals(currentVersion)) {
                    statusInfo.setCurrentVersion(currentVersion);
                    if (!InConsistentVersionStatus.contains(statusInfo)) {
                        InConsistentVersionStatus.add(statusInfo);
                    }
                    continue;
                }

                // 3. 一致则判断备份目录是否存在，不存在则记录
                int isExists = ssh.sudoExec("ls " + statusInfo.getBackupPath(), 0, 2);
                if (isExists != 0) {
                    noExistBackupStatus.add(statusInfo);
                }
                else {
                    finalStatusInfo.add(statusInfo);
                }
            }
            catch (Exception e) {
                throw new RollbackException("failed to get " + statusInfo.getType() + " version on "
                        + hostInfo.getHostName(), e);
            }
            finally {
                CommonUtils.closeResource(ssh);
            }
        }
        availableStatusInfos = finalStatusInfo;
    }

    public HostInfo getHostInfoWithCheck(String hostName) {
        HostInfo h = hostInfoMap.get(hostName);
        if (h == null) {
            throw new IllegalArgumentException("no such host info:" + hostName);
        }
        return h;
    }

    public BasicInstallConfig getInstallConfig() {
        return installConfig;
    }

    public ConfigInfo getConfigInfo() {
        return configInfo;
    }

    public Map<ServiceType, List<StatusInfo>> getAvailableServiceToStatus() {
        if (availableServiceToStatus == null) {
            availableServiceToStatus = new HashMap<>();
            for (StatusInfo statusInfo : availableStatusInfos) {
                List<StatusInfo> statusInfoList = availableServiceToStatus.get(statusInfo.getType());
                if (statusInfoList == null) {
                    statusInfoList = new ArrayList<>();
                    availableServiceToStatus.put(statusInfo.getType(), statusInfoList);
                }
                statusInfoList.add(statusInfo);

            }
        }
        return availableServiceToStatus;
    }

    public List<ServiceType> getNoExistsInStatusService() {
        return noExistsInStatusService;
    }

    public List<StatusInfo> getNoExistsServiceStatus() {
        return noExistsServiceStatus;
    }

    public List<StatusInfo> getNoExistBackupStatus() {
        return noExistBackupStatus;
    }

    public List<StatusInfo> getInConsistentVersionStatus() {
        return InConsistentVersionStatus;
    }
}
