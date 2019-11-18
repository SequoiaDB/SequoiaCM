package com.sequoiacm.deploy.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.module.DataSourceInfo;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.MetaSourceInfo;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshMgr;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;

public class ScmPasswordFileSender {
    private static final Logger logger = LoggerFactory.getLogger(ScmPasswordFileSender.class);

    private static final String SECRET_DIR_NAME = "secret";

    private SshMgr sshFactory = SshMgr.getInstance();

    // host to passwordFiles
    private List<HostInfo> alredySendMetasourcePassword = new ArrayList<>();
    private List<HostInfo> alredySendAuditsourcePassword = new ArrayList<>();
    private Map<HostInfo, List<DataSourceInfo>> alreadySendDatasourcePassword = new HashMap<>();

    private ScmDeployInfoMgr deployInfoMgr = ScmDeployInfoMgr.getInstance();

    private static volatile ScmPasswordFileSender instance;

    public static ScmPasswordFileSender getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ScmPasswordFileSender.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ScmPasswordFileSender();
            return instance;
        }
    }

    public String getDsPasswdFilePath(DataSourceInfo datasouce) {
        return deployInfoMgr.getInstallConfig().getInstallPath() + "/" + SECRET_DIR_NAME + "/"
                + datasouce.getName() + ".pwd";
    }

    public String getMetasourcePasswdFilePath() {
        return deployInfoMgr.getInstallConfig().getInstallPath() + "/" + SECRET_DIR_NAME
                + "/metasource.pwd";
    }

    public String getAuditsourcePasswdFilePath() {
        return deployInfoMgr.getInstallConfig().getInstallPath() + "/" + SECRET_DIR_NAME
                + "/auditsource.pwd";
    }

    public String sendDsPasswdFile(HostInfo host, DataSourceInfo datasouce) throws Exception {
        String remoteDirPath = deployInfoMgr.getInstallConfig().getInstallPath() + "/"
                + SECRET_DIR_NAME + "/" + datasouce.getName() + ".pwd";

        List<DataSourceInfo> datasources = alreadySendDatasourcePassword.get(host);
        if (datasources != null && datasources.contains(datasouce)) {
            return remoteDirPath;
        }
        if (datasources == null) {
            datasources = new ArrayList<>();
            alreadySendDatasourcePassword.put(host, datasources);
        }

        sendPwdFile(host, datasouce.getUser(), datasouce.getPassword(), datasouce.getPasswordFile(),
                deployInfoMgr.getInstallConfig().getInstallPath() + "/" + SECRET_DIR_NAME + "/",
                datasouce.getName() + ".pwd");
        datasources.add(datasouce);
        return remoteDirPath;

    }

    public String sendMetasourcePasswdFile(HostInfo host, MetaSourceInfo metasource)
            throws Exception {
        String remotePasswdFilePath = deployInfoMgr.getInstallConfig().getInstallPath() + "/"
                + SECRET_DIR_NAME + "/metasource.pwd";
        if (alredySendMetasourcePassword.contains(host)) {
            logger.debug("metasource password file has been send:host=" + host.getHostName()
                    + ", file=" + remotePasswdFilePath);
            return remotePasswdFilePath;
        }

        sendPwdFile(host, metasource.getUser(), metasource.getPassword(), null,
                deployInfoMgr.getInstallConfig().getInstallPath() + "/" + SECRET_DIR_NAME + "/",
                "metasource.pwd");
        alredySendMetasourcePassword.add(host);
        return remotePasswdFilePath;
    }

    private void sendPwdFile(HostInfo host, String userName, String plainText,
            String localPasswordFile, String remoteDirPath, String fileName)
            throws IOException, Exception {
        Ssh ssh = sshFactory.getSsh(host);
        try {
            ssh.sudoSuExec(deployInfoMgr.getInstallConfig().getInstallUser(),
                    "mkdir -p " + remoteDirPath, null);
            if (plainText != null) {
                String encryptPwd = ScmPasswordMgr.getInstance()
                        .encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, plainText);
                ssh.sudoSuExec(deployInfoMgr.getInstallConfig().getInstallUser(),
                        "echo " + userName + ":" + encryptPwd + " > " + remoteDirPath + fileName,
                        null);
                return;
            }

            if (localPasswordFile != null) {
                ssh.scp(localPasswordFile, remoteDirPath + fileName);
                ssh.sudoExec("chown " + deployInfoMgr.getInstallConfig().getInstallUser() + " "
                        + remoteDirPath + fileName);
                return;
            }
            throw new IllegalArgumentException("password did not define");
        }
        finally {
            ssh.close();
        }
    }

    public String sendAuditSourcePasswdFile(HostInfo host, MetaSourceInfo metasource)
            throws Exception {
        String remotePasswdFilePath = deployInfoMgr.getInstallConfig().getInstallPath() + "/"
                + SECRET_DIR_NAME + "/auditsource.pwd";
        if (alredySendAuditsourcePassword.contains(host)) {
            logger.debug("auditsource password file has been send:host=" + host.getHostName()
                    + ", file=" + remotePasswdFilePath);
            return remotePasswdFilePath;
        }

        sendPwdFile(host, metasource.getUser(), metasource.getPassword(), null,
                deployInfoMgr.getInstallConfig().getInstallPath() + "/" + SECRET_DIR_NAME + "/",
                "auditsource.pwd");
        alredySendAuditsourcePassword.add(host);
        return remotePasswdFilePath;
    }

    public void cleanPasswordFile(HostInfo host, boolean dryRun) throws IOException {
        if (dryRun) {
            logger.info("Directory will be delete:host=" + host.getHostName() + ", dir="
                    + deployInfoMgr.getInstallConfig().getInstallPath() + "/" + SECRET_DIR_NAME);
            return;
        }
        Ssh ssh = sshFactory.getSsh(host);
        try {
            ssh.sudoExec("rm -rf " + deployInfoMgr.getInstallConfig().getInstallPath() + "/"
                    + SECRET_DIR_NAME);
        }
        finally {
            ssh.close();
        }
    }

}
