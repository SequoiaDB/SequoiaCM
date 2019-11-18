package com.sequoiacm.client.element;

import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;

/**
 * Scm node info.
 */
public class ScmNodeInfo {
    private int nodeId;
    private String name;
    private int port;
    private String hostName;
    private int siteId;
    private int type;

    /**
     * Create a instance of scm node info.
     *
     * @param obj
     *            a bson containing information about scm node.
     */
    public ScmNodeInfo(BSONObject obj) {
        Object tmp = obj.get(FieldName.FIELD_CLCONTENTSERVER_ID);
        if (tmp != null) {
            nodeId = (Integer) tmp;
        }

        tmp = obj.get(FieldName.FIELD_CLCONTENTSERVER_NAME);
        if (tmp != null) {
            name = (String) tmp;
        }

        tmp = obj.get(FieldName.FIELD_CLCONTENTSERVER_PORT);
        if (tmp != null) {
            port = (Integer) tmp;
        }

        tmp = obj.get(FieldName.FIELD_CLCONTENTSERVER_HOST_NAME);
        if (tmp != null) {
            hostName = (String) tmp;
        }

        tmp = obj.get(FieldName.FIELD_CLCONTENTSERVER_SITEID);
        if (tmp != null) {
            siteId = (Integer) tmp;
        }

        tmp = obj.get(FieldName.FIELD_CLCONTENTSERVER_TYPE);
        if (tmp != null) {
            type = (Integer) tmp;
        }
    }

    /**
     * Gets the node id.
     *
     * @return node id.
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Gets the node name.
     *
     * @return node name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the node port.
     *
     * @return node port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the host name.
     *
     * @return host name.
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Gets the site id.
     *
     * @return site id.
     */
    public int getSiteId() {
        return siteId;
    }

    /**
     * Gets the node type.
     *
     * @return node type.
     */
    public int getType() {
        return type;
    }

    @Override
    public String toString() {
        return "id=" + nodeId + ",name=" + name + ",siteId=" + siteId + ",type=" + type
                + ",hostName=" + hostName + ",port=" + port;
    }

}
