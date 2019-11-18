package com.sequoiacm.client.core;

import org.bson.BSONObject;

/**
 * Scm health class.
 */
public class ScmHealth {

    private String serviceName;

    private String nodeName;

    private String status;

    /**
     * Create a instance of ScmHealth.
     *
     * @param obj
     *            a bson containing basic information about ScmHealth.
     */
    public ScmHealth(BSONObject obj) {
        Object temp = null;

        temp = obj.get("service_name");
        if (null != temp) {
            setServiceName(temp.toString());
        }

        temp = obj.get("node_name");
        if (null != temp) {
            setNodeName(temp.toString());
        }

        temp = obj.get("status");
        if (null != temp) {
            setStatus(temp.toString());
        }
    }

    /**
     * Gets the service name.
     *
     * @return service name.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the service name.
     *
     * @param serviceName
     *            service name.
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Gets the node name.
     *
     * @return node name.
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * Sets the node name.
     *
     * @param nodeName
     *            node name.
     */
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * Gets the node status.
     *
     * @return node status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the node status.
     *
     * @param status node status.
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
