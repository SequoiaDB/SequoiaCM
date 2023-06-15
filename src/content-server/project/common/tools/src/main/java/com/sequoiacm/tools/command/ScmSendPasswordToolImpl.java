package com.sequoiacm.tools.command;

import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.Ssh;
import com.sequoiacm.tools.element.ScmHostInfo;
import com.sequoiacm.tools.exception.ScmExitCode;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ScmSendPasswordToolImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmSendPasswordToolImpl.class);

    private static final String LONG_USER = "user";
    private static final String SHORT_USER = "u";

    private static final String LONG_PASSWD = "password";
    private static final String SHORT_PASSWD = "p";

    private static final String LONG_HOSTS = "hosts";
    private static final String LONG_HOSTS_FILE = "hosts-file";
    private static final String LONG_SAVE_PATH = "save-path";
    private static final String LONG_OVERRIDE = "override";

    private String username;
    private String password;
    private String savePath;
    private boolean override;
    private List<ScmHostInfo> hostList;
    private List<PasswordSender> passwordSenders = new ArrayList<>();

    private final Options ops;
    private final ScmHelpGenerator hp;

    public ScmSendPasswordToolImpl() throws ScmToolsException {
        super("sendpassword");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(SHORT_USER, LONG_USER, "username.", true, true, false));
        ops.addOption(hp.createOpt(SHORT_PASSWD, LONG_PASSWD, "password for encrypt.", false, true,
                false));
        ops.addOption(hp.createOpt(null, LONG_HOSTS,
                "hosts to send password files. example: server1:port,server2:port", false, true,
                false));
        ops.addOption(hp.createOpt(null, LONG_HOSTS_FILE,
                "hosts file to send password files. file content example: server1,22,scmadmin,admin",
                false, true, false));
        ops.addOption(hp.createOpt(null, LONG_SAVE_PATH,
                "the storage location of the password file on the remote host.", true, true,
                false));
        ops.addOption(hp.createOpt(null, LONG_OVERRIDE,
                "overwrite when password file with the same name exists on the remote host.", false,
                false, false));

    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        checkAndParseArgs(args);
        try {
            // 1.生成待发送密码文件的内容
            String pwdFileContent = generatePwdFileContent();

            // 2.与各个主机建立连接
            initPasswordSenders();

            // 3.把密码文件拷贝到各个机器上
            int processCount = 0;
            List<String> successHosts = new ArrayList<>();
            for (PasswordSender sender : passwordSenders) {
                try {
                    processCount++;
                    boolean isFileExists = sender.isFileExists(savePath);
                    if (!override && isFileExists) {
                        System.out.println("[" + processCount + "]" + "Send password file to "
                                + sender.getHost() + " failed: file is already exists");
                    }
                    else {
                        sender.send(pwdFileContent, savePath);
                        successHosts.add(sender.getHost());
                        System.out.println("[" + processCount + "]" + "Send password file to "
                                + sender.getHost() + " success"
                                + (override && isFileExists ? "(override)" : ""));
                    }
                }
                catch (Exception e) {
                    System.out.println("[" + processCount + "]" + "Send password file to "
                            + sender.getHost() + " failed: " + e.getMessage());
                    logger.error("send password file to " + sender.getHost() + " failed", e);
                }
            }

            // 4.打印汇总结果
            System.out.println("Process finished, success: " + successHosts.size() + ", failed: "
                    + (processCount - successHosts.size()));
            if (successHosts.size() > 0) {
                System.out.println("The password file is saved on the hosts: " + successHosts
                        + ", path: " + savePath);
            }
        }
        finally {
            destroy();
        }

    }

    private String generatePwdFileContent() throws ScmToolsException {
        try {
            String encrypted = ScmPasswordMgr.getInstance()
                    .encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, password);
            return username + ":" + encrypted;
        }
        catch (Exception e) {
            throw new ScmToolsException("failed to encrypt password", ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    private void initPasswordSenders() throws ScmToolsException {
        for (ScmHostInfo hostInfo : this.hostList) {
            Ssh ssh = null;
            try {
                PasswordSender passwordSender;
                if (ScmCommon.isLocalHost(hostInfo.getHost())) {
                    passwordSender = new LocalPasswordSender(hostInfo.getHost());
                }
                else {
                    ssh = new Ssh(hostInfo.getHost(), hostInfo.getPort(), hostInfo.getUsername(),
                            hostInfo.getPassword());
                    if (!ssh.isSftpAvailable()) {
                        throw new ScmToolsException(
                                "sftp service may not be available, host=" + hostInfo.getHost(),
                                ScmExitCode.SYSTEM_ERROR);
                    }
                    passwordSender = new RemotePasswordSender(ssh);
                }
                passwordSenders.add(passwordSender);
            }
            catch (ScmToolsException e) {
                ScmCommon.closeResource(ssh);
                throw e;
            }
            catch (Exception e) {
                ScmCommon.closeResource(ssh);
                throw new ScmToolsException("failed to connect to " + hostInfo.getHost(),
                        ScmExitCode.SYSTEM_ERROR, e);
            }
        }

    }

    private void destroy() {
        for (PasswordSender passwordSender : passwordSenders) {
            passwordSender.close();
        }
    }

    private void checkAndParseArgs(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        if (cl.hasOption(LONG_HOSTS) && cl.hasOption(LONG_HOSTS_FILE)) {
            throw new ScmToolsException("param: " + LONG_HOSTS + " and " + LONG_HOSTS_FILE
                    + " can not be specified at the same time", ScmExitCode.INVALID_ARG);
        }
        if (!cl.hasOption(LONG_HOSTS) && !cl.hasOption(LONG_HOSTS_FILE)) {
            throw new ScmToolsException("param: " + LONG_HOSTS + " or " + LONG_HOSTS_FILE
                    + " must be specified at least one", ScmExitCode.INVALID_ARG);
        }

        this.username = cl.getOptionValue(SHORT_USER);
        this.password = cl.getOptionValue(SHORT_PASSWD);
        if (this.password == null) {
            System.out.print("password: ");
            this.password = ScmCommandUtil.readPasswdFromStdIn();
        }
        this.savePath = cl.getOptionValue(LONG_SAVE_PATH);
        this.override = cl.hasOption(LONG_OVERRIDE);

        String hostsStr = cl.getOptionValue(LONG_HOSTS);
        String hostsFilePath = cl.getOptionValue(LONG_HOSTS_FILE);
        if (hostsStr != null) {
            this.hostList = parseHostsFromString(hostsStr);
            if (hostList.isEmpty()) {
                throw new ScmToolsException("hosts info is empty: hosts=" + hostsStr,
                        ScmExitCode.INVALID_ARG);
            }
        }
        else {
            this.hostList = parseHostsFromFile(hostsFilePath);
            if (hostList.isEmpty()) {
                throw new ScmToolsException("hosts file is empty:filePath=" + hostsFilePath,
                        ScmExitCode.INVALID_ARG);
            }
        }

    }

    // hosts: server1:port,server2,server3:port
    private List<ScmHostInfo> parseHostsFromString(String hosts) throws ScmToolsException {
        List<ScmHostInfo> res = new ArrayList<>();
        String[] hostArray = hosts.split(",");
        for (String hostStr : hostArray) {
            String[] split = hostStr.split(":");
            if (split.length < 1 || split.length > 2) {
                throw new ScmToolsException(
                        "param: " + LONG_HOSTS
                                + " must be in the format: server1[:port],server2[:port]",
                        ScmExitCode.INVALID_ARG);
            }
            String host = split[0].trim();
            if (host.isEmpty()) {
                throw new ScmToolsException(
                        "param: " + LONG_HOSTS
                                + " must be in the format: server1[:port],server2[:port]",
                        ScmExitCode.INVALID_ARG);
            }
            int port = 22;
            if (split.length == 2) {
                try {
                    port = Integer.parseInt(split[1].trim());
                }
                catch (NumberFormatException e) {
                    throw new ScmToolsException("invalid port number: " + split[1],
                            ScmExitCode.INVALID_ARG);
                }
            }
            ScmHostInfo hostInfo = new ScmHostInfo(host, port);
            if (res.contains(hostInfo)) {
                throw new ScmToolsException("repeat host info:" + hostInfo,
                        ScmExitCode.INVALID_ARG);
            }
            res.add(hostInfo);
        }
        return res;
    }

    private List<ScmHostInfo> parseHostsFromFile(String hostsFilePath) throws ScmToolsException {
        List<ScmHostInfo> res = new ArrayList<>();
        File file = new File(hostsFilePath);
        if (!file.isAbsolute()) {
            file = new File(ScmCommon.getUserWorkingDir(), hostsFilePath);
        }
        if (!file.exists()) {
            throw new ScmToolsException("hosts file does not exist:" + hostsFilePath,
                    ScmExitCode.FILE_NOT_FIND);
        }
        if (!file.isFile()) {
            throw new ScmToolsException(hostsFilePath + " is not a file", ScmExitCode.INVALID_ARG);
        }

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            // line: hostname,port,user,password
            while ((line = bufferedReader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] split = line.split(",");
                if (split.length != 4) {
                    throw new ScmToolsException("invalid host info:" + line,
                            ScmExitCode.INVALID_ARG);
                }
                String hostname = split[0].trim();
                if (hostname.isEmpty()) {
                    throw new ScmToolsException("invalid host info:" + line + ", hostname is empty",
                            ScmExitCode.INVALID_ARG);
                }
                int port;
                try {
                    port = Integer.parseInt(split[1].trim());
                }
                catch (NumberFormatException e) {
                    throw new ScmToolsException("invalid port number: " + split[1],
                            ScmExitCode.INVALID_ARG);
                }
                String tempUser = split[2].trim();
                if (tempUser.isEmpty()) {
                    throw new ScmToolsException("invalid host info:" + line + ", username is empty",
                            ScmExitCode.INVALID_ARG);
                }
                String tempPassword = split[3].trim();
                if (tempPassword.isEmpty()) {
                    throw new ScmToolsException("invalid host info:" + line + ", password is empty",
                            ScmExitCode.INVALID_ARG);
                }

                ScmHostInfo hostInfo = new ScmHostInfo(hostname, port, tempUser, tempPassword);
                if (res.contains(hostInfo)) {
                    throw new ScmToolsException("repeat host info:" + hostInfo,
                            ScmExitCode.INVALID_ARG);
                }
                res.add(hostInfo);
            }
        }
        catch (ScmToolsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmToolsException("failed to parse hosts file: " + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR);
        }
        finally {
            ScmCommon.closeResource(bufferedReader);
        }
        return res;
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}

interface PasswordSender {

    boolean isFileExists(String filePath) throws IOException;

    void send(String fileContent, String savePath) throws IOException;

    void close();

    String getHost();
}

class LocalPasswordSender implements PasswordSender {

    private String hostname;

    public LocalPasswordSender(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public boolean isFileExists(String filePath) {
        return new File(filePath).exists();
    }

    @Override
    public void send(String fileContent, String savePath) throws IOException {
        try {
            Files.write(Paths.get(savePath), fileContent.getBytes());
        }
        catch (AccessDeniedException e) {
            throw new IOException("Permission denied", e);
        }
    }

    @Override
    public void close() {

    }

    @Override
    public String getHost() {
        return hostname;
    }
}

class RemotePasswordSender implements PasswordSender {

    private final Ssh ssh;

    public RemotePasswordSender(Ssh ssh) {
        this.ssh = ssh;
    }

    @Override
    public boolean isFileExists(String filePath) throws IOException {
        return ssh.isFileExists(filePath);
    }

    @Override
    public void send(String fileContent, String savePath) throws IOException {
        ssh.scp(new ByteArrayInputStream(fileContent.getBytes()), savePath);
    }

    @Override
    public void close() {
        ssh.close();
    }

    @Override
    public String getHost() {
        return ssh.getHost();
    }
}
