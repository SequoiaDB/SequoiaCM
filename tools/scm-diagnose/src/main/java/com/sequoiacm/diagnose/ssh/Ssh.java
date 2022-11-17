package com.sequoiacm.diagnose.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.sequoiacm.diagnose.common.ExecRes;
import com.sequoiacm.diagnose.common.Services;
import com.sequoiacm.diagnose.config.SshCommonConfig;
import com.sequoiacm.diagnose.execption.LogCollectException;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

public class Ssh {
    private Session session;
    private String host;
    private String user;
    private String password;
    private int port = 22;
    private static final Logger logger = LoggerFactory.getLogger(Ssh.class);

    public Ssh(String host, int port, String username, String password) throws ScmToolsException {
        this.host = host;
        this.port = port;
        this.user = username;
        this.password = password;
        try {
            initSession();
            logger.info("ssh connect successfully host=" + host + ", user='" + user
                    + ", password= ****** " + ", port=" + port);
        }
        catch (JSchException e) {
            disconnect();
            throw new ScmToolsException("Failed to ssh connect to " + host + ":" + port,
                    LogCollectException.SSH_CONNECT_FAILED, e);
        }
    }

    public String getHost() {
        return host;
    }

    private void initSession() throws JSchException {
        JSch jsch = new JSch();
        if (password != null && password.length() != 0) {
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
        }
        else {
            try {
                jsch.addIdentity(SshCommonConfig.getPrivateKeyPath());
            }
            catch (JSchException e) {
                logger.error(host + " ssh password free login failed");
                throw new RuntimeException(e);
            }
            session = jsch.getSession(user, host, port);
        }
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(SshCommonConfig.getConnectTimeout());
    }

    public void disconnect() {
        if (session != null) {
            try {
                session.disconnect();
            }
            catch (Exception e) {
                logger.warn("Failed to disconnect:remote={}:{}", host, port, e);
            }
        }
    }

    public void checkExistDir(String path) throws ScmToolsException {
        ChannelSftp channelSftp = null;
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(SshCommonConfig.getConnectTimeout());
            channelSftp.stat(path);
        }
        catch (JSchException e) {
            throw new ScmToolsException("channelSftp openChannel failed",
                    LogCollectException.SSH_CONNECT_FAILED, e);
        }
        catch (SftpException e) {
            throw new ScmToolsException("remote host " + host + " not this dir:" + path,
                    LogCollectException.FILE_NOT_FIND, e);
        }
        finally {
            if (channelSftp != null && !channelSftp.isClosed()) {
                channelSftp.disconnect();
            }
        }
    }

    private ExecRes sshExecuteCommand(String command) throws ScmToolsException {
        Channel channel = null;
        try {
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            return runCommand(channel, command, Arrays.asList(0));
        }
        catch (Exception e) {
            throw new ScmToolsException(
                    "failed to exec cmd:remoteHost=" + host + ":" + port + ", command=" + command,
                    LogCollectException.SHELL_EXEC_ERROR, e);
        }
        finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private ExecRes runCommand(Channel channel, String command, List<Integer> expectExitCode)
            throws JSchException, IOException {
        StringBuffer stdoutBf = new StringBuffer();
        StringBuffer stderrBf = new StringBuffer();
        InputStream er = ((ChannelExec) channel).getErrStream();
        InputStream in = channel.getInputStream();
        OutputStream os = channel.getOutputStream();
        channel.connect(SshCommonConfig.getConnectTimeout());

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
        int exitStatus = channel.getExitStatus();
        if (!expectExitCode.contains(exitStatus)) {
            throw new IOException("failed to execute command, remoteHost:" + host + ", command:"
                    + command + ", stderror:" + stderr + ", stdout:" + stdout + ", exitCode:"
                    + exitStatus + ", expectExitCode:" + expectExitCode);
        }
        logger.debug("execute command success, remoteHost:" + host + ", command:" + command
                + ", stderror:" + stderr + ", stdout:" + stdout + ", exitCode=" + exitStatus
                + ", expectExitCode:" + expectExitCode);
        return new ExecRes(exitStatus, stdout, stderr);
    }

    public List<String> lsCopyLogFile(String serviceName, String path, int maxLogCount)
            throws ScmToolsException {
        Vector vector = null;
        ChannelSftp channelSftp = null;
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(SshCommonConfig.getConnectTimeout());
            vector = channelSftp.ls(path);
        }
        catch (JSchException e) {
            throw new ScmToolsException("channelSftp openChannel failed",
                    LogCollectException.SSH_CONNECT_FAILED, e);
        }
        catch (SftpException e) {
            throw new ScmToolsException("ssh ls " + path + " failed",
                    LogCollectException.FILE_NOT_FIND, e);
        }
        finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
        }
        Collections.sort(vector, new Comparator<ChannelSftp.LsEntry>() {
            @Override
            public int compare(ChannelSftp.LsEntry o1, ChannelSftp.LsEntry o2) {
                return o2.getAttrs().getMTime() - o1.getAttrs().getMTime();
            }
        });
        Services service = Services.getServices(serviceName);
        int logCount = 0;
        ArrayList<String> logList = new ArrayList<>();
        for (int i = 0; i < vector.size(); i++) {
            String[] lsMsg = vector.get(i).toString().split(" ");
            String logFileName = lsMsg[lsMsg.length - 1];

            // Daemon will get all log files
            if (Services.Daemon.getServiceName().equals(serviceName)) {
                logList.add(logFileName);
                continue;
            }
            if (Pattern.matches("^error.*out$", logFileName)
                    || Pattern.matches(".*syserror.*", logFileName)) {
                logList.add(logFileName);
                continue;
            }
            if (Pattern.matches(service.getMatch(), logFileName)) {
                if (logCount == maxLogCount) {
                    continue;
                }
                logList.add(logFileName);
                logCount++;
            }
        }
        return logList;
    }

    public void mkdir(String path) throws ScmToolsException {
        logger.info("remote hosts " + host + " create path " + path);
        String command = "mkdir -p " + path;
        ExecRes execRes = sshExecuteCommand(command);
        if (execRes.getExitCode() != 0) {
            throw new ScmToolsException(execRes.getStdErr(), LogCollectException.SHELL_EXEC_ERROR);
        }
    }

    public void zipFile(String tarName, String tarPath, List<String> logFile)
            throws ScmToolsException {
        StringBuilder builder = new StringBuilder();
        for (String logName : logFile) {
            builder.append(" " + logName);
        }
        String command = "tar zcvf " + tarName + " -C " + tarPath + builder;
        logger.info("remote hosts " + host + " zip file, command=" + command);
        ExecRes execRes = sshExecuteCommand(command);
        if (execRes.getExitCode() != 0) {
            throw new ScmToolsException(execRes.getStdErr(), LogCollectException.SHELL_EXEC_ERROR);
        }
    }

    public List<String> lsFile(String path) throws ScmToolsException {
        Vector vector = null;
        ChannelSftp channelSftp = null;
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(SshCommonConfig.getConnectTimeout());
            vector = channelSftp.ls(path);
        }
        catch (JSchException e) {
            System.out.println("channelSftp openChannel failed");
            throw new ScmToolsException("channelSftp openChannel failed",
                    LogCollectException.SSH_CONNECT_FAILED, e);
        }
        catch (SftpException e) {
            throw new ScmToolsException("ssh ls " + path + " failed",
                    LogCollectException.FILE_NOT_FIND, e);
        }
        finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
        }
        ArrayList<String> lsFile = new ArrayList<>();
        for (int i = 0; i < vector.size(); i++) {
            String[] lsMsg = vector.get(i).toString().split(" ");
            String fileName = lsMsg[lsMsg.length - 1];
            if (!fileName.equals(".") && !fileName.equals("..")) {
                lsFile.add(fileName);
            }
        }
        return lsFile;
    }

    public void copyFileFromRemote(String src, String dst) throws ScmToolsException {
        logger.info("copy " + src + " file from host " + host + " to local " + dst);
        ChannelSftp channelSftp = null;
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(SshCommonConfig.getConnectTimeout());
            channelSftp.get(src, dst);
        }
        catch (JSchException e) {
            throw new ScmToolsException("channelSftp openChannel failed",
                    LogCollectException.SSH_CONNECT_FAILED, e);
        }
        catch (SftpException e) {
            throw new ScmToolsException("copy " + src + " from " + host + " to local failed",
                    LogCollectException.COPY_FILE_FAILED, e);
        }
        finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
        }
    }

    public void rmDir(String path) throws ScmToolsException {
        logger.info("remote host " + host + " remove filePath " + path);
        String command = "rm -rf " + path;
        ExecRes execRes = sshExecuteCommand(command);
        if (execRes.getExitCode() != 0) {
            throw new ScmToolsException(execRes.getStdErr(), LogCollectException.SHELL_EXEC_ERROR);
        }
    }
}
