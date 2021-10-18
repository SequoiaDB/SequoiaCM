package com.sequoiacm.deploy.deployer;

import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.ssh.Ssh;
import org.bson.BSONObject;

@Deployer
public class DaemonDeployer extends ServiceDeployerBase {

    public DaemonDeployer() {
        super(ServiceType.DAEMON);
    }

    @Override
    public void deploy(NodeInfo node) throws Exception {
        String host = node.getHostName();
        HostInfo hostInfo = confMgr.getHostInfoWithCheck(host);
        Ssh ssh = sshFactory.getSsh(hostInfo);
        try {
            String serviceInstallPath = serviceInstallerMgr.getInstallPath(hostInfo,
                    getServiceType().getInstllPack());
            try {
                startNode(ssh, node, serviceInstallPath, null);
            }
            catch (Exception e) {
                throw new Exception("failed to start node, please check remote log " + host + ":("
                        + serviceInstallPath + "/log/", e);
            }
        }
        finally {
            ssh.close();
        }
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceRemoteInstallPath,
            String deployJsonFileRemotePath) {
        return serviceRemoteInstallPath + "/bin/scmd.sh start";
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node) {
        return null;
    }
}
