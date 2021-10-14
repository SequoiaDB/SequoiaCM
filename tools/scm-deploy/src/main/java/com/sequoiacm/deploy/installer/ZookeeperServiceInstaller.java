package com.sequoiacm.deploy.installer;

import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallConfig;
import com.sequoiacm.deploy.module.InstallPackType;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshMgr;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Installer
public class ZookeeperServiceInstaller extends ServiceInstallerBase {
    private Map<String, String> shellMap;

    public ZookeeperServiceInstaller() {
        super(InstallPackType.ZOOKEEPER);

        shellMap = new HashMap<>();
        shellMap.put("zookeeper-3.4.12", "3.4.12.sh");
    }

    @Override
    public String install(HostInfo host) throws Exception {
        String remoteInstallPath = super.install(host);
        Ssh ssh = sshFactory.getSsh(host);
        try {
            // 根据 zookeeper 版本，替换对应的zkServer.sh脚本，这是为了集成 daemon tool
            File zkDir = new File(remoteInstallPath);
            String zkDirName = zkDir.getName();
            String scmZkScript = shellMap.get(zkDirName);
            // zookeeper 版本升级之后，一定需要修改 zkServer.sh
            if (scmZkScript == null) {
                throw new IllegalArgumentException(
                        "did not find matched version of zookeeper script:"
                                + zkDirName.substring(zkDirName.indexOf("-") + 1));
            }
            CommonConfig commonConfig = CommonConfig.getInstance();
            String remoteZkBinPath = remoteInstallPath + "/bin";
            String localZkScriptPath = commonConfig.getBasePath() + "sequoiacm-deploy/bindata/zk_shell/" + scmZkScript;
            ssh.scp(localZkScriptPath, remoteZkBinPath);
            String oldShell = remoteZkBinPath + "/" + scmZkScript;
            String newShell = remoteZkBinPath + "/zkServer.sh";
            ssh.sudoExec("mv -f " + oldShell + " " + newShell, 0);
            ssh.changeOwner(remoteZkBinPath, installConfig.getInstallUser(),
                    installConfig.getInstallUserGroup());
            ssh.sudoSuExec(installConfig.getInstallUser(), "chmod +x " + remoteZkBinPath + "/*.sh",
                    null);

            // 在 zookeeper 安装目录的 conf 下创建 java.env， 这个文件里面的变量将会作为 zk 的启动参数
            String createJavaEnvCmd = " echo -e \"" +
            // 调整log4j日志输出路径
                    "export ZOO_LOG_DIR=\\\"\\$ZOOKEEPER_PREFIX/logs\\\" \\n" +
                    // 设置log4j日志翻转
                    "export ZOO_LOG4J_PROP=\\\"INFO,ROLLINGFILE\\\"\" " + "> " + remoteInstallPath
                    + "/conf/java.env";
            ssh.sudoSuExec(installConfig.getInstallUser(), createJavaEnvCmd, null);

            // 调整日志最大个数
            String changeLogFileMaxBackupCmd = "echo \"log4j.appender.ROLLINGFILE.MaxBackupIndex=10\" >> "
                    + remoteInstallPath + "/conf/log4j.properties";
            ssh.sudoSuExec(installConfig.getInstallUser(), changeLogFileMaxBackupCmd, null);
        }
        finally {
            ssh.close();
        }

        return remoteInstallPath;

    }
}
