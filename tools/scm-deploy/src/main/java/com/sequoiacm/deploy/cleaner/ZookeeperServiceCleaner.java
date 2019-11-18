package com.sequoiacm.deploy.cleaner;

import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.module.InstallConfig;
import com.sequoiacm.deploy.module.InstallPackType;
import com.sequoiacm.deploy.ssh.Ssh;

@Cleaner
public class ZookeeperServiceCleaner extends ServiceCleanerBase {
    private static final Logger logger = LoggerFactory.getLogger(ServiceCleanerBase.class);

    public ZookeeperServiceCleaner() {
        super(InstallPackType.ZOOKEEPER, "zkServer.sh", "stop");
    }

    @Override
    protected void stopNode(Ssh ssh, InstallConfig installConfig, String javaHome) {
        String installUser = installConfig.getInstallUser();
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("JAVA_HOME", javaHome);
        env.put("PATH", "$JAVA_HOME/bin:$PATH");
        try {
            String stopScript = getInstallPath() + "/bin/" + getStopScriptName();
            int isExists = ssh.sudoExec("ls " + stopScript, 0, 2);
            if (isExists == 0) {
                String lsZooCfg = "ls " + getInstallPath() + "/conf/zoo*";
                String stopCmd = stopScript + " " + getStopScriptOption();
                ssh.sudoSuExec(installUser,
                        "for f in `" + lsZooCfg + "`; do " + stopCmd + "  $f; done", env);
            }
        }
        catch (Exception e) {
            logger.warn("failed to stop service:serviceType={}", getType(), e);
        }
    }
}
