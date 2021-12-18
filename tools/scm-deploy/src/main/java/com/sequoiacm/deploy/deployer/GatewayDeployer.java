package com.sequoiacm.deploy.deployer;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;

@Deployer
public class GatewayDeployer extends ServiceDeployerBase {

    public GatewayDeployer() {
        super(ServiceType.GATEWAY);
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node) throws Exception {
        BasicBSONList templateBSONArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.GATEWAY);
        BSONObject templateServerBson = (BSONObject) templateBSONArray.get(0);

        BasicBSONList decoratedArrayBson = new BasicBSONList();
        BasicBSONObject decoratedBSON = new BasicBSONObject();
        decoratedBSON.putAll(templateServerBson);
        decoratedBSON.putAll(super.genBaseDeployJson(node));

        decoratedArrayBson.add(decoratedBSON);
        return new BasicBSONObject().append(DeployJsonDefine.GATEWAY, decoratedArrayBson)
                .append(DeployJsonDefine.AUDIT, templateBson.get(DeployJsonDefine.AUDIT));
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        return serviceInstallPath + "/bin/scmcloudctl.sh start --timeout "
                + getWaitServiceReadyTimeout() + "  -p " + node.getPort();
    }
}
