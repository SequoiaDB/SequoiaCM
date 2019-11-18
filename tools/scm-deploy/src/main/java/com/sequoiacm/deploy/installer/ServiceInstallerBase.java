package com.sequoiacm.deploy.installer;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallConfig;
import com.sequoiacm.deploy.module.InstallPackType;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshMgr;

public class ServiceInstallerBase implements ServiceInstaller {
    private static final Logger logger = LoggerFactory.getLogger(ServiceInstallerBase.class);
    private SshMgr sshFactory;

    private ServicesInstallPackManager packManager;

    private InstallPackType type;

    private InstallConfig installConfig;

    public ServiceInstallerBase(InstallPackType type, SshMgr sshFactory,
            ServicesInstallPackManager packManager, InstallConfig installConfig) {
        this.type = type;
        this.sshFactory = sshFactory;
        this.packManager = packManager;
        this.installConfig = installConfig;
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

            ssh.scp(installPack.getAbsolutePath(), ssh.getScpTmpPath());
            ssh.sudoExec(
                    "tar -xf " + ssh.getScpTmpPath() + installPack.getName() + " -C " + installPath,
                    0);
            ssh.sudoExec("chown " + installConfig.getInstallUser() + " " + untarPath + " -R");

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
