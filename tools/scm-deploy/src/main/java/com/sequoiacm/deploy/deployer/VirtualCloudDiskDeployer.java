package com.sequoiacm.deploy.deployer;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.ssh.Ssh;

@Deployer
public class VirtualCloudDiskDeployer extends ServiceDeployerBase {

    public VirtualCloudDiskDeployer() {
        super(ServiceType.VIRTUAL_CLOUD_DISK, null, "deploy.json");
    }

    @Override
    protected void createNodeByRemoteJsonFile(Ssh ssh, String serviceRemoteInstallPath,
            String deployJsonFileRemotePath) throws IOException {
        HostInfo hostInfo = super.getDeployInfoMgr().getHostInfoWithCheck(ssh.getHost());
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("JAVA_HOME", hostInfo.getJavaHome());
        env.put("PATH", "$JAVA_HOME/bin:$PATH");
        String deploy = "python " + serviceRemoteInstallPath + "/deploy.py -c ";
        ssh.sudoSuExec(super.getDeployInfoMgr().getInstallConfig().getInstallUser(), deploy, env);
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node) {
        BasicBSONList cloudDiskArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.VIRTUAL_CLOUD_DISK);
        BSONObject cloudDisk = (BSONObject) cloudDiskArray.get(0);

        cloudDisk.put(DeployJsonDefine.SERVER_PORT, node.getPort() + "");
        cloudDisk.put("scm.cloud.disk.db.urls",
                super.getDeployInfoMgr().getMetasourceInfo().getUrl());
        cloudDisk.put("scm.cloud.disk.db.username",
                super.getDeployInfoMgr().getMetasourceInfo().getUser());
        String msPwd = super.getDeployInfoMgr().getMetasourceInfo().getPassword();
        CommonUtils.assertTrue(msPwd != null, "virtual clouddisk need metasource plain pwssword");
        cloudDisk.put("scm.cloud.disk.db.password", msPwd);

        // is not wrong for DeployJsonDefine.CLOUD_DISK!!!!
        return new BasicBSONObject().append(DeployJsonDefine.CLOUD_DISK, cloudDiskArray);
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        return serviceInstallPath + "/bin/start.sh";
    }
}
