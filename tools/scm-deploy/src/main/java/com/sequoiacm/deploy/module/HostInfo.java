package com.sequoiacm.deploy.module;

import org.bson.BSONObject;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

public class HostInfo {

    private String userName;
    private int port;
    private String password;
    private String hostName;
    private String javaHome;

    public static final ConfCoverter<HostInfo> CONVERTER = new ConfCoverter<HostInfo>() {
        @Override
        public HostInfo convert(BSONObject bson) {
            return new HostInfo(bson);
        }
    };

    public HostInfo(String userName, int port, String password, String hostName, String javaHome) {
        this.userName = userName;
        this.port = port;
        this.password = password;
        this.hostName = hostName;
        this.javaHome = javaHome;
    }

    public HostInfo(BSONObject bson) {
        userName = BsonUtils.getStringChecked(bson, ConfFileDefine.HOST_USER);
        password = BsonUtils.getStringOrElse(bson, ConfFileDefine.HOST_PASSWORD, "");
        port = Integer.valueOf(BsonUtils.getStringOrElse(bson, ConfFileDefine.HOST_SSH_PORT, "22"));
        hostName = BsonUtils.getStringChecked(bson, ConfFileDefine.HOST_HOSTNAME);
        javaHome = BsonUtils.getString(bson, ConfFileDefine.HOST_JAVA_NOME);
    }

    public String getJavaHome() {
        return javaHome;
    }

    public String getHostName() {
        return hostName;
    }

    public String getUserName() {
        return userName;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + port;
        return result;
    }

    public void resetJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HostInfo other = (HostInfo) obj;
        if (hostName == null) {
            if (other.hostName != null)
                return false;
        }
        else if (!hostName.equals(other.hostName))
            return false;
        if (port != other.port)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "HostInfo [userName=" + userName + ", port=" + port + ", hostName=" + hostName
                + ", javaHome=" + javaHome + "]";
    }
}
