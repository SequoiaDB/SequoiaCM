package com.sequoiacm.deploy.deployer;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.core.ScmPasswordFileSender;
import com.sequoiacm.deploy.core.SiteBuilder;
import com.sequoiacm.deploy.module.DataSourceInfo;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.module.SiteInfo;
import com.sequoiacm.deploy.module.SiteNodeInfo;
import com.sequoiacm.deploy.ssh.Ssh;

@Deployer
public class ContentserverDeployer extends ServiceDeployerBase {
    // private static final Logger logger =
    // LoggerFactory.getLogger(ContentserverDeployer.class);
    private ScmPasswordFileSender pwdFileSender = ScmPasswordFileSender.getInstance();;

    private SiteBuilder siteBuilder = SiteBuilder.getInstance();

    public ContentserverDeployer() {
        super(ServiceType.CONTENT_SERVER,
                CommonConfig.getInstance().getContentServerBaseDeployFilePath());
    }

    @Override
    protected void beforeDeploy(NodeInfo node) throws Exception {
        super.beforeDeploy(node);
        HostInfo hostInfo = super.getDeployInfoMgr().getHostInfoWithCheck(node.getHostName());
        pwdFileSender.sendMetasourcePasswdFile(hostInfo, getDeployInfoMgr().getMetasourceInfo());
        pwdFileSender.sendAuditSourcePasswdFile(hostInfo, getDeployInfoMgr().getAuditsourceInfo());
        String siteName = ((SiteNodeInfo) node).getSiteName();
        SiteInfo siteInfo = super.getDeployInfoMgr().getSiteInfo(siteName);
        DataSourceInfo dataSourceInfo = super.getDeployInfoMgr()
                .getDatasouceInfo(siteInfo.getDatasourceName());
        pwdFileSender.sendDsPasswdFile(hostInfo, dataSourceInfo);

        siteBuilder.buidAllSite();
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node) {

        /**
         * { nodes:[ { node:[ {real node info here} ] } ] }
         */
        BasicBSONList nodesArray = BsonUtils.getArrayChecked(templateBson, "nodes");
        BSONObject nodesIdx0 = (BSONObject) nodesArray.get(0);
        BasicBSONList nodeArray = BsonUtils.getArrayChecked(nodesIdx0, "node");
        BSONObject nodeIdx0 = (BSONObject) nodeArray.get(0);
        nodeArray.clear();

        BasicBSONObject decoratedBSON = new BasicBSONObject();
        decoratedBSON.putAll(nodeIdx0);
        decoratedBSON.put("name", node.getHostName() + "_" + node.getPort());
        decoratedBSON.put("url", node.getHostName() + ":" + node.getPort());
        decoratedBSON.put("siteName", ((SiteNodeInfo) node).getSiteName());
        BSONObject propsTemplate = BsonUtils.getBSONObjectChecked(nodeIdx0, "customProperties");
        BSONObject props = new BasicBSONObject();
        props.putAll(propsTemplate);
        props.putAll(super.genBaseDeployJson(node));
        props.put(DeployJsonDefine.ZOOKEEPER_URL, super.getDeployInfoMgr().getZkUrls());
        decoratedBSON.put("customProperties", props);
        nodeArray.add(decoratedBSON);

        BasicBSONList sites = BsonUtils.getArrayChecked(templateBson, "sites");
        BSONObject site = (BSONObject) sites.get(0);
        BSONObject meta = BsonUtils.getBSONObjectChecked(site, "meta");
        meta.put("url", super.getDeployInfoMgr().getMetasourceInfo().getUrl());
        meta.put("user", super.getDeployInfoMgr().getMetasourceInfo().getUser());
        meta.put("password", pwdFileSender.getMetasourcePasswdFilePath());

        BSONObject auditBSON = BsonUtils.getBSONObjectChecked(templateBson, DeployJsonDefine.AUDIT);
        auditBSON.put(DeployJsonDefine.AUDIT_URL, getDeployInfoMgr().getAuditsourceInfo().getUrl());
        auditBSON.put(DeployJsonDefine.AUDIT_USER,
                getDeployInfoMgr().getAuditsourceInfo().getUser());
        auditBSON.put(DeployJsonDefine.AUDIT_PASSWORD,
                pwdFileSender.getAuditsourcePasswdFilePath());

        BSONObject gatewayBson = BsonUtils.getBSONObjectChecked(templateBson, "gateway");
        String gatewayUrl = super.getDeployInfoMgr().getFirstGatewayUrl();
        gatewayBson.put("url", gatewayUrl);
        gatewayBson.put("user", "admin");
        gatewayBson.put("password", "admin");

        return templateBson;
    }

    @Override
    protected void createNodeByRemoteJsonFile(Ssh ssh, String serviceRemoteInstallPath,
            String deployJsonFileRemotePath) throws IOException {
        HostInfo hostInfo = super.getDeployInfoMgr().getHostInfoWithCheck(ssh.getHost());
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("JAVA_HOME", hostInfo.getJavaHome());
        env.put("PATH", "$JAVA_HOME/bin:$PATH");
        String deploy = "python " + serviceRemoteInstallPath
                + "/deploy.py --createnode  -c " + deployJsonFileRemotePath;
        ssh.sudoSuExec(super.getDeployInfoMgr().getInstallConfig().getInstallUser(), deploy, env);
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        return serviceInstallPath + "/bin/scmctl.sh start --timeout " + getWaitServiceReadyTimeout()
                + "  -p " + node.getPort();
    }

}
