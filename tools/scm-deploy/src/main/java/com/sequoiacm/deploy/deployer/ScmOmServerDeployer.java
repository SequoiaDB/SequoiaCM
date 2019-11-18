package com.sequoiacm.deploy.deployer;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;
@Deployer
public class ScmOmServerDeployer extends ServiceDeployerBase {

    public ScmOmServerDeployer() {
        super(ServiceType.OM_SERVER);
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node) {
        BasicBSONList templateBSONArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.OM_SERVER);
        BSONObject templateServerBson = (BSONObject) templateBSONArray.get(0);

        BasicBSONList decoratedArrayBson = new BasicBSONList();
        BasicBSONObject decoratedBSON = new BasicBSONObject();
        decoratedBSON.putAll(templateServerBson);

        decoratedBSON.put(DeployJsonDefine.SERVER_PORT, node.getPort() + "");
        decoratedBSON.put(DeployJsonDefine.HOSTNAME, node.getHostName());

        List<NodeInfo> gateways = super.getDeployInfoMgr()
                .getNodesByServiceType(ServiceType.GATEWAY);
        if (gateways == null) {
            throw new IllegalArgumentException(
                    "failed to deploy om server, gateway service not found");
        }
        ArrayList<String> gatewayUrls = new ArrayList<>();
        for (NodeInfo gateway : gateways) {
            gatewayUrls.add(gateway.getHostName() + ":" + gateway.getPort());
        }
        decoratedBSON.put("scm.omserver.gateway", CommonUtils.toString(gatewayUrls, ","));

        return new BasicBSONObject().append(DeployJsonDefine.OM_SERVER, decoratedArrayBson);
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        return serviceInstallPath + "/bin/omctl.sh start -p " + node.getPort();
    }
}
