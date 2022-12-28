package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.core.ScmDeployInfoMgr;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallPackType;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Cleaner
public class NonServiceCleaner implements ServiceCleaner {
    private static final Logger logger = LoggerFactory.getLogger(ServiceCleanerBase.class);

    private SshMgr sshFactory = SshMgr.getInstance();
    private InstallPackType type;
    private ScmDeployInfoMgr deployInfoMgr = ScmDeployInfoMgr.getInstance();

    private List<String> installPathList;

    public NonServiceCleaner() {
        this.type = InstallPackType.NON_SERVICE;
    }

    @Override
    public InstallPackType getType() {
        return type;
    }

    @Override
    public void clean(HostInfo host, boolean dryRun) throws Exception {
        if (dryRun) {
            logger.info("Directory will be delete:host=" + host.getHostName() + ", dir="
                    + getInstallPathList().toString());
            return;
        }
        Ssh ssh = null;
        try {
            ssh = sshFactory.getSsh(host);
            for (String installPath : getInstallPathList()) {
                removeInstallPath(ssh, installPath);
            }
        } finally {
            CommonUtils.closeResource(ssh);
        }
    }

    public void removeInstallPath(Ssh ssh, String installPath) throws Exception {
        try {
            String cmd = "rm -rf " + installPath;
            ssh.sudoExec(cmd);
        } catch (Exception e) {
            throw new Exception("failed to remove install dir:" + installPath, e);
        }
    }

    private List<String> getInstallPathList() {
        String installPathPrefix = deployInfoMgr.getInstallConfig().getInstallPath() + "/";
        if (installPathList == null) {
            installPathList = new ArrayList<>();
            for (String dirName : type.getDirs()) {
                installPathList.add(installPathPrefix + dirName);
            }
        }
        return installPathList;
    }
}
