package com.sequoiacm.deploy.deployer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.module.SiteInfo;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.core.ScmDeployInfoMgr;
import com.sequoiacm.deploy.installer.ServiceInstallerMgr;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshMgr;

public abstract class ServiceDeployerBase implements ServiceDeployer {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDeployerBase.class);

    protected ScmDeployInfoMgr confMgr = ScmDeployInfoMgr.getInstance();

    protected CommonConfig commonConfig = CommonConfig.getInstance();

    protected ServiceInstallerMgr serviceInstallerMgr = ServiceInstallerMgr.getInstance();

    protected SshMgr sshFactory = SshMgr.getInstance();

    private ServiceType type;
    private String deployTemplateJsonFile;
    private String toRemoteDeployJsonFileName;

    public ServiceDeployerBase(ServiceType type) {
        this(type, null, null);
    }

    public ServiceDeployerBase(ServiceType type, String deployTemplateJsonFile) {
        this(type, deployTemplateJsonFile, null);
    }

    public ServiceDeployerBase(ServiceType type, String deployTemplateJsonFile,
            String toRemoteDeployJsonFileName) {
        this.type = type;
        this.deployTemplateJsonFile = deployTemplateJsonFile;
        if (this.deployTemplateJsonFile == null) {
            this.deployTemplateJsonFile = commonConfig.getBaseServiceTemplateFilePath();
        }
        this.toRemoteDeployJsonFileName = toRemoteDeployJsonFileName;
    }

    protected void beforeDeploy(NodeInfo node) throws Exception {
    }

    private File genDeployJsonFile(NodeInfo node) throws Exception {
        String jsonStr = CommonUtils.readContentFromLocalFile(deployTemplateJsonFile);
        BSONObject bson = (BSONObject) JSON.parse(jsonStr);
        bson = decorateTemplateDeployJson(bson, node);

        String fileName = getServiceType() + "_" + node.getHostName() + "_" + node.getPort()
                + "_deploy.json";
        FileOutputStream os = new FileOutputStream(
                commonConfig.getTempPath() + File.separator + fileName);
        try {
            String json = com.alibaba.fastjson.JSON.toJSONString(bson.toMap(), true);
            os.write(json.getBytes("utf-8"));
        }
        finally {
            CommonUtils.closeResource(os);
        }

        return new File(commonConfig.getTempPath() + File.separator + fileName);
    }

    @Override
    public void deploy(NodeInfo node) throws Exception {
        beforeDeploy(node);
        File deployJsonFile = genDeployJsonFile(node);
        String host = node.getHostName();
        HostInfo hostInfo = confMgr.getHostInfoWithCheck(host);
        Ssh ssh = sshFactory.getSsh(hostInfo);
        try {
            String serviceInstallPath = serviceInstallerMgr.getInstallPath(hostInfo,
                    getServiceType().getInstllPack());
            String installUser = confMgr.getInstallConfig().getInstallUser();
            String remoteJsonFilePath = serviceInstallPath;
            if (toRemoteDeployJsonFileName == null) {
                remoteJsonFilePath = remoteJsonFilePath + "/tmp";
                ssh.sudoSuExec(installUser, "mkdir -p " + remoteJsonFilePath, null);
                remoteJsonFilePath = remoteJsonFilePath + "/" + deployJsonFile.getName();
            }
            else {
                remoteJsonFilePath = remoteJsonFilePath + "/" + toRemoteDeployJsonFileName;
            }
            ssh.scp(deployJsonFile.getAbsolutePath(), remoteJsonFilePath);
            ssh.changeOwner(remoteJsonFilePath, installUser,
                    confMgr.getInstallConfig().getInstallUserGroup());

            try {
                createNodeByRemoteJsonFile(ssh, serviceInstallPath, remoteJsonFilePath);
            }
            catch (Exception e) {
                reThrowCreateNodeException(host, serviceInstallPath, node, e);
            }

            try {
                startNode(ssh, node, serviceInstallPath, remoteJsonFilePath);
            }
            catch (Exception e) {
                reThrowStartNodeException(host, serviceInstallPath, node, e);
            }

        }
        finally {
            ssh.close();
        }
    }

    protected void reThrowCreateNodeException(String host, String installPath, NodeInfo node,
            Exception e) throws Exception {
        throw new Exception("failed to crete node, please check remote log " + host + ":"
                + installPath + "/log/admin", e);
    }

    protected void reThrowStartNodeException(String host, String installPath, NodeInfo node,
            Exception e) throws Exception {
        throw new Exception("failed to start node, please check remote log " + host + ":("
                + installPath + "/log/start" + ", " + installPath + "/log/"
                + getServiceType().getType() + "/" + node.getPort() + ")", e);
    }

    protected void createNodeByRemoteJsonFile(Ssh ssh, String serviceRemoteInstallPath,
            String deployJsonFileRemotePath) throws IOException {
        HostInfo hostInfo = confMgr.getHostInfoWithCheck(ssh.getHost());
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("JAVA_HOME", hostInfo.getJavaHome());
        env.put("PATH", "$JAVA_HOME/bin:$PATH");
        String deploy = "python " + serviceRemoteInstallPath + "/deploy.py -c "
                + deployJsonFileRemotePath;
        ssh.sudoSuExec(confMgr.getInstallConfig().getInstallUser(), deploy, env);
    }

    public void startNode(Ssh ssh, NodeInfo node, String serviceRemoteInstallPath,
            String deployJsonFileRemotePath) throws Exception {
        HostInfo hostInfo = confMgr.getHostInfoWithCheck(ssh.getHost());
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("JAVA_HOME", hostInfo.getJavaHome());
        env.put("PATH", "$JAVA_HOME/bin:$PATH");
        ssh.sudoSuExec(confMgr.getInstallConfig().getInstallUser(),
                getStartCmd(node, serviceRemoteInstallPath, deployJsonFileRemotePath), env);
    }

    protected abstract String getStartCmd(NodeInfo node, String serviceRemoteInstallPath,
            String deployJsonFileRemotePath);

    protected abstract BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node)
            throws Exception;

    // service center info, hystrix, custom node props
    protected BSONObject genBaseDeployJson(NodeInfo node, boolean isNeedHystrixConf) throws Exception {
        List<String> zones = confMgr.getZones();
        // availableZone indicates that service-center is deployed in the zone
        List<String> availableZones = new ArrayList<>();
        for (String zone : zones) {
            List<String> zoneUrls = confMgr.getServiceCenterUrlByZone(zone);
            if (zoneUrls != null && zoneUrls.size() > 0) {
                if (zone.equals(node.getZone())) {
                    availableZones.add(0, zone);
                }
                else {
                    availableZones.add(zone);
                }
            }
        }
        BasicBSONObject basicBSON = new BasicBSONObject();
        basicBSON.put(DeployJsonDefine.AVAILABILITY_ZONES_PREFIX + commonConfig.getRegionName(),
                CommonUtils.toString(availableZones, ","));
        basicBSON.put(DeployJsonDefine.HOSTNAME, node.getHostName());
        basicBSON.put(DeployJsonDefine.REGION, commonConfig.getRegionName());
        basicBSON.put(DeployJsonDefine.SERVER_PORT, node.getPort() + "");
        basicBSON.put(DeployJsonDefine.ZONE, node.getZone());
        if (node.getManagementPort() != null) {
            basicBSON.put(DeployJsonDefine.MANAGEMENT_PORT, node.getManagementPort() + "");
        }
        for (String zone : zones) {
            List<String> url = confMgr.getServiceCenterUrlByZone(zone);
            basicBSON.put(DeployJsonDefine.ZONE_URL_PREFIX + zone, CommonUtils.toString(url, ","));
        }

        // generate hystrix config
        if (isNeedHystrixConf) {
            basicBSON.putAll(confMgr.getHystrixConfig());
        }

        BSONObject customConf = node.getCustomNodeConf();
        if (customConf != null) {
            basicBSON.putAll(customConf);
        }
        return basicBSON;
    }

    protected BSONObject genBaseDeployJson(NodeInfo node) throws Exception {
       return genBaseDeployJson(node, true);
    }

    @Override
    public ServiceType getServiceType() {
        return type;
    }

    protected ScmDeployInfoMgr getDeployInfoMgr() {
        return confMgr;
    }

    public CommonConfig getCommonConfig() {
        return commonConfig;
    }

    protected int getWaitServiceReadyTimeout() {
        int timeoutMs = commonConfig.getWaitServiceReadyTimeout();
        if (timeoutMs > 5000) {
            return timeoutMs / 1000;
        }

        return 120;
    }
}
