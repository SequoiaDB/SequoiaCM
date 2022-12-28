package com.sequoiacm.deploy.installer;

import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.core.ScmDeployInfoMgr;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallConfig;
import com.sequoiacm.deploy.module.InstallPackType;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshMgr;

import java.io.File;
import java.util.List;

@Installer
public class NonServiceInstaller implements ServiceInstaller {
    private final SshMgr sshFactory = SshMgr.getInstance();

    private final ServicesInstallPackManager packManager = ServicesInstallPackManager.getInstance();

    private InstallPackType type;

    private final InstallConfig installConfig = ScmDeployInfoMgr.getInstance().getInstallConfig();

    private final String TOOLS = "tools";

    public NonServiceInstaller() {
        this.type = InstallPackType.NON_SERVICE;
    }

    @Override
    public String install(HostInfo host) throws Exception {
        Ssh ssh = sshFactory.getSsh(host);
        List<File> nonServiceFiles = packManager.getNonServiceFiles();
        String installPath = installConfig.getInstallPath() + "/";
        try {
            for (File nonServiceFile : nonServiceFiles) {
                int isExists = ssh.sudoExec("ls " + installPath + nonServiceFile.getName(), 0, 2);
                if (isExists == 0) {
                    throw new IllegalArgumentException("install failed, dir already exist: host="
                            + host.getHostName() + ", dir=" + installPath + nonServiceFile.getName());
                }
                ssh.sudoExec("cp -rf " + ssh.getScpTmpPath()
                        + CommonConfig.getInstance().getRemoteInstallPackPath() + "/" + nonServiceFile.getName()
                        + " " + installPath);
                ssh.changeOwner(installPath + nonServiceFile.getName(), installConfig.getInstallUser(), installConfig.getInstallUserGroup());
                if (TOOLS.equals(nonServiceFile.getName())) {
                    ssh.sudoSuExecRes(installConfig.getInstallUser(), "chmod +x " + installPath + nonServiceFile.getName() + "/*/bin/*.sh", null);
                }
            }
        } finally {
            ssh.close();
        }
        return null;
    }

    @Override
    public InstallPackType getType() {
        return type;
    }
}
