package com.sequoiacm.deploy.deployer;

import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;
@Deployer
public class CloudDiskDeployer extends ServiceDeployerBase {

    public CloudDiskDeployer() {
        super(ServiceType.CLOUD_DISK_SERVER);
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node) {
        BasicBSONList templateBSONArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.CLOUD_DISK);
        BSONObject templateServerBson = (BSONObject) templateBSONArray.get(0);

        BasicBSONList decoratedArrayBson = new BasicBSONList();
        BasicBSONObject decoratedBSON = new BasicBSONObject();
        decoratedBSON.putAll(templateServerBson);
        decoratedBSON.putAll(super.genBaseDeployJson(node));

        List<NodeInfo> virtualCd = super.getDeployInfoMgr()
                .getNodesByServiceType(ServiceType.VIRTUAL_CLOUD_DISK);
        if (virtualCd != null && virtualCd.size() > 0) {
            decoratedBSON.put("cloud-disk.test.model", "true");
            decoratedBSON.put("cloud-disk.test.url", "http://" + virtualCd.get(0).getHostName()
                    + ":" + virtualCd.get(0).getPort() + "/disk");
        }
        decoratedArrayBson.add(decoratedBSON);
        return new BasicBSONObject().append(DeployJsonDefine.CLOUD_DISK, decoratedArrayBson);
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        return serviceInstallPath + "/bin/cdiskctl.sh start -p " + node.getPort();
    }
}
