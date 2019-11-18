package com.sequoiacm.client.core;

import org.bson.BSONObject;

/**
 * SCM host info
 */
public class ScmHostInfo {

    private String hostName;

    private double cpuIdle;

    private double cpuSys;

    private double cpuUser;

    private double cpuOther;

    private long freeRam;

    private long totalRam;

    private long freeSwap;

    private long totalSwap;

    /**
     * Construct ScmHostInfo instance.
     *
     * @param obj
     *            bsonObject.
     */
    public ScmHostInfo(BSONObject obj) {

        Object temp = null;

        temp = obj.get("hostname");
        if (null != temp) {
            setHostName(temp.toString());
        }

        Object cpu = obj.get("cpu");
        if (null != cpu) {
            temp = ((BSONObject) cpu).get("idle");
            if (null != temp) {
                setCpuIdle(Double.parseDouble(temp.toString()));
            }

            temp = ((BSONObject) cpu).get("sys");
            if (null != temp) {
                setCpuSys(Double.parseDouble(temp.toString()));
            }

            temp = ((BSONObject) cpu).get("user");
            if (null != temp) {
                setCpuUser(Double.parseDouble(temp.toString()));
            }

            temp = ((BSONObject) cpu).get("other");
            if (null != temp) {
                setCpuOther(Double.parseDouble(temp.toString()));
            }
        }

        Object mem = obj.get("memory");
        if (null != mem) {

            temp = ((BSONObject) mem).get("free_ram");
            if (null != temp) {
                setFreeRam(Long.parseLong(temp.toString()));
            }

            temp = ((BSONObject) mem).get("total_ram");
            if (null != temp) {
                setTotalRam(Long.parseLong(temp.toString()));
            }

            temp = ((BSONObject) mem).get("free_swap");
            if (null != temp) {
                setFreeSwap(Long.parseLong(temp.toString()));
            }

            temp = ((BSONObject) mem).get("total_swap");
            if (null != temp) {
                setTotalSwap(Long.parseLong(temp.toString()));
            }
        }
    }

    /**
     * Get the host name.
     *
     * @return host name.
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Set the host name.
     *
     * @param hostName
     *            host name.
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Get the cpuIdle.
     *
     * @return cpuIdle.
     */
    public double getCpuIdle() {
        return cpuIdle;
    }

    /**
     * Set the cpuIdle.
     *
     * @param cpuIdle
     *            cpuIdle.
     */
    public void setCpuIdle(double cpuIdle) {
        this.cpuIdle = cpuIdle;
    }

    /**
     * Get the cpuSys.
     *
     * @return cpuSys.
     */
    public double getCpuSys() {
        return cpuSys;
    }

    /**
     * Set the cpuSys.
     *
     * @param cpuSys
     *            cpuSys.
     */
    public void setCpuSys(double cpuSys) {
        this.cpuSys = cpuSys;
    }

    /**
     * Get the cpuUser.
     *
     * @return cpuUser
     */
    public double getCpuUser() {
        return cpuUser;
    }

    /**
     * Set the cpuUser.
     *
     * @param cpuUser
     *            cpuUser
     */
    public void setCpuUser(double cpuUser) {
        this.cpuUser = cpuUser;
    }

    /**
     * Get the cpuOther.
     *
     * @return cpuOther
     */
    public double getCpuOther() {
        return cpuOther;
    }

    /**
     * Set the cpuOther.
     *
     * @param cpuOther
     *            cpuOther
     */
    public void setCpuOther(double cpuOther) {
        this.cpuOther = cpuOther;
    }

    /**
     * Get the freeRam.
     *
     * @return freeRam
     */
    public long getFreeRam() {
        return freeRam;
    }

    /**
     * Set the freeRam.
     *
     * @param freeRam
     *            freeRam
     */
    public void setFreeRam(long freeRam) {
        this.freeRam = freeRam;
    }

    /**
     * Get the totalRam.
     *
     * @return totalRam
     */
    public long getTotalRam() {
        return totalRam;
    }

    /**
     * Set totalRam.
     *
     * @param totalRam
     *            totalRam
     */
    public void setTotalRam(long totalRam) {
        this.totalRam = totalRam;
    }

    /**
     * Get freeSwap.
     *
     * @return freeSwap
     */
    public long getFreeSwap() {
        return freeSwap;
    }

    /**
     * Set freeSwap.
     *
     * @param freeSwap
     *            freeSwap
     */
    public void setFreeSwap(long freeSwap) {
        this.freeSwap = freeSwap;
    }

    /**
     * Get the totalSwap.
     *
     * @return totalSwap
     */
    public long getTotalSwap() {
        return totalSwap;
    }

    /**
     * Set totalSwap.
     *
     * @param totalSwap
     *            totalSwap
     */
    public void setTotalSwap(long totalSwap) {
        this.totalSwap = totalSwap;
    }

}
