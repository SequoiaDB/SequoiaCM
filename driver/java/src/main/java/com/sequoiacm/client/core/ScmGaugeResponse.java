package com.sequoiacm.client.core;

import org.bson.BSONObject;

/**
 * SCM gauge response.
 */
public class ScmGaugeResponse {

    private String nodeName;

    private String serviceName;

    private long responseTime;

    private long count;

    /**
     * Construct a ScmGaugeResponse instance
     *
     * @param obj
     *            bsonObject
     */
    public ScmGaugeResponse(BSONObject obj) {

        Object temp = null;

        temp = obj.get("service_name");
        if (null != temp) {
            setServiceName(temp.toString());
        }

        temp = obj.get("node_name");
        if (null != temp) {
            setNodeName(temp.toString());
        }

        temp = obj.get("response_time");
        if (null != temp) {
            setResponseTime(Long.parseLong(temp.toString()));
        }

        temp = obj.get("response_count");
        if (null != temp) {
            setCount(Long.parseLong(temp.toString()));
        }
    }

    /**
     * Get the node name of the gauge response.
     *
     * @return node name.
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * set the node name of the gauge response.
     *
     * @param nodeName
     *            node name.
     */
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * Get the service name of the gauge response.
     *
     * @return node name.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Set the service name of the gauge response.
     *
     * @param serviceName
     *            service name.
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Get the response time of gauge response.
     *
     * @return time.
     */
    public long getResponseTime() {
        return responseTime;
    }

    /**
     * Set the response time of gauge response.
     *
     * @param responseTime
     *            time.
     */
    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    /**
     * Get the count of the gauge response.
     *
     * @return count.
     */
    public long getCount() {
        return count;
    }

    /**
     * Set the count of gauge response.
     *
     * @param count
     *            count.
     */
    public void setCount(long count) {
        this.count = count;
    }

}
