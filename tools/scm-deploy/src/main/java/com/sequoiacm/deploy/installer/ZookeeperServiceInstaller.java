package com.sequoiacm.deploy.installer;

import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallConfig;
import com.sequoiacm.deploy.module.InstallPackType;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshMgr;

@Installer
public class ZookeeperServiceInstaller extends ServiceInstallerBase {
    public ZookeeperServiceInstaller() {
        super(InstallPackType.ZOOKEEPER);
    }

    @Override
    public String install(HostInfo host) throws Exception {
        String remoteInstallPath = super.install(host);
        Ssh ssh = sshFactory.getSsh(host);
        try {
            // 在 zookeeper 安装目录的 conf 下创建 java.env， 这个文件里面的变量将会作为 zk 的启动参数
            String createJavaEnvCmd = " echo -e \"" +
                    // 调整log4j日志输出路径
                    "export ZOO_LOG_DIR=\\\"\\$ZOOKEEPER_PREFIX/logs\\\" \\n" +
                    // 设置log4j日志翻转
                    "export ZOO_LOG4J_PROP=\\\"INFO,ROLLINGFILE\\\"\" " +
                    "> " + remoteInstallPath + "/conf/java.env";
            ssh.sudoSuExec(installConfig.getInstallUser(), createJavaEnvCmd, null);

            // 调整日志最大个数
            String changeLogFileMaxBackupCmd = "echo \"log4j.appender.ROLLINGFILE.MaxBackupIndex=10\" >> " + remoteInstallPath + "/conf/log4j.properties";
            ssh.sudoSuExec(installConfig.getInstallUser(), changeLogFileMaxBackupCmd, null);
        } finally {
            ssh.close();
        }

        return remoteInstallPath;

    }
}
