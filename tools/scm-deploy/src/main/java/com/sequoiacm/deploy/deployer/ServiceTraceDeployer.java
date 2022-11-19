package com.sequoiacm.deploy.deployer;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

@Deployer
public class ServiceTraceDeployer extends ServiceDeployerBase {

    public ServiceTraceDeployer() {
        super(ServiceType.SERVICE_TRACE);
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceRemoteInstallPath,
            String deployJsonFileRemotePath) {
        return serviceRemoteInstallPath + "/bin/scmcloudctl.sh start --timeout "
                + getWaitServiceReadyTimeout() + "  -p " + node.getPort();
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node)
            throws Exception {
        BasicBSONList templateBSONArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.SERVICE_TRACE);
        BSONObject templateServerBson = (BSONObject) templateBSONArray.get(0);

        BasicBSONList decoratedArrayBson = new BasicBSONList();
        BasicBSONObject decoratedBSON = new BasicBSONObject();
        decoratedBSON.putAll(templateServerBson);
        decoratedBSON.putAll(super.genBaseDeployJson(node));

        decoratedArrayBson.add(decoratedBSON);
        return new BasicBSONObject().append(DeployJsonDefine.SERVICE_TRACE, decoratedArrayBson)
                .append(DeployJsonDefine.AUDIT, templateBson.get(DeployJsonDefine.AUDIT));
    }
}
