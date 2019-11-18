package com.sequoiacm.deploy.config;

public class SshConfig {
    private int connectTimeout = 3 * 60 * 1000;
    private String privateKeyPath = "~/.ssh/id_rsa";
    private String envFile = "/etc/profile";

    public SshConfig() {
        SystemApplicationProperty p = SystemApplicationProperty.getInstance();
        connectTimeout = p.getInt("ssh.connectTimeout", connectTimeout);
        privateKeyPath = p.getString("ssh.privateKeyPath", privateKeyPath);
        envFile = p.getString("ssh.envFile", envFile);
    }

    public void setConnectTimeout(int sshConnectTimeout) {
        this.connectTimeout = sshConnectTimeout;
    }

    public String getPriKeyPath() {
        return privateKeyPath;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public String getEnvFile() {
        return envFile;
    }

    public void setEnvFile(String envFile) {
        this.envFile = envFile;
    }

}
