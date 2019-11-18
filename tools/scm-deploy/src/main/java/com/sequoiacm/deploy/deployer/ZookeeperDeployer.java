package com.sequoiacm.deploy.deployer;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.module.ZkNodeInfo;
import com.sequoiacm.deploy.ssh.Ssh;

@Deployer
public class ZookeeperDeployer extends ServiceDeployerBase {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperDeployer.class);

    public ZookeeperDeployer() {
        super(ServiceType.ZOOKEEPER);
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo n) {
        List<NodeInfo> zkNodes = super.getDeployInfoMgr().getNodesByServiceType(getServiceType());

        BasicBSONList templateBSONArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.ZK_SERVER);
        BSONObject templateServerBson = (BSONObject) templateBSONArray.get(0);

        int myId = 1;
        BasicBSONList decoratedArray = new BasicBSONList();
        for (NodeInfo node : zkNodes) {
            ZkNodeInfo zkNode = (ZkNodeInfo) node;
            BasicBSONObject decoratedBSON = new BasicBSONObject();
            decoratedBSON.putAll(templateServerBson);
            decoratedBSON.put("server", zkNode.getHostName() + ":" + zkNode.getServerPort1() + ":"
                    + zkNode.getServerPort2());
            decoratedBSON.put("clientPort", zkNode.getPort());
            decoratedBSON.put("myid", myId++);
            if (zkNode.equals(n)) {
                decoratedBSON.put("deploy", true);
            }
            else {
                decoratedBSON.put("deploy", false);
            }
            decoratedArray.add(decoratedBSON);
        }

        return new BasicBSONObject().append(DeployJsonDefine.ZK_SERVER, decoratedArray);
    }

    @Override
    protected void createNodeByRemoteJsonFile(Ssh ssh, String serviceRemoteInstallPath,
            String deployJsonFileRemotePath) throws IOException {
        HostInfo hostInfo = super.getDeployInfoMgr().getHostInfoWithCheck(ssh.getHost());
        String zkDeployScriptPath = super.getCommonConfig().getZkDeployScript();
        ssh.scp(zkDeployScriptPath, serviceRemoteInstallPath);
        String remoteZkDeployScriptPath = serviceRemoteInstallPath + "/"
                + new File(zkDeployScriptPath).getName();
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("JAVA_HOME", hostInfo.getJavaHome());
        env.put("PATH", "$JAVA_HOME/bin:$PATH");
        String deploy = "python " + remoteZkDeployScriptPath + " -d -c " + deployJsonFileRemotePath;
        ssh.sudoSuExec(super.getDeployInfoMgr().getInstallConfig().getInstallUser(), deploy, env);
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        String zkDeployScriptPath = super.getCommonConfig().getZkDeployScript();
        String remoteZkDeployScriptPath = serviceInstallPath + "/"
                + new File(zkDeployScriptPath).getName();
        return "python " + remoteZkDeployScriptPath + " -s -c " + deployJsonFileRemotePath;
    }

    @Override
    protected void reThrowCreateNodeException(String host, String installPath, NodeInfo node,
            Exception e) throws Exception {
        throw new Exception(
                "Failed to crete node, check " + CommonUtils.getLogFilePath() + " for details", e);
    }

    @Override
    protected void reThrowStartNodeException(String host, String installPath, NodeInfo node,
            Exception e) throws Exception {
        throw new Exception("Failed to start node, check remote log in " + host + ":" + installPath
                + "/zookeeper.out", e);
    }
}
