package com.sequoiacm.deploy.deployer;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;

@Deployer
public class S3ServerDeployer extends ServiceDeployerBase {

    public S3ServerDeployer() {
        super(ServiceType.S3_SERVER);
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node) throws Exception {
        BasicBSONList templateBSONArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.S3_SERVER);
        BSONObject templateServerBson = (BSONObject) templateBSONArray.get(0);

        BasicBSONList decoratedArrayBson = new BasicBSONList();
        BasicBSONObject decoratedBSON = new BasicBSONObject();
        decoratedBSON.putAll(templateServerBson);
        decoratedBSON.putAll(super.genBaseDeployJson(node));

        decoratedArrayBson.add(decoratedBSON);
        return new BasicBSONObject().append(DeployJsonDefine.S3_SERVER, decoratedArrayBson);
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        return serviceInstallPath + "/bin/s3ctl.sh start --timeout " + getWaitServiceReadyTimeout()
                + "  -p " + node.getPort();
    }
}
