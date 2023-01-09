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
    private List<HostInfo> alredySendAdminPassword = new ArrayList<>();
    private Map<HostInfo, List<DataSourceInfo>> alreadySendDatasourcePassword = new HashMap<>();

    // key 主机名， value 已经发送的文件列表 （元数据服务、数据服务的密码文件不在这个数据结构维护，它们被单独维护在前面几个变量上）
    private Map<HostInfo, List<String>> commonAlreadySendFile = new HashMap<>();

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

    public String getAdminPasswdFilePath() {
        return deployInfoMgr.getInstallConfig().getInstallPath() + "/" + SECRET_DIR_NAME
                + "/admin.pwd";
    }


    private List<String> commonAlreadySendFiles(HostInfo host) {
        List<String> sendFiles = commonAlreadySendFile.get(host);
        if (sendFiles == null) {
            sendFiles = new ArrayList<>();
            commonAlreadySendFile.put(host, sendFiles);
        }
        return sendFiles;
    }

    public String sendPlaintextAsPasswordFile(HostInfo host, String userName,
            String plainTextPassword, String remoteFileName) throws Exception {
        String remoteDirPath = deployInfoMgr.getInstallConfig().getInstallPath() + "/"
                + SECRET_DIR_NAME + "/" + remoteFileName;
        List<String> sendFiles = commonAlreadySendFiles(host);
        if (sendFiles.contains(remoteDirPath)) {
            return remoteDirPath;
        }
        sendPwdFile(host, userName, plainTextPassword, null,
                deployInfoMgr.getInstallConfig().getInstallPath() + "/" + SECRET_DIR_NAME + "/",
                remoteFileName);
        sendFiles.add(remoteDirPath);
        return remoteDirPath;
    }

    public String sendFile(HostInfo host, String localFilePath, String remoteFileName)
            throws Exception {
        String remoteDirPath = deployInfoMgr.getInstallConfig().getInstallPath() + "/"
                + SECRET_DIR_NAME + "/" + remoteFileName;
        List<String> sendFiles = commonAlreadySendFiles(host);
        if (sendFiles.contains(remoteDirPath)) {
            return remoteDirPath;
        }
        sendPwdFile(host, null, null, localFilePath,
                deployInfoMgr.getInstallConfig().getInstallPath() + "/" + SECRET_DIR_NAME + "/",
                remoteFileName);
        sendFiles.add(remoteDirPath);
        return remoteDirPath;
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

    public String sendAdminPasswdFile(HostInfo host,String adminUsername, String adminPassword)
            throws Exception {
        String remotePasswdFilePath = deployInfoMgr.getInstallConfig().getInstallPath() + "/"
                + SECRET_DIR_NAME + "/admin.pwd";
        if (alredySendAdminPassword.contains(host)) {
            logger.debug("admin password file has been send:host=" + host.getHostName()
                    + ", file=" + remotePasswdFilePath);
            return remotePasswdFilePath;
        }

        sendPwdFile(host, adminUsername, adminPassword, null,
                deployInfoMgr.getInstallConfig().getInstallPath() + "/" + SECRET_DIR_NAME + "/",
                "admin.pwd");
        alredySendAdminPassword.add(host);
        return remotePasswdFilePath;
    }

    private boolean isFileExist(Ssh ssh, String filePath) throws IOException {
        int isExists = ssh.sudoExec("ls " + filePath, 0, 2);
        if (isExists == 0) {
            return true;
        }
        return false;
    }

    private void sendPwdFile(HostInfo host, String userName, String plainText,
            String localPasswordFile, String remoteDirPath, String fileName)
            throws IOException, Exception {
        Ssh ssh = sshFactory.getSsh(host);
        try {
            if (isFileExist(ssh, remoteDirPath + fileName)) {
                throw new Exception("failed to send file to remote host, file already exist: host="
                        + host.getHostName() + ", file=" + remoteDirPath + fileName);
            }

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
                ssh.changeOwner(remoteDirPath + fileName,
                        deployInfoMgr.getInstallConfig().getInstallUser(),
                        deployInfoMgr.getInstallConfig().getInstallUserGroup());
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
