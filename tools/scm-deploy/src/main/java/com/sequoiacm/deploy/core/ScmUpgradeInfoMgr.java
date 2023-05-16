package com.sequoiacm.deploy.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.sequoiacm.deploy.common.SdbTools;
import com.sequoiacm.deploy.module.BasicInstallConfig;
import com.sequoiacm.deploy.module.VersionFileType;
import com.sequoiacm.deploy.parser.KeyValueConverter;
import com.sequoiacm.infrastructure.config.core.common.ScmGlobalConfigDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.command.SubOption;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.exception.UpgradeException;
import com.sequoiacm.deploy.installer.ServicesInstallPackManager;
import com.sequoiacm.deploy.module.ConfigInfo;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallPackType;
import com.sequoiacm.deploy.module.JavaVersion;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.parser.ScmConfParser;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshExecRes;
import com.sequoiacm.deploy.ssh.SshMgr;

public class ScmUpgradeInfoMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmUpgradeInfoMgr.class);
    private static volatile ScmUpgradeInfoMgr instance;
    private SshMgr sshMgr = SshMgr.getInstance();
    private ServicesInstallPackManager packManager = ServicesInstallPackManager.getInstance();
    private BasicInstallConfig installConfig;
    private List<HostInfo> hosts;
    private Map<String, HostInfo> hostInfoMap = new HashMap<>();
    private ConfigInfo configInfo;
    private Map<InstallPackType, String> installPackToNewVersion = new HashMap<>();

    private boolean isCheck;
    private CommonConfig commonConf = CommonConfig.getInstance();

    private List<ServiceType> allServices = new ArrayList<>();
    private List<ServiceType> noExistServices = new ArrayList<>();
    private Map<HostInfo, Map<ServiceType, String>> upgradeHostService = new HashMap<>();

    private Map<ServiceType, Map<HostInfo, String>> upgradeServiceHost = new HashMap<>();
    private Map<HostInfo, Map<ServiceType, String>> unableUpgradeHostService = new HashMap<>();
    private Map<ServiceType, Map<HostInfo, String>> unableUpgradeServiceHost = new HashMap<>();
    Map<String, String> globalConfig = new HashMap<>();

    public static ScmUpgradeInfoMgr getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ScmUpgradeInfoMgr.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ScmUpgradeInfoMgr();
            return instance;
        }
    }

    private ScmUpgradeInfoMgr() {
        try {
            init();
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Upgrade configuration error:" + e.getMessage(), e);
        }
    }

    private synchronized void init() throws Exception {
        logger.info("Parsing the upgrade configuration...");
        ScmConfParser parser = new ScmConfParser(commonConf.getUpgradeConfigFilePath());
        initInstallConfig(parser);
        initHostInfo(parser);
        initConfigInfo(parser);
        initInstallPackToNewVersion();
        initGlobalConfig(parser);
        logger.info("Parse the upgrade configuration success");
    }

    private void initGlobalConfig(ScmConfParser parser) {
        Map<String, String> globalConfig = parser.getKeyValueSeaction(
                ConfFileDefine.SECTION_CLUSTER_GLOBAL_CONFIG, keyValue -> keyValue);
        if (globalConfig != null) {
            this.globalConfig = globalConfig;
        }
    }

    public Map<String, String> getGlobalConfig() {
        return globalConfig;
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
        Map<String, HostInfo> allHostInfoMap = new HashMap<>();
        for (HostInfo host : hosts) {
            allHostInfoMap.put(host.getHostName(), host);
        }
        String commandLineHostNames = commonConf.getSubOptionValue(SubOption.HOST);
        if (commandLineHostNames == null) {
            this.hosts = hosts;
            this.hostInfoMap = allHostInfoMap;
        }
        else {
            List<HostInfo> commandLineHosts = new ArrayList<>();
            Map<String, HostInfo> commandLineHostInfoMap = new HashMap<>();
            for (String commandLineHostName : commandLineHostNames.trim().split(",")) {
                HostInfo host = allHostInfoMap.get(commandLineHostName);
                if (host == null) {
                    throw new IllegalArgumentException(
                            "CommandLine:The specified host on the command line dose not exist in upgrade Configuration,host="
                                    + commandLineHostName);
                }
                commandLineHosts.add(host);
                commandLineHostInfoMap.put(host.getHostName(), host);
            }
            this.hosts = commandLineHosts;
            this.hostInfoMap = commandLineHostInfoMap;
        }
    }

    private void initConfigInfo(ScmConfParser parser) {
        this.configInfo = parser.getKeyValueSeaction(ConfFileDefine.SEACTION_CONFIG,
                ConfigInfo.CONVERTER);
        String commandLineServicesStr = commonConf.getSubOptionValue(SubOption.SERVICE);
        if (commandLineServicesStr == null) {
            return;
        }
        List<ServiceType> commandLineServices = new ArrayList<>();
        for (String type : commandLineServicesStr.split(",")) {
            ServiceType serviceType = ServiceType.getTypeWithCheck(type.trim());
            commandLineServices.add(serviceType);
        }
        configInfo.setServices(commandLineServices);
    }

    private void initInstallPackToNewVersion() {
        for (final InstallPackType installPackType : InstallPackType.values()) {
            if (installPackType == InstallPackType.ZOOKEEPER) {
                continue;
            }
            String version = null;
            if (installPackType == InstallPackType.NON_SERVICE) {
                File file = new File(CommonConfig.getInstance().getBasePath()
                        + installPackType.getUntarDirName());
                File[] files = file.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return Pattern.matches(installPackType.getPackNameRegexp(), name);
                    }
                });
                if (files == null || files.length <= 0) {
                    throw new UpgradeException(
                            "new version file not found:file=" + file.getAbsolutePath());
                }
                version = VersionFileType.getVersion(files[0].getName());
            }
            else {
                String serviceInstallPackName = packManager.getServicePack(installPackType)
                        .getName();
                version = VersionFileType.getVersion(serviceInstallPackName);
            }
            installPackToNewVersion.put(installPackType, version);
        }
    }

    public void check() {
        if (isCheck) {
            return;
        }
        logger.info("Checking the upgrade configuration... (0/2)");
        logger.info("Checking the upgrade configuration: Host (1/2)");
        checkHostIsReachable();
        checkJavaHome();
        checkInstallPath();

        logger.info("Checking the upgrade configuration: Config (2/2)");
        checkServiceVersion();
        checkIsServiceScriptExist();
        checkGlobalConfig();
        logger.info("Check the upgrade configuration success");
        isCheck = true;
    }

    private void checkGlobalConfig() {
        if (!getGlobalConfig().isEmpty()) {
            if (configInfo.getScmUser() == null || configInfo.getScmPassword() == null
                    || configInfo.getScmGateway() == null) {
                throw new IllegalArgumentException(
                        "scmUser, scmPassword, scmGateway must be set in config");
            }
        }
    }

    private void checkServiceVersion() {
        // 找到主机上的所有服务，并拿到旧版本
        for (HostInfo host : hosts) {
            Ssh ssh = null;
            try {
                ssh = sshMgr.getSsh(host);
                SshExecRes installRes = ssh.sudoExecRes("ls " + installConfig.getInstallPath(), 0,
                        2);
                for (String dirName : installRes.getStdOut().split("\n")) {
                    InstallPackType installPackType = InstallPackType.getType(dirName);
                    if (installPackType == null || installPackType == InstallPackType.ZOOKEEPER) {
                        continue;
                    }
                    for (ServiceType service : ServiceType.getServiceTypes(installPackType)) {

                        String versionFileSuffix = service.getJarFileSuffix();
                        SshExecRes versionFileRes = ssh.sudoExecRes(
                                "ls " + installConfig.getInstallPath() + "/" + versionFileSuffix, 0,
                                2);
                        if (versionFileRes.getExitCode() != 0) {
                            logger.warn("fail to find service version:service=" + service
                                    + ", host=" + host.getHostName() + ", file="
                                    + installConfig.getInstallPath() + "/" + versionFileSuffix);
                            continue;
                        }

                        if (!allServices.contains(service)) {
                            allServices.add(service);
                        }
                        if (!configInfo.getServices().contains(service)) {
                            continue;
                        }

                        String versionFilePath = versionFileRes.getStdOut().replace("\n", "");
                        String newVersion = installPackToNewVersion.get(service.getInstllPack());
                        String oldVersion = VersionFileType.getVersion(versionFilePath);
                        if (VersionFileType.compareVersion(newVersion, oldVersion) <= 0) {
                            Map<ServiceType, String> unableServiceVersion = unableUpgradeHostService
                                    .get(host);
                            if (unableServiceVersion == null) {
                                unableServiceVersion = new HashMap<>();
                                unableUpgradeHostService.put(host, unableServiceVersion);
                            }
                            unableServiceVersion.put(service, oldVersion);
                        }
                        else {
                            Map<ServiceType, String> serviceVersion = upgradeHostService.get(host);
                            if (serviceVersion == null) {
                                serviceVersion = new HashMap<>();
                                upgradeHostService.put(host, serviceVersion);
                            }
                            serviceVersion.put(service, oldVersion);
                        }
                    }
                }
            }
            catch (Exception e) {
                throw new UpgradeException("failed to get all service on " + host.getHostName(), e);
            }
            finally {
                CommonUtils.closeResource(ssh);
            }
        }
        transformHostServiceToServiceHost(upgradeHostService, upgradeServiceHost);
        transformHostServiceToServiceHost(unableUpgradeHostService, unableUpgradeServiceHost);
        for (ServiceType service : configInfo.getServices()) {
            if (!allServices.contains(service)) {
                noExistServices.add(service);
            }
        }

    }

    private void transformHostServiceToServiceHost(
            Map<HostInfo, Map<ServiceType, String>> hostService,
            Map<ServiceType, Map<HostInfo, String>> serviceHost) {
        for (Map.Entry<HostInfo, Map<ServiceType, String>> hostServiceEntry : hostService
                .entrySet()) {
            HostInfo host = hostServiceEntry.getKey();
            Map<ServiceType, String> serviceVersion = hostServiceEntry.getValue();
            for (Map.Entry<ServiceType, String> serviceVersionEntry : serviceVersion.entrySet()) {
                ServiceType service = serviceVersionEntry.getKey();
                String version = serviceVersionEntry.getValue();
                Map<HostInfo, String> hostVersion = serviceHost.get(service);
                if (hostVersion == null) {
                    hostVersion = new HashMap<>();
                    serviceHost.put(service, hostVersion);
                }
                hostVersion.put(host, version);
            }
        }
    }

    private void checkIsServiceScriptExist() {
        for (Map.Entry<ServiceType, Map<HostInfo, String>> entry : unableUpgradeServiceHost
                .entrySet()) {
            ServiceType serviceType = entry.getKey();
            if (serviceType.getStartStopScript() == null) {
                continue;
            }
            Set<HostInfo> hostInfoSet = entry.getValue().keySet();
            for (HostInfo host : hostInfoSet) {
                Ssh ssh = null;
                try {
                    ssh = sshMgr.getSsh(host);
                    String startStopScriptPath = installConfig.getInstallPath() + "/"
                            + serviceType.getInstllPack().getUntarDirName() + "/bin/"
                            + serviceType.getStartStopScript();
                    int isExists = ssh.sudoExec("ls " + startStopScriptPath);
                    if (isExists != 0) {
                        throw new UpgradeException(
                                startStopScriptPath + " does not exist on " + host.getHostName());
                    }
                }
                catch (Exception e) {
                    throw new UpgradeException(
                            "failed to check startStopScript on " + host.getHostName(), e);
                }
                finally {
                    CommonUtils.closeResource(ssh);
                }

            }
        }
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
                int isExists = ssh.sudoExec("ls " + installPath, 0, 2);
                if (isExists == 2) {
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

    public BasicInstallConfig getInstallConfig() {
        return installConfig;
    }

    public List<HostInfo> getHosts() {
        return hosts;
    }

    public Map<String, HostInfo> getHostInfoMap() {
        return hostInfoMap;
    }

    public HostInfo getHostInfoWithCheck(String hostName) {
        HostInfo h = hostInfoMap.get(hostName);
        if (h == null) {
            throw new IllegalArgumentException("no such host info:" + hostName);
        }
        return h;
    }

    public ConfigInfo getConfigInfo() {
        return configInfo;
    }

    public Map<InstallPackType, String> getInstallPackToNewVersion() {
        return installPackToNewVersion;
    }

    public List<ServiceType> getNoExistServices() {
        return noExistServices;
    }

    public Map<HostInfo, Map<ServiceType, String>> getUpgradeHostService() {
        return upgradeHostService;
    }

    public Map<ServiceType, Map<HostInfo, String>> getUpgradeServiceHost() {
        return upgradeServiceHost;
    }

    public Map<HostInfo, Map<ServiceType, String>> getUnableUpgradeHostService() {
        return unableUpgradeHostService;
    }

    public Map<ServiceType, Map<HostInfo, String>> getUnableUpgradeServiceHost() {
        return unableUpgradeServiceHost;
    }
}
