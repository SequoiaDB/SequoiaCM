package com.sequoiacm.test.module;

import com.sequoiacm.test.common.CommonDefine;
import com.sequoiacm.test.common.StringUtil;

import java.util.List;
import java.util.Objects;

public class HostInfo {

    private String hostname;
    private int port;
    private String user;
    private String password;
    private String javaHome;

    public HostInfo() {

    }

    public HostInfo(String host) {
        if (Objects.equals(CommonDefine.LOCALHOST, host)) {
            this.hostname = host;
        }
        else {
            List<String> params = StringUtil.string2List(host, ":");
            try {
                this.user = params.get(0);
                this.password = params.get(1);
                this.hostname = params.get(2);
                this.port = Integer.parseInt(params.get(3));
            }
            catch (Exception e) {
                throw new IllegalArgumentException("the host format is illegal:" + host);
            }
        }
    }

    public boolean isLocalHost() {
        return Objects.equals(CommonDefine.LOCALHOST, hostname);
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getJavaHome() {
        return javaHome;
    }

    public void resetJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, password, hostname, port);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        HostInfo that = (HostInfo) obj;
        return Objects.equals(user, that.user) && Objects.equals(password, that.password)
                && Objects.equals(hostname, that.hostname) && Objects.equals(port, that.port);
    }

    @Override
    public String toString() {
        return "HostInfo{" + "hostname='" + hostname + '\'' + ", port=" + port + ", user='" + user
                + '\'' + ", password='" + password + '\'' + '}';
    }
}
