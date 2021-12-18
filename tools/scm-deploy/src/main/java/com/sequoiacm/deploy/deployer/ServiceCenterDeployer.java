package com.sequoiacm.deploy.deployer;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;

@Deployer
public class ServiceCenterDeployer extends ServiceDeployerBase {

    public ServiceCenterDeployer() {
        super(ServiceType.SERVICE_CENTER);
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node) throws Exception {
        BasicBSONList templateBSONArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.SERVICE_CENTER);
        BSONObject templateServerBson = (BSONObject) templateBSONArray.get(0);

        BasicBSONList decoratedArrayBson = new BasicBSONList();
        BasicBSONObject decoratedBSON = new BasicBSONObject();
        decoratedBSON.putAll(templateServerBson);
        decoratedBSON.putAll(super.genBaseDeployJson(node, false));

        decoratedArrayBson.add(decoratedBSON);
        return new BasicBSONObject().append(DeployJsonDefine.SERVICE_CENTER, decoratedArrayBson)
                .append(DeployJsonDefine.AUDIT, templateBson.get(DeployJsonDefine.AUDIT));
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        return serviceInstallPath + "/bin/scmcloudctl.sh start --timeout "
                + getWaitServiceReadyTimeout() + "  -p " + node.getPort();
    }
}
