package com.sequoiacm.test.ssh;

import com.jcraft.jsch.*;
import com.sequoiacm.test.common.CommonDefine;
import com.sequoiacm.test.module.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.Vector;

public class Ssh implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(Ssh.class);
    private static final int CONNECT_TIMEOUT = 3 * 60 * 1000;
    private static final String ENV_PATH = "/etc/profile";

    private Session session;
    private String host;
    private int port;
    private String username;
    private String password;
    private SshMgr sshMgr;
    private JSch jsch;
    private boolean isRootUser;

    Ssh(SshMgr sshMgr, String host, int port, String username, String password) throws IOException {
        this.jsch = new JSch();
        this.host = host;
        this.port = port;
        this.username = username;
        this.isRootUser = username.equals("root");
        this.password = password;
        this.sshMgr = sshMgr;
        try {
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(CONNECT_TIMEOUT);
        }
        catch (Exception e) {
            disconnect();
            throw new IOException("Failed to connect to " + host + ":" + port, e);
        }
    }

    public String getHost() {
        return host;
    }

    public boolean isSftpAvailable() {
        ChannelSftp channel = null;
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CONNECT_TIMEOUT);
        }
        catch (JSchException e) {
            logger.debug("Failed to connect to sftp service, host={}", host, e);
            return false;
        }
        finally {
            if (channel != null) {
                channel.disconnect();
            }
        }

        return true;
    }

    public String searchEnv(String key) throws IOException {
        String command = "echo -e '#!/bin/bash \n . " + ENV_PATH + " > /dev/null \n echo $" + key
                + "' > ./searchEnv.sh && /bin/bash ./searchEnv.sh && rm ./searchEnv.sh";
        ExecResult execResult = internalSudoExec(command);
        String envValue = execResult.getStdOut();
        if (envValue == null || envValue.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "Env " + key + " not define, host=" + host + ", searchPath=" + ENV_PATH);
        }

        return envValue.trim();
    }

    public void scpFileFrom(String localPath, String remotePath) throws IOException {
        ChannelSftp channel = null;
        try {
            logger.debug("Copying file from remote:remoteHost={}, remotePath={}, localPath={}",
                    host, remotePath, localPath);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CONNECT_TIMEOUT);
            channel.get(remotePath, localPath);
        }
        catch (Exception e) {
            throw new IOException("Failed to copy file from remote by sftp:remoteHost=" + host + ":"
                    + port + ", localFile=" + localPath + ", remotePath=" + remotePath, e);
        }
        finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public void scpFolderFrom(String localPath, String remotePath) throws IOException {
        ChannelSftp channel = null;
        try {
            logger.debug(
                    "Copying folder content from remote:remoteHost={}, remotePath={}, localPath={}",
                    host, remotePath, localPath);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CONNECT_TIMEOUT);

            channel.cd(remotePath);
            recursiveFolderDownload(localPath, remotePath, channel);
        }
        catch (Exception e) {
            throw new IOException("Failed to copy folder content from remote by sftp:remoteHost="
                    + host + ":" + port + ", localPath=" + localPath + ", remotePath=" + remotePath,
                    e);
        }
        finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void recursiveFolderDownload(String localPath, String remotePath, ChannelSftp channel)
            throws SftpException {
        Vector<ChannelSftp.LsEntry> fileAndFolderList = channel.ls(remotePath);

        for (ChannelSftp.LsEntry item : fileAndFolderList) {
            if (!item.getAttrs().isDir()) {
                channel.get(remotePath + CommonDefine.LINUX_PATH_SEPARATOR + item.getFilename(),
                        localPath + File.separator + item.getFilename());
            }
            else if (!".".equals(item.getFilename()) && !"..".equals(item.getFilename())) {
                new File(localPath + File.separator + item.getFilename()).mkdirs();
                recursiveFolderDownload(localPath + File.separator + item.getFilename(),
                        remotePath + CommonDefine.LINUX_PATH_SEPARATOR + item.getFilename(),
                        channel);
            }
        }
    }

    public void scpTo(String localPath, String remotePath) throws IOException {
        ChannelSftp channel = null;
        try {
            logger.debug("Copying file to remote:remoteHost={}, localFile={}, remotePath={}", host,
                    localPath, remotePath);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CONNECT_TIMEOUT);
            channel.put(localPath, remotePath);
        }
        catch (Exception e) {
            throw new IOException("Failed to copy file to remote by sftp:remoteHost=" + host + ":"
                    + port + ", localFile=" + localPath + ", remotePath=" + remotePath, e);
        }
        finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public ExecResult sudoExec(String command) throws IOException {
        return internalSudoExec(command);
    }

    public ExecResult exec(String command, Map<String, String> env) throws IOException {
        StringBuilder envCmdBuilder = new StringBuilder();
        if (env != null) {
            for (Map.Entry<String, String> entry : env.entrySet()) {
                envCmdBuilder.append("export ").append(entry.getKey()).append("=")
                        .append(entry.getValue()).append(" && ");
            }
        }
        return internalExec(envCmdBuilder.toString() + command, false);
    }

    private ExecResult internalSudoExec(String command) throws IOException {
        if (isRootUser) {
            return internalExec(command, false);
        }
        command = "sudo -S -p '' " + command;
        return internalExec(command, true);
    }

    private ExecResult internalExec(String command, boolean isNeedSendPasswd) throws IOException {
        Channel channel = null;
        try {
            channel = session.openChannel("exec");

            logger.debug("Executing command, remoteHost:{}, command:{}", host, command);
            ((ChannelExec) channel).setCommand(command);

            return runCommand(channel, isNeedSendPasswd);
        }
        catch (Exception e) {
            throw new IOException(
                    "Failed to exec cmd:remoteHost=" + host + ":" + port + ", command=" + command,
                    e);
        }
        finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public void changeOwner(String path, String changeToUser, String changeToGroup)
            throws IOException {
        String command;
        if (changeToGroup == null) {
            command = "chown -R " + changeToUser + " " + path;
        }
        else {
            command = "chown -R " + changeToUser + ":" + changeToGroup + " " + path;
        }
        ExecResult execResult = sudoExec(command);
        if (execResult.getExitCode() != 0) {
            throw new IOException("Failed to change dir owner, remoteHost:" + host + ", command:"
                    + command + ", exec result" + execResult);
        }
    }

    private ExecResult runCommand(Channel channel, boolean isNeedSendPasswd)
            throws IOException, JSchException {
        StringBuilder stdoutBf = new StringBuilder();
        StringBuilder stderrBf = new StringBuilder();
        if (isNeedSendPasswd) {
            ((ChannelExec) channel).setPty(true);
        }
        InputStream er = ((ChannelExec) channel).getErrStream();
        InputStream in = channel.getInputStream();
        OutputStream os = channel.getOutputStream();
        channel.connect(CONNECT_TIMEOUT);

        if (isNeedSendPasswd) {
            os.write((password + "\n").getBytes());
            os.flush();
        }

        byte[] tmp = new byte[4 * 1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) {
                    break;
                }
                stdoutBf.append(new String(tmp, 0, i));
            }
            while (er.available() > 0) {
                int i = er.read(tmp, 0, 1024);
                if (i < 0) {
                    break;
                }
                stderrBf.append(new String(tmp, 0, i));
            }

            if (channel.isClosed()) {
                if (in.available() > 0 || er.available() > 0) {
                    continue;
                }
                break;
            }

            try {
                Thread.sleep(200);
            }
            catch (InterruptedException e) {
                throw new IOException(
                        "Failed to wait for the execution to complete, cause by interrupt", e);
            }
        }

        String stderr = stderrBf.toString();
        String stdout = stdoutBf.toString();
        if (isNeedSendPasswd && stdout.length() >= password.length()
                && stdout.startsWith(password)) {
            // we have setTTY, the password that we send will occur in stdout,
            // remove it.
            stdout = stdout.substring(password.length()).trim();
        }

        return new ExecResult(channel.getExitStatus(), stdout, stderr);
    }

    @Override
    public void close() {
        if (sshMgr != null) {
            sshMgr.release(this);
            return;
        }

        disconnect();
    }

    void disconnect() {
        if (session != null) {
            try {
                session.disconnect();
            }
            catch (Exception e) {
                logger.warn("Failed to disconnect:remote={}:{}", host, port, e);
            }
        }
    }

}
