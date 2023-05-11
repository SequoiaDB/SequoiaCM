package com.sequoiacm.tools.element;

import java.util.Objects;

public class ScmHostInfo {

    private String host;
    private int port;

    private String username;
    private String password;

    public ScmHostInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public ScmHostInfo(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScmHostInfo hostInfo = (ScmHostInfo) o;
        return host.equals(hostInfo.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host);
    }

    @Override
    public String toString() {
        return "ScmHostInfo{" + "host='" + host + '\'' + ", port=" + port + ", user='" + username
                + '\'' + ", password='" + password + '\'' + '}';
    }
}
