package com.sequoiacm.deploy.deployer;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.core.ScmPasswordFileSender;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;

@Deployer
public class AdminServerDeployer extends ServiceDeployerBase {

    private ScmPasswordFileSender pwdFileSender = ScmPasswordFileSender.getInstance();

    public AdminServerDeployer() {
        super(ServiceType.ADMIN_SERVER);
    }

    @Override
    protected void beforeDeploy(NodeInfo node) throws Exception {
        super.beforeDeploy(node);
        HostInfo hostInfo = super.getDeployInfoMgr().getHostInfoWithCheck(node.getHostName());
        pwdFileSender.sendMetasourcePasswdFile(hostInfo, getDeployInfoMgr().getMetasourceInfo());
        pwdFileSender.sendAuditSourcePasswdFile(hostInfo, getDeployInfoMgr().getAuditsourceInfo());
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node) throws Exception {
        BasicBSONList templateAdminBSONArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.ADMIN_SERVER);
        BSONObject templateAdminServerBson = (BSONObject) templateAdminBSONArray.get(0);

        BasicBSONList decoratedAdminArray = new BasicBSONList();

        BasicBSONObject decoratedAdminBSON = new BasicBSONObject();
        decoratedAdminBSON.putAll(templateAdminServerBson);
        decoratedAdminBSON.putAll(super.genBaseDeployJson(node));

        decoratedAdminBSON.put(DeployJsonDefine.STORE_SDB_URL,
                getDeployInfoMgr().getMetasourceInfo().getUrl());
        decoratedAdminBSON.put(DeployJsonDefine.STORE_SDB_USER,
                getDeployInfoMgr().getMetasourceInfo().getUser());
        decoratedAdminBSON.put(DeployJsonDefine.STORE_SDB_PASSWORD,
                pwdFileSender.getMetasourcePasswdFilePath());
        decoratedAdminBSON.put(DeployJsonDefine.ZOOKEEPER_URL, getDeployInfoMgr().getZkUrls());
        decoratedAdminArray.add(decoratedAdminBSON);

        BSONObject auditBSON = BsonUtils.getBSONObject(templateBson, DeployJsonDefine.AUDIT);
        auditBSON.put(DeployJsonDefine.AUDIT_URL, getDeployInfoMgr().getAuditsourceInfo().getUrl());
        auditBSON.put(DeployJsonDefine.AUDIT_USER,
                getDeployInfoMgr().getAuditsourceInfo().getUser());
        auditBSON.put(DeployJsonDefine.AUDIT_PASSWORD,
                pwdFileSender.getAuditsourcePasswdFilePath());

        return new BasicBSONObject().append(DeployJsonDefine.ADMIN_SERVER, decoratedAdminArray)
                .append(DeployJsonDefine.AUDIT, auditBSON);
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        return serviceInstallPath + "/bin/scmcloudctl.sh start --timeout "
                + getWaitServiceReadyTimeout() + " -p " + node.getPort();
    }
}
