
package com.sequoiacm.tools.common;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class Ssh implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(Ssh.class);
    private static final int connectTimeout = 3 * 60 * 1000;
    private static final String privateKeyPath = "~/.ssh/id_rsa";
    private Session session;
    private String host;
    private int port;
    private String username;
    private String password;
    private JSch jsch;

    public Ssh(String host, int port, String username, String password) throws IOException {
        this.jsch = new JSch();
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
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
                        + ", password is empty, use private key to connect:" + privateKeyPath, e);
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
        }
        else {
            jsch.addIdentity(privateKeyPath);
            session = jsch.getSession(username, host, port);
        }
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(connectTimeout);
    }

    public boolean isSftpAvailable() {
        ChannelSftp channel = null;
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(connectTimeout);
        }
        catch (JSchException e) {
            return false;
        }
        finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
        return true;
    }

    public void scp(InputStream data, String remotePath) throws IOException {
        ChannelSftp channel = null;
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(connectTimeout);
            channel.put(data, remotePath);
        }
        catch (Exception e) {
            throw new IOException("failed to send data to remote by sftp: remoteHost=" + host + ":"
                    + port + ", remotePath=" + remotePath + ", error=" + e.getMessage(), e);
        }
        finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public boolean isFileExists(String path) throws IOException {
        ChannelSftp channel = null;
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(connectTimeout);
            channel.lstat(path);
            return true;
        }
        catch (Exception e) {
            if (e instanceof SftpException
                    && ((SftpException) e).id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            }
            throw new IOException("failed to check file status: remoteHost=" + host + ":" + port
                    + "remotePath=" + path, e);
        }
        finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public void close() {
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
