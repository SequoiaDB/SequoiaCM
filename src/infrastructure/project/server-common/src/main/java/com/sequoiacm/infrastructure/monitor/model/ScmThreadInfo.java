package com.sequoiacm.infrastructure.monitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScmThreadInfo {

    @JsonProperty("all")
    private Integer all;

    @JsonProperty("runnable")
    private Integer runnable;

    @JsonProperty("waiting")
    private Integer waiting;

    public Integer getAll() {
        return all;
    }

    public void setAll(Integer all) {
        this.all = all;
    }

    public Integer getRunnable() {
        return runnable;
    }

    public void setRunnable(Integer runnable) {
        this.runnable = runnable;
    }

    public Integer getWaiting() {
        return waiting;
    }

    public void setWaiting(Integer waiting) {
        this.waiting = waiting;
    }

    @Override
    public String toString() {
        return "ScmThreadInfo{" + "all=" + all + ", runnable=" + runnable + ", waiting=" + waiting
                + '}';
    }
}
