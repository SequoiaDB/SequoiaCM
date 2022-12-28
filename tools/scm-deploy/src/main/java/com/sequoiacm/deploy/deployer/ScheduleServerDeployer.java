
package com.sequoiacm.deploy.deployer;

import com.sequoiacm.deploy.common.CommonUtils;
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
public class ScheduleServerDeployer extends ServiceDeployerBase {

    private ScmPasswordFileSender pwdFileSender = ScmPasswordFileSender.getInstance();

    public ScheduleServerDeployer() {
        super(ServiceType.SCHEDULE_SERVER);
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
        BasicBSONList templateBSONArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.SCHEDULE_SERVER);
        BSONObject templateServerBson = (BSONObject) templateBSONArray.get(0);

        BasicBSONList decoratedArray = new BasicBSONList();
        BasicBSONObject decoratedBSON = new BasicBSONObject();
        decoratedBSON.putAll(templateServerBson);
        decoratedBSON.putAll(super.genBaseDeployJson(node));

        decoratedBSON.put(DeployJsonDefine.ZOOKEEPER_URL, getDeployInfoMgr().getZkUrls());
        decoratedBSON.put(DeployJsonDefine.STORE_SDB_URL,
                getDeployInfoMgr().getMetasourceInfo().getUrl());
        decoratedBSON.put(DeployJsonDefine.STORE_SDB_USER,
                getDeployInfoMgr().getMetasourceInfo().getUser());
        decoratedBSON.put(DeployJsonDefine.STORE_SDB_PASSWORD,
                pwdFileSender.getMetasourcePasswdFilePath());
        decoratedArray.add(decoratedBSON);

        BSONObject auditBSON = BsonUtils.getBSONObject(templateBson, DeployJsonDefine.AUDIT);
        auditBSON.put(DeployJsonDefine.AUDIT_URL, getDeployInfoMgr().getAuditsourceInfo().getUrl());
        auditBSON.put(DeployJsonDefine.AUDIT_USER,
                getDeployInfoMgr().getAuditsourceInfo().getUser());
        auditBSON.put(DeployJsonDefine.AUDIT_PASSWORD,
                pwdFileSender.getAuditsourcePasswdFilePath());

        return new BasicBSONObject().append(DeployJsonDefine.SCHEDULE_SERVER, decoratedArray)
                .append(DeployJsonDefine.AUDIT, auditBSON);
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        return serviceInstallPath + "/bin/schctl.sh start --timeout " + CommonUtils.getWaitServiceReadyTimeout()
                + " -p " + node.getPort();
    }
}
