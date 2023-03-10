package com.sequoiacm.test.module;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;

import com.sequoiacm.test.common.CommonDefine;
import com.sequoiacm.test.common.StringUtil;

public class HostInfo {

    private String hostname;
    private int port;
    private String user;
    private String password;
    private String javaHome;
    private boolean isLocalHost;

    public HostInfo() {

    }

    public HostInfo(String host) {
        if (Objects.equals(CommonDefine.LOCALHOST, host)) {
            this.hostname = host;
            this.isLocalHost = true;
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

            String localHostname, localHostAddress;
            try {
                InetAddress ia = InetAddress.getLocalHost();
                localHostname = ia.getHostName();
                localHostAddress = ia.getHostAddress();
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to get local hostname", e);
            }
            if (hostname.equals(localHostname) || hostname.equals(localHostAddress)) {
                this.isLocalHost = true;
            }
        }
    }

    public boolean isLocalHost() {
        return isLocalHost;
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
        return Objects.hash(hostname);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        HostInfo that = (HostInfo) obj;
        return Objects.equals(hostname, that.hostname);
    }

    @Override
    public String toString() {
        return "HostInfo{" + "hostname='" + hostname + '\'' + '}';
    }
}
