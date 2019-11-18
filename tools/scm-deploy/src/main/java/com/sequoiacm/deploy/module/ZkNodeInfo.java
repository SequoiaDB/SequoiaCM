package com.sequoiacm.deploy.module;

import org.bson.BSONObject;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

public class ZkNodeInfo extends NodeInfo {
    private int serverPort1;
    private int serverPort2;

    public static final ConfCoverter<NodeInfo> CONVERTER = new ConfCoverter<NodeInfo>() {
        @Override
        public NodeInfo convert(BSONObject bson) {
            return new ZkNodeInfo(bson);
        }
    };

    public ZkNodeInfo(BSONObject bson) {
        setZone(null);
        setServiceType(ServiceType.ZOOKEEPER);
        setHostName(BsonUtils.getStringChecked(bson, ConfFileDefine.NODE_HOSTR_NAME));
        setPort(Integer
                .valueOf(BsonUtils.getStringChecked(bson, ConfFileDefine.ZOOKEEPER_CLIENT_PORT)));
        setCustomNodeConf(null);
        String serverPortStr = BsonUtils.getString(bson, ConfFileDefine.ZOOKEEPER_SERVER_PORT);

        String[] serverPortArray = serverPortStr.split(":");
        if (serverPortArray.length != 2) {
            throw new IllegalArgumentException("syntax error, server port:" + serverPortStr);
        }
        serverPort1 = Integer.valueOf(serverPortArray[0]);
        serverPort2 = Integer.valueOf(serverPortArray[1]);
    }

    public int getServerPort1() {
        return serverPort1;
    }

    public int getServerPort2() {
        return serverPort2;
    }

    @Override
    public String toString() {
        return "ZkNodeInfo [serverPort1=" + serverPort1 + ", serverPort2=" + serverPort2
                + ", getHostName()=" + getHostName() + ", getPort()=" + getPort()
                + ", getServiceType()=" + getServiceType() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + serverPort1;
        result = prime * result + serverPort2;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ZkNodeInfo other = (ZkNodeInfo) obj;

        if (serverPort1 != other.serverPort1)
            return false;
        if (serverPort2 != other.serverPort2)
            return false;
        return true;
    }

}
