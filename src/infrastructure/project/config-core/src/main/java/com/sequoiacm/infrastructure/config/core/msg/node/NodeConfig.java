package com.sequoiacm.infrastructure.config.core.msg.node;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;

public class NodeConfig implements Config {
    private int id;
    private String name;
    private int type;
    private int siteId;
    private String hostName; 
    private int port;
    
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public int getSiteId() {
        return siteId;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public BSONObject toBSONObject() {
        BSONObject nodeConfigObj = new BasicBSONObject();
        nodeConfigObj.put(FieldName.FIELD_CLCONTENTSERVER_ID, id);
        nodeConfigObj.put(FieldName.FIELD_CLCONTENTSERVER_NAME, name);
        nodeConfigObj.put(FieldName.FIELD_CLCONTENTSERVER_TYPE, type);
        nodeConfigObj.put(FieldName.FIELD_CLCONTENTSERVER_SITEID, siteId);
        nodeConfigObj.put(FieldName.FIELD_CLCONTENTSERVER_HOST_NAME, hostName);
        nodeConfigObj.put(FieldName.FIELD_CLCONTENTSERVER_PORT, port);
        return nodeConfigObj;
    }
}
