package com.sequoiacm.deploy.cleaner;

import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.core.ScmDeployInfoMgr;
import com.sequoiacm.deploy.installer.ServicesInstallPackManager;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallConfig;
import com.sequoiacm.deploy.module.InstallPackType;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshMgr;

public abstract class ServiceCleanerBase implements ServiceCleaner {
    private static final Logger logger = LoggerFactory.getLogger(ServiceCleanerBase.class);

    private SshMgr sshFactory = SshMgr.getInstance();
    private InstallPackType type;
    private ScmDeployInfoMgr deployInfoMgr = ScmDeployInfoMgr.getInstance();

    private String installPath;

    private String stopScriptName;

    private String stopScriptOption;

    private ServicesInstallPackManager installPackManager = ServicesInstallPackManager
            .getInstance();

    public ServiceCleanerBase(InstallPackType type, String stopScriptName,
            String stopScriptOption) {
        this.type = type;
        this.stopScriptName = stopScriptName;
        this.stopScriptOption = stopScriptOption;
    }

    @Override
    public InstallPackType getType() {
        return type;
    }

    public String getInstallPath() {
        if (installPath == null) {
            installPath = deployInfoMgr.getInstallConfig().getInstallPath() + "/"
                    + type.getUntarDirName(installPackManager.getServicePack(type).getName());
        }
        return installPath;
    }

    public String getStopScriptName() {
        return stopScriptName;
    }

    public String getStopScriptOption() {
        return stopScriptOption;
    }

    @Override
    public void clean(HostInfo host, boolean dryRun) {
        if (dryRun) {
            logger.info("Directory will be delete:host=" + host.getHostName() + ", dir="
                    + getInstallPath());
            return;
        }

        Ssh ssh = null;
        try {
            ssh = sshFactory.getSsh(host);
            stopNode(ssh, deployInfoMgr.getInstallConfig(), host.getJavaHome());
            removeInstallPath(ssh);
        }
        catch (Exception e) {
            logger.warn("failed to clean:host={}, service={}", host.getHostName(), getType(), e);
        }
        finally {
            CommonUtils.closeResource(ssh);
        }
    }

    protected void removeInstallPath(Ssh ssh) {
        try {
            ssh.sudoExec("rm -rf " + getInstallPath());
        }
        catch (Exception e) {
            logger.warn("failed to remove install dir:" + getInstallPath(), e);
        }
    }

    protected void stopNode(Ssh ssh, InstallConfig installConfig, String javaHome) {
        String installUser = installConfig.getInstallUser();
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("JAVA_HOME", javaHome);
        env.put("PATH", "$JAVA_HOME/bin:$PATH");
        try {
            String stopScript = getInstallPath() + "/bin/" + stopScriptName;
            int isExists = ssh.sudoExec("ls " + stopScript, 0, 2);
            if (isExists == 0) {
                ssh.sudoSuExec(installUser, stopScript + " " + stopScriptOption, env);
            }
        }
        catch (Exception e) {
            logger.warn("failed to stop service:host={}, serviceType={}", ssh.getHost(), getType(), e);
        }
    }

}
