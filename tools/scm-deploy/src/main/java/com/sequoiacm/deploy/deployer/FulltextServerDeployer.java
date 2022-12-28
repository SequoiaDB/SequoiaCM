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
public class FulltextServerDeployer extends ServiceDeployerBase {

    private ScmPasswordFileSender pwdFileSender = ScmPasswordFileSender.getInstance();;

    public FulltextServerDeployer() {
        super(ServiceType.FULLTEXT_SERVER);
    }

    @Override
    protected void beforeDeploy(NodeInfo node) throws Exception {
        super.beforeDeploy(node);
        HostInfo hostInfo = super.getDeployInfoMgr().getHostInfoWithCheck(node.getHostName());
        pwdFileSender.sendMetasourcePasswdFile(hostInfo, getDeployInfoMgr().getMetasourceInfo());
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node) throws Exception {
        BasicBSONList templateBSONArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.FULLTEXT_SERVER);
        BSONObject templateServerBson = (BSONObject) templateBSONArray.get(0);

        BasicBSONList decoratedArray = new BasicBSONList();
        BasicBSONObject decoratedBSON = new BasicBSONObject();
        decoratedBSON.putAll(templateServerBson);
        decoratedBSON.putAll(super.genBaseDeployJson(node));

        decoratedBSON.put(DeployJsonDefine.ZOOKEEPER_URL, getDeployInfoMgr().getZkUrls());
        decoratedArray.add(decoratedBSON);
        return new BasicBSONObject().append(DeployJsonDefine.FULLTEXT_SERVER, decoratedArray);
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        return serviceInstallPath + "/bin/ftctl.sh start --timeout " + CommonUtils.getWaitServiceReadyTimeout()
                + " -p " + node.getPort();
    }
}
