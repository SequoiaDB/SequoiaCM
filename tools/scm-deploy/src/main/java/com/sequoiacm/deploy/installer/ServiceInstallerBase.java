package com.sequoiacm.deploy.installer;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.core.ScmDeployInfoMgr;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallConfig;
import com.sequoiacm.deploy.module.InstallPackType;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshMgr;

public class ServiceInstallerBase implements ServiceInstaller {
    private static final Logger logger = LoggerFactory.getLogger(ServiceInstallerBase.class);
    protected SshMgr sshFactory = SshMgr.getInstance();

    protected ServicesInstallPackManager packManager = ServicesInstallPackManager.getInstance();

    protected InstallPackType type;

    protected InstallConfig installConfig =  ScmDeployInfoMgr.getInstance().getInstallConfig();

    public ServiceInstallerBase(InstallPackType type) {
        this.type = type;
    }

    @Override
    public String install(HostInfo host) throws Exception {
        File installPack = packManager.getServicePack(getType());

        Ssh ssh = sshFactory.getSsh(host);
        try {
            String installPath = installConfig.getInstallPath() + "/";
            String untarPath = installPath + getType().getUntarDirName(installPack.getName()) + "/";

            int isExists = ssh.sudoExec("ls " + untarPath, 0, 2);
            if (isExists == 0) {
                throw new IllegalArgumentException("install failed, dir alread exist: host="
                        + host.getHostName() + ", dir=" + untarPath);
            }

            ssh.sudoExec("tar -xf '" + ssh.getScpTmpPath()
                    + CommonConfig.getInstance().getRemoteInstallPackPath() + "/package/"
                    + installPack.getName() + "' -C " + installPath);
            ssh.changeOwner(untarPath, installConfig.getInstallUser(),
                    installConfig.getInstallUserGroup());

            ssh.sudoSuExec(installConfig.getInstallUser(), "chmod +x " + untarPath + "/bin/*.sh",
                    null);

            return untarPath;
        }
        finally {
            ssh.close();
        }
    }

    @Override
    public InstallPackType getType() {
        return type;
    }

}
