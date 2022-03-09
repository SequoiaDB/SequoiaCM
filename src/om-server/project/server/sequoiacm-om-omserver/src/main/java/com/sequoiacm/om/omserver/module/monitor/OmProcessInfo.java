package com.sequoiacm.om.omserver.module.monitor;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmProcessInfo {

    @JsonProperty("pid")
    private String pid;

    @JsonProperty("uptime")
    private Long uptime;

    @JsonProperty("process_cpu_usage")
    private Double processCpuUsage;

    @JsonProperty("system_cpu_usage")
    private Double systemCpuUsage;

    @JsonProperty("cpus")
    private Integer cpus;

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Long getUptime() {
        return uptime;
    }

    public void setUptime(Long uptime) {
        this.uptime = uptime;
    }

    public Double getProcessCpuUsage() {
        return processCpuUsage;
    }

    public void setProcessCpuUsage(Double processCpuUsage) {
        this.processCpuUsage = processCpuUsage;
    }

    public Double getSystemCpuUsage() {
        return systemCpuUsage;
    }

    public void setSystemCpuUsage(Double systemCpuUsage) {
        this.systemCpuUsage = systemCpuUsage;
    }

    public Integer getCpus() {
        return cpus;
    }

    public void setCpus(Integer cpus) {
        this.cpus = cpus;
    }
}
