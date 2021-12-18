package com.sequoiacm.deploy.core;

import java.io.IOException;
import java.util.*;

import com.sequoiacm.deploy.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.common.SdbTools;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.module.*;
import com.sequoiacm.deploy.parser.ScmDeployConfParser;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshExecRes;
import com.sequoiacm.deploy.ssh.SshMgr;
import com.sequoiadb.base.Sequoiadb;

public class ScmDeployInfoMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmDeployInfoMgr.class);

    private SshMgr sshMgr;

    private Map<ServiceType, List<NodeInfo>> serviceToNodes = new HashMap<>();

    private Map<HostInfo, List<InstallPackType>> hostToInstallPack = new HashMap<>();

    private Map<String, List<String>> zonesToServiceCenterUrl = new HashMap<>();

    private MetaSourceInfo metasourceInfo;

    private MetaSourceInfo auditsourceInfo;

    private String zkUrls;

    private InstallConfig installConfig;

    private BSONObject hystrixConfig = new BasicBSONObject();

    private SiteInfo rootSite;
    private List<SiteInfo> siteInfos;
    private Map<String, SiteInfo> siteNameToSiteInfos = new HashMap<>();
    private Set<String> usedDatasourceSet = new HashSet<>();
    private Set<String> usedUrlSet = new HashSet<>();

    private Map<String, DataSourceInfo> datasouceMap = new HashMap<>();

    private Map<String, HostInfo> hostInfoMap = new HashMap<>();
    private List<HostInfo> hosts;

    private List<String> zoneNames;

    private CommonConfig commonConf;

    private boolean isCheck;

    private boolean isEnableDaemon;

    private SiteStrategyInfo siteStrategy;

    private static volatile ScmDeployInfoMgr instance;

    public static ScmDeployInfoMgr getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ScmDeployInfoMgr.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ScmDeployInfoMgr();
            return instance;
        }
    }

    private ScmDeployInfoMgr() {
        this.sshMgr = SshMgr.getInstance();
        this.commonConf = CommonConfig.getInstance();
        try {
            init();
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Configuration error:" + e.getMessage(), e);
        }
    }

    private synchronized void init() throws Exception {
        logger.info("Parsing the configuration...");
        ScmDeployConfParser parser = new ScmDeployConfParser(commonConf.getDeployConfigFilePath());
        initHostInfo(parser);
        initDatasouceInfo(parser);
        initMetasourceInfo(parser);
        initAuditsourceInfo(parser);
        initSiteInfo(parser);
        initZones(parser);
        initDaemonInfo(parser);
        initNodeInfo(parser);
        initInstallConfig(parser);
        initHystrixConfig();
        logger.info("Parse the configuration success");
    }

    private void initHystrixConfig() throws Exception {
        BSONObject bsonObject = CommonUtils
                .parseJsonFile(commonConf.getHystrixConfigPath());
        BSONObject rootSiteHystrixConf = BsonUtils.getBSONObjectChecked(bsonObject, "rootSite");
        SiteInfo rootSite = getRootSite();
        for (String configKey : rootSiteHystrixConf.keySet()) {
            String configValue = (String) rootSiteHystrixConf.get(configKey);
            String realConfigKey = configKey.replace("$serverName",
                    rootSite.getName().toLowerCase());
            this.hystrixConfig.put(realConfigKey, configValue);
        }
    }

    public void check() {
        if (isCheck) {
            return;
        }
        logger.info("Checking the configuration... (0/4)");

        logger.info("Checking the configuration: Datasource (1/4)");
        checkDatasources();

        logger.info("Checking the configuration: Metasource (2/4)");
        checkMetasource();

        logger.info("Checking the configuration: Auditsource (3/4)");
        checkAuditsource();

        logger.info("Checking the configuration: Host (4/4)");
        checkHostIsReachable();
        checkJavaHome();
        checkCrontabIsUsable();

        logger.info("Check the configuration success");
        isCheck = true;
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

    private void checkCrontabIsUsable() {
        if (!isEnableDaemon) {
            return;
        }
        for (HostInfo host : hosts) {
            Ssh ssh = null;
            try {
                ssh = sshMgr.getSsh(host);
                boolean isCrontabRunning = false;
                for (String command : CommonUtils.crontabCommands) {
                    try {
                        ssh.exec(command);
                        isCrontabRunning = true;
                        break;
                    }
                    catch (Exception e) {
                        // 如果执行的命令报错，就直接跳过，报错的原因：1.该命令不属于检测该系统crontab服务状态的命令；2.该系统没有开启crontab服务。
                        logger.debug("failed to exec command to check crontab status, command={}", command, e);
                    }
                }
                if (!isCrontabRunning) {
                    logger.warn(
                            "failed to check linux crontab status, please ensure the service start "
                                    + "or daemon process won't start automatically when it is suspended, host={}",
                            host.getHostName());
                }
            }
            catch (Exception e) {
                throw new IllegalArgumentException("failed to check linux crontab status, host="
                        + host.getHostName() + ", causeby=" + e.getMessage(), e);
            }
            finally {
                CommonUtils.closeResource(ssh);
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

    private void initZones(ScmDeployConfParser parser) {
        List<ZoneInfo> zones = parser.getSeactionWithCheck(ConfFileDefine.SEACTION_ZONE,
                ZoneInfo.CONVERTER);
        zoneNames = new ArrayList<>();
        for (ZoneInfo zone : zones) {
            zoneNames.add(zone.getName());
        }
    }

    private void initNodeInfo(ScmDeployConfParser parser) {
        Map<String, List<Integer>> portsOnHost = new HashMap<>();

        List<NodeInfo> serviceNodes = parser
                .getSeactionWithCheck(ConfFileDefine.SEACTION_SERVICE_NODE, NodeInfo.CONVERTER);
        checkServiceNode(serviceNodes, portsOnHost);

        List<NodeInfo> sitenodes = parser.getSeactionWithCheck(ConfFileDefine.SEACTION_SITE_NODE,
                SiteNodeInfo.CONVERTER);
        checkSiteNode(sitenodes, portsOnHost);

        List<NodeInfo> zknodes = parser.getSeactionWithCheck(ConfFileDefine.SEACTION_ZOOKEEPER,
                ZkNodeInfo.CONVERTER);
        checkZkNode(zknodes, portsOnHost);
        this.zkUrls = concatZkUrl(zknodes);

        serviceNodes.addAll(sitenodes);
        serviceNodes.addAll(zknodes);
        for (NodeInfo node : serviceNodes) {
            ServiceType serviceType = node.getServiceType();

            List<NodeInfo> list = serviceToNodes.get(serviceType);
            if (list == null) {
                list = new ArrayList<>();
                serviceToNodes.put(serviceType, list);
            }
            list.add(node);

            HostInfo host = hostInfoMap.get(node.getHostName());
            if (host == null) {
                throw new IllegalArgumentException("invalid node info, unregnized hostName:"
                        + node.getHostName() + ", node=" + node);
            }
            List<InstallPackType> services = hostToInstallPack.get(host);
            if (services == null) {
                services = new ArrayList<>();
                hostToInstallPack.put(host, services);
            }

            if (!services.contains(serviceType.getInstllPack())) {
                services.add(serviceType.getInstllPack());
            }

            if (node.getServiceType() == ServiceType.SERVICE_CENTER) {
                List<String> serviceCenterUrl = zonesToServiceCenterUrl.get(node.getZone());
                if (serviceCenterUrl == null) {
                    serviceCenterUrl = new ArrayList<>();
                    zonesToServiceCenterUrl.put(node.getZone(), serviceCenterUrl);
                }
                serviceCenterUrl
                        .add("http://" + node.getHostName() + ":" + node.getPort() + "/eureka/");
            }
        }

        if (isEnableDaemon) {
            List<NodeInfo> list = new ArrayList<>();
            for (Map.Entry<HostInfo, List<InstallPackType>> entry : hostToInstallPack.entrySet()) {
                entry.getValue().add(InstallPackType.DAEMON);
                HostInfo host = entry.getKey();
                list.add(new NodeInfo(host.getHostName(), ServiceType.DAEMON));
            }
            serviceToNodes.put(ServiceType.DAEMON, list);
        }

        for (ServiceType service : ServiceType.values()) {
            if (service.isRequire()) {
                List<NodeInfo> nodes = getNodesByServiceType(service);
                if (nodes == null || nodes.size() == 0) {
                    throw new IllegalArgumentException("missing required service node:" + service);
                }
            }
        }
    }

    private void checkServiceNode(List<NodeInfo> serviceNodes,
            Map<String, List<Integer>> portsOnHost) {
        for (NodeInfo node : serviceNodes) {
            if (!zoneNames.contains(node.getZone())) {
                throw new IllegalArgumentException("invalid service node, unregnized zoneName:"
                        + node.getZone() + ", node=" + node);
            }
            checkPortConflict(portsOnHost, node.getHostName(), node.getPort());
        }
    }

    private void checkZkNode(List<NodeInfo> zknodes, Map<String, List<Integer>> portsOnHost) {
        for (NodeInfo node : zknodes) {
            ZkNodeInfo zkNode = (ZkNodeInfo) node;
            checkPortConflict(portsOnHost, zkNode.getHostName(), zkNode.getPort());
            checkPortConflict(portsOnHost, zkNode.getHostName(), zkNode.getServerPort1());
            checkPortConflict(portsOnHost, zkNode.getHostName(), zkNode.getServerPort2());
        }
    }

    private void checkSiteNode(List<NodeInfo> sitenodes, Map<String, List<Integer>> portsOnHost) {
        List<String> allSiteName = new ArrayList<>(siteNameToSiteInfos.keySet());
        for (NodeInfo node : sitenodes) {
            SiteNodeInfo siteNode = (SiteNodeInfo) node;
            if (!siteNameToSiteInfos.containsKey(siteNode.getSiteName())) {
                throw new IllegalArgumentException("invalid sitenode, unregnized site name:"
                        + siteNode.getSiteName() + ", node=" + siteNode.toString());
            }
            allSiteName.remove(siteNode.getSiteName());

            if (!hostInfoMap.containsKey(siteNode.getHostName())) {
                throw new IllegalArgumentException(
                        "invalid sitenode, unregnized hostname:" + siteNode.toString());
            }
            if (!zoneNames.contains(node.getZone())) {
                throw new IllegalArgumentException("invalid service node, unregnized zoneName:"
                        + node.getZone() + ", node=" + node);
            }

            checkPortConflict(portsOnHost, siteNode.getHostName(), siteNode.getPort());
        }

        if (allSiteName.size() > 0) {
            throw new IllegalArgumentException("site requires at least a node:" + allSiteName);
        }
    }

    private void checkPortConflict(Map<String, List<Integer>> portsOnHost, String hostName,
            Integer port) {
        List<Integer> ports = portsOnHost.get(hostName);
        if (ports == null) {
            ports = new ArrayList<>();
            ports.add(port);
            portsOnHost.put(hostName, ports);
        }
        else {
            if (ports.contains(port)) {
                throw new IllegalArgumentException(
                        "port conflict:host=" + hostName + ", port=" + port);
            }
            ports.add(port);
        }
    }

    private void initAuditsourceInfo(ScmDeployConfParser parser) {
        List<AuditSourceInfo> auditSources = parser.getSeaction(ConfFileDefine.SEACTION_AUDITSOURCE,
                AuditSourceInfo.CONVERTER);
        if (auditSources == null || auditSources.size() == 0) {
            auditsourceInfo = metasourceInfo;
        }
        else {
            CommonUtils.assertTrue(auditSources.size() == 1,
                    "only need one auditsource:" + auditSources);
            auditsourceInfo = auditSources.get(0);
        }
    }

    private void checkAuditsource() {
        try {
            checkSdbSource(auditsourceInfo);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("failed to check auditsource", e);
        }
    }

    private void initDaemonInfo(ScmDeployConfParser parser) {
        List<DaemonInfo> daemonInfos = parser.getSeaction(ConfFileDefine.SEACTION_DAEMON,
                DaemonInfo.CONVERTER);
        if (daemonInfos == null || daemonInfos.size() == 0) {
            isEnableDaemon = true;
        }
        else {
            CommonUtils.assertTrue(daemonInfos.size() == 1,
                    "only need one enableDaemon option:" + daemonInfos);
            isEnableDaemon = daemonInfos.get(0).isEnableDaemon();
        }
    }

    private void initInstallConfig(ScmDeployConfParser parser) {
        List<InstallConfig> installConfigs = parser.getSeactionWithCheck(
                ConfFileDefine.SEACTION_INSTALLCONFIG, InstallConfig.CONVERTER);
        CommonUtils.assertTrue(installConfigs.size() == 1,
                "only need one installconfig:" + installConfigs.toString());
        this.installConfig = installConfigs.get(0);
    }

    private void initMetasourceInfo(ScmDeployConfParser parser) {
        List<MetaSourceInfo> metasources = parser
                .getSeactionWithCheck(ConfFileDefine.SEACTION_METASOURCE, MetaSourceInfo.CONVERTER);
        CommonUtils.assertTrue(metasources.size() == 1, "only need one metasource:" + metasources);
        metasourceInfo = metasources.get(0);

    }

    private void checkMetasource() {
        try {
            checkSdbSource(metasourceInfo);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("failed to check metasource", e);
        }
    }

    private void checkSdbSource(MetaSourceInfo source) {
        Sequoiadb db = null;
        try {
            db = SdbTools.createSdb(source.getUrl(), source.getUser(), source.getPassword());
            db.getDomain(source.getDomain());
        }
        finally {
            CommonUtils.closeResource(db);
        }
    }

    private void initDatasouceInfo(ScmDeployConfParser parser) {
        List<DataSourceInfo> datasources = parser
                .getSeactionWithCheck(ConfFileDefine.SEACTION_DATASOURCE, DataSourceInfo.CONVERTER);
        for (DataSourceInfo datasource : datasources) {
            DataSourceInfo sameNameDatasource = datasouceMap.get(datasource.getName());
            if (sameNameDatasource != null) {
                if (sameNameDatasource.getType() != DatasourceType.CEPH_S3
                        && datasource.getType() != DatasourceType.CEPH_S3) {
                    throw new IllegalArgumentException(
                            "conflict datasource: name=" + datasource.getName());
                }
                datasource.resetName(datasource.getName() + "-standby");
                sameNameDatasource.setStandbyDatasource(datasource);
                continue;
            }
            String[] urls = datasource.getUrl().split(",");
            for (String url : urls) {
                if (usedUrlSet.contains(url)) {
                    throw new IllegalArgumentException("url use twice: url=" + url);
                }
                usedUrlSet.add(url);
            }
            datasouceMap.put(datasource.getName(), datasource);
        }
    }

    private void checkDatasources() {
        for (DataSourceInfo datasource : datasouceMap.values()) {
            if (datasource.getType() == DatasourceType.SEQUOIADB) {
                Sequoiadb db = null;
                try {
                    db = SdbTools.createSdb(datasource.getUrl(), datasource.getUser(),
                            datasource.getPassword());
                }
                catch (Exception e) {
                    throw new IllegalArgumentException(
                            "failed to check datasource:" + datasource.getName(), e);
                }
                finally {
                    CommonUtils.closeResource(db);
                }
            }
        }
    }

    private void initSiteInfo(ScmDeployConfParser parser) {
        this.siteInfos = parser.getSeactionWithCheck(ConfFileDefine.SEACTION_SITES,
                SiteInfo.CONVERTER);
        for (SiteInfo siteInfo : siteInfos) {
            String siteName = siteInfo.getName();
            String datasourceName = siteInfo.getDatasourceName();
            if (usedDatasourceSet.contains(datasourceName)) {
                throw new IllegalArgumentException("one datasource can't use in two sites:"
                        + siteInfo.getName() + ", " + siteInfo.getDatasourceName());
            }
            usedDatasourceSet.add(datasourceName);
            siteNameToSiteInfos.put(siteName, siteInfo);
            if (siteInfo.isRoot()) {
                if (rootSite != null) {
                    throw new IllegalArgumentException("unsupport multiple root site:"
                            + siteInfo.getName() + ", " + rootSite.getName());
                }
                rootSite = siteInfo;
            }

            DataSourceInfo ds = datasouceMap.get(siteInfo.getDatasourceName());
            if (ds == null) {
                throw new IllegalArgumentException("invalid site info, unregnized datasouceName:"
                        + siteInfo.getDatasourceName() + ", site=" + siteInfo);
            }

        }

        if (rootSite == null) {
            throw new IllegalArgumentException("root site not exist");
        }

        List<SiteStrategyInfo> siteStrategyList = parser.getSeactionWithCheck(
                ConfFileDefine.SEACTION_SITE_STRATEGY, SiteStrategyInfo.CONVERTER);
        CommonUtils.assertTrue(siteStrategyList.size() == 1,
                "only support one site strategy:" + siteStrategyList);
        siteStrategy = siteStrategyList.get(0);
    }

    private void initHostInfo(ScmDeployConfParser parser) {
        this.hosts = parser.getSeactionWithCheck(ConfFileDefine.SEACTION_HOST, HostInfo.CONVERTER);
        for (HostInfo host : hosts) {
            hostInfoMap.put(host.getHostName(), host);
        }
    }

    private String concatZkUrl(List<NodeInfo> zknodes) {
        List<String> urls = new ArrayList<>();
        for (NodeInfo zkNode : zknodes) {
            urls.add(zkNode.getHostName() + ":" + zkNode.getPort());
        }
        return CommonUtils.toString(urls, ",");
    }

    public String getZkUrls() {
        return zkUrls;
    }

    public List<HostInfo> getHosts() {
        return hosts;
    }

    public Map<HostInfo, List<InstallPackType>> getServiceOnHost() {
        return hostToInstallPack;
    }

    public List<NodeInfo> getNodesByServiceType(ServiceType type) {
        return serviceToNodes.get(type);
    }

    public List<SiteInfo> getSites() {
        return siteInfos;
    }

    public SiteInfo getRootSite() {
        return rootSite;
    }

    public List<String> getZones() {
        return zoneNames;
    }

    public MetaSourceInfo getMetasourceInfo() {
        return this.metasourceInfo;
    }

    public MetaSourceInfo getAuditsourceInfo() {
        return this.auditsourceInfo;
    }

    public SiteStrategyInfo getSiteStrategy() {
        return siteStrategy;
    }

    public List<String> getServiceCenterUrlByZone(String zone) {
        List<String> url = zonesToServiceCenterUrl.get(zone);
        if (url == null) {
            return new ArrayList<>();
        }
        return url;
    }

    public SiteInfo getSiteInfo(String siteName) {
        SiteInfo s = siteNameToSiteInfos.get(siteName);
        if (s == null) {
            throw new IllegalArgumentException("site info not exists:" + siteName);
        }
        return s;
    }

    public String getFirstGatewayUrl() {
        List<NodeInfo> nodes = getNodesByServiceType(ServiceType.GATEWAY);
        if (nodes == null) {
            throw new IllegalArgumentException("gateway service not found");
        }
        return nodes.get(0).getHostName() + ":" + nodes.get(0).getPort();
    }

    public InstallConfig getInstallConfig() {
        return installConfig;
    }

    public DataSourceInfo getDatasouceInfo(String datasourceName) {
        DataSourceInfo ds = datasouceMap.get(datasourceName);
        if (ds == null) {
            throw new IllegalArgumentException("datasouce not exist:" + datasourceName);
        }
        return ds;
    }

    public HostInfo getHostInfoWithCheck(String hostName) {
        HostInfo h = hostInfoMap.get(hostName);
        if (h == null) {
            throw new IllegalArgumentException("no such host info:" + hostName);
        }
        return h;
    }

    public BSONObject getHystrixConfig() {
        return hystrixConfig;
    }
}
