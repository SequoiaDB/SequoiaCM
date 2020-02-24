
package com.sequoiacm.deploy.ssh;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sequoiacm.deploy.config.SshConfig;

public class Ssh implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(Ssh.class);
    private Session session;
    private String host;
    private int port;
    private String username;
    private String password;
    private SshMgr sshMgr;
    private JSch jsch;
    private boolean isNeedMakeScpTmpDir;
    private String scpTmpPath;
    private SshConfig sshConfig;
    private boolean isRootUser;

    Ssh(SshMgr sshMgr, String host, int port, String username, String password, SshConfig sshConfig)
            throws IOException {
        this.jsch = new JSch();
        this.sshConfig = sshConfig;
        this.host = host;
        this.port = port;
        this.username = username;
        this.isRootUser = username.equals("root");
        this.isNeedMakeScpTmpDir = true;
        this.password = password;
        this.sshMgr = sshMgr;
        this.scpTmpPath = "/tmp/scm-deploy-tmp/";
        try {
            initSession(jsch);
        }
        catch (Exception e) {
            disconnect();
            if (password != null && password.length() != 0) {
                throw new IOException("failed to connect to " + host + ":" + port, e);
            }
            else {
                throw new IOException("failed to connect to " + host + ":" + port
                        + ", password is empty, use private key to connect:"
                        + sshConfig.getPriKeyPath(), e);
            }
        }
    }

    public String getHost() {
        return host;
    }

    private void initSession(JSch jsch) throws JSchException {
        if (password != null && password.length() != 0) {
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(sshConfig.getConnectTimeout());
        }
        else {
            jsch.addIdentity(sshConfig.getPriKeyPath());
            session = jsch.getSession(username, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(sshConfig.getConnectTimeout());
        }
    }

    public String searchEnv(String key) throws IOException {
        String command = "echo -e '#!/bin/bash \n . " + sshConfig.getEnvFile()
                + " > /dev/null \n echo $" + key
                + "' > ./searchEnv.sh && /bin/bash ./searchEnv.sh && rm ./searchEnv.sh";
        SshExecRes res = internalSudoExec(command);
        String envValue = res.getStdOut();
        if (envValue == null || envValue.trim().length() == 0) {
            throw new IllegalArgumentException("env " + key + " not define, host=" + host
                    + ", searchPath=" + sshConfig.getEnvFile());
        }

        return envValue.trim();
    }

    public void scp(String localPath, String remotePath) throws IOException {
        if (isNeedMakeScpTmpDir) {
            sudoExec("mkdir -p " + scpTmpPath);
            sudoExec("chown " + username + " " + scpTmpPath + " -R");
            isNeedMakeScpTmpDir = false;
        }
        if (remotePath.trim().startsWith(scpTmpPath)) {
            internalScp(localPath, remotePath);
            return;
        }

        String tmpFilePath = scpTmpPath + new File(localPath).getName();
        internalScp(localPath, tmpFilePath);
        sudoExec("mv " + tmpFilePath + " " + remotePath);
    }

    public String getScpTmpPath() {
        return scpTmpPath;
    }

    private void internalScp(String localPath, String remotePath) throws IOException {
        ChannelSftp channel = null;
        try {
            logger.debug("copying file to remote:remoteHost={}, localFile={}, remotePath={}", host,
                    localPath, remotePath);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(sshConfig.getConnectTimeout());
            channel.put(localPath, remotePath);
        }
        catch (Exception e) {
            throw new IOException("failed to scp:remoteHost=" + host + ":" + port + ", localFile="
                    + localPath + ", remotePath=" + remotePath, e);
        }
        finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public int sudoSuExec(String suToUser, String command, LinkedHashMap<String, String> env,
            Integer... expectExitCode) throws IOException {
        StringBuilder envCmdBuilder = new StringBuilder();
        if (env != null) {
            for (Entry<String, String> entry : env.entrySet()) {
                envCmdBuilder.append("export ").append(entry.getKey()).append("=")
                        .append(entry.getValue()).append(" && ");
            }
        }
        String cmd = "su " + suToUser + " -c '" + envCmdBuilder.toString() + command + "'";
        return sudoExec(cmd, expectExitCode);
    }

    public int sudoExec(String command, Integer... expectExitCode) throws IOException {
        return internalSudoExec(command, expectExitCode).getExitCode();
    }

    private SshExecRes internalSudoExec(String command, Integer... expectExitCode)
            throws IOException {
        if (isRootUser) {
            return internalExec(command, false, expectExitCode);
        }
        command = "sudo -S -p '' " + command;
        return internalExec(command, true, expectExitCode);
    }

    private SshExecRes internalExec(String command, boolean isNeedSendPasswd,
            Integer... expectExitCode) throws IOException {
        Channel channel = null;
        try {

            channel = session.openChannel("exec");

            logger.debug("executing command, remoteHost:{}, command:{}, expectedExitCode:{}", host,
                    command, Arrays.toString(expectExitCode));
            ((ChannelExec) channel).setCommand(command);

            if (expectExitCode == null || expectExitCode.length == 0) {
                return runCommand(channel, command, isNeedSendPasswd, Arrays.asList(0));
            }
            else {
                return runCommand(channel, command, isNeedSendPasswd,
                        Arrays.asList(expectExitCode));
            }
        }
        catch (Exception e) {
            throw new IOException(
                    "failed to exec cmd:remoteHost=" + host + ":" + port + ", command=" + command,
                    e);
        }
        finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public SshExecRes exec(String command, Integer... expectExitCode) throws IOException {
        return internalExec(command, false, expectExitCode);
    }

    private SshExecRes runCommand(Channel channel, String command, boolean isNeedSendPasswd,
            List<Integer> expectExitCode) throws IOException, JSchException {
        StringBuffer stdoutBf = new StringBuffer();
        StringBuffer stderrBf = new StringBuffer();
        if (isNeedSendPasswd) {
            ((ChannelExec) channel).setPty(true);
        }
        InputStream er = ((ChannelExec) channel).getErrStream();
        InputStream in = channel.getInputStream();
        OutputStream os = channel.getOutputStream();
        channel.connect(sshConfig.getConnectTimeout());

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
                        "failed to wait for the execution to complete, cause by interrupt", e);
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

        int exitStatus = channel.getExitStatus();
        if (!expectExitCode.contains(exitStatus)) {
            throw new IOException("failed to execute command, remoteHost:" + host + ", command:"
                    + command + ", stderror:" + stderr + ", stdout:" + stdout + ", exitCode:"
                    + exitStatus + ", expectExitCode:" + expectExitCode);
        }
        logger.debug("execute command success, remoteHost:" + host + ", command:" + command
                + ", stderror:" + stderr + ", stdout:" + stdout + ", exitCode=" + exitStatus
                + ", expectExitCode:" + expectExitCode);
        return new SshExecRes(exitStatus, stdout, stderr);
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
                logger.warn("failed to disconnect:remote={}:{}", host, port, e);
            }
        }
    }

}
