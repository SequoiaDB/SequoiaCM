package com.sequoiacm.deploy.deployer;

import com.sequoiacm.deploy.common.CommonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.core.ScmPasswordFileSender;
import com.sequoiacm.deploy.module.DataSourceInfo;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.S3NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.module.SiteInfo;

@Deployer
public class S3ServerDeployer extends ServiceDeployerBase {
    private ScmPasswordFileSender pwdFileSender = ScmPasswordFileSender.getInstance();

    public S3ServerDeployer() {
        super(ServiceType.S3_SERVER);
    }

    @Override
    protected void beforeDeploy(NodeInfo node) throws Exception {
        super.beforeDeploy(node);
        HostInfo hostInfo = super.getDeployInfoMgr().getHostInfoWithCheck(node.getHostName());
        pwdFileSender.sendMetasourcePasswdFile(hostInfo, getDeployInfoMgr().getMetasourceInfo());
        pwdFileSender.sendAuditSourcePasswdFile(hostInfo, getDeployInfoMgr().getAuditsourceInfo());
        String siteName = ((S3NodeInfo) node).getBindingSite();
        SiteInfo siteInfo = super.getDeployInfoMgr().getSiteInfo(siteName);
        DataSourceInfo dataSourceInfo = super.getDeployInfoMgr()
                .getDatasouceInfo(siteInfo.getDatasourceName());
        pwdFileSender.sendDsPasswdFile(hostInfo, dataSourceInfo);
        if (dataSourceInfo.getStandbyDatasource() != null) {
            pwdFileSender.sendDsPasswdFile(hostInfo, dataSourceInfo.getStandbyDatasource());
        }
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node)
            throws Exception {
        S3NodeInfo s3Node = (S3NodeInfo) node;
        BasicBSONList templateBSONArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.S3_SERVER);
        BSONObject templateServerBson = (BSONObject) templateBSONArray.get(0);

        BasicBSONList decoratedArrayBson = new BasicBSONList();
        BasicBSONObject decoratedBSON = new BasicBSONObject();
        decoratedBSON.putAll(templateServerBson);
        decoratedBSON.putAll(super.genBaseDeployJson(node));

        decoratedBSON.put(DeployJsonDefine.SPRING_APP_NAME, s3Node.getServiceName());
        decoratedBSON.put(DeployJsonDefine.CONTENT_MODULE_SITE, s3Node.getBindingSite());
        decoratedBSON.put(DeployJsonDefine.ROOT_SITE_META_URL,
                getDeployInfoMgr().getMetasourceInfo().getUrl());
        decoratedBSON.put(DeployJsonDefine.ROOT_SITE_META_USER,
                getDeployInfoMgr().getMetasourceInfo().getUser());
        decoratedBSON.put(DeployJsonDefine.ROOT_SITE_META_PWD,
                pwdFileSender.getMetasourcePasswdFilePath());
        decoratedBSON.put(DeployJsonDefine.ZOOKEEPER_URL, getDeployInfoMgr().getZkUrls());

        decoratedArrayBson.add(decoratedBSON);

        BSONObject auditBSON = BsonUtils.getBSONObject(templateBson, DeployJsonDefine.AUDIT);
        auditBSON.put(DeployJsonDefine.AUDIT_URL, getDeployInfoMgr().getAuditsourceInfo().getUrl());
        auditBSON.put(DeployJsonDefine.AUDIT_USER,
                getDeployInfoMgr().getAuditsourceInfo().getUser());
        auditBSON.put(DeployJsonDefine.AUDIT_PASSWORD,
                pwdFileSender.getAuditsourcePasswdFilePath());
        return new BasicBSONObject().append(DeployJsonDefine.S3_SERVER, decoratedArrayBson)
                .append(DeployJsonDefine.AUDIT, auditBSON);
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        return serviceInstallPath + "/bin/s3ctl.sh start --timeout " + CommonUtils.getWaitServiceReadyTimeout()
                + "  -p " + node.getPort();
    }
}
