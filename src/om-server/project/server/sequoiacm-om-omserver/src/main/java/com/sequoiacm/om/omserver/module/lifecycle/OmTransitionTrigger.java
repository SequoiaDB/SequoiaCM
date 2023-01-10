package com.sequoiacm.om.omserver.module.lifecycle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.element.lifecycle.ScmTrigger;

public class OmTransitionTrigger {

    @JsonProperty("id")
    private String id;

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("create_time")
    private String createTime;

    @JsonProperty("last_access_time")
    private String lastAccessTime;

    @JsonProperty("build_time")
    private String buildTime;

    @JsonProperty("transition_time")
    private String transitionTime;

    public OmTransitionTrigger() {

    }

    public OmTransitionTrigger(ScmTrigger scmTrigger) {
        this.id = scmTrigger.getID();
        this.mode = scmTrigger.getMode();
        this.createTime = scmTrigger.getCreateTime();
        this.lastAccessTime = scmTrigger.getLastAccessTime();
        this.buildTime = scmTrigger.getBuildTime();
        this.transitionTime = scmTrigger.getTransitionTime();
    }

    public ScmTrigger transformToScmTrigger() {
        ScmTrigger scmTrigger = new ScmTrigger();
        scmTrigger.setID(id);
        scmTrigger.setMode(mode);
        scmTrigger.setCreateTime(createTime);
        scmTrigger.setLastAccessTime(lastAccessTime);
        scmTrigger.setBuildTime(buildTime);
        scmTrigger.setTransitionTime(transitionTime);
        return scmTrigger;
    }

    public String getId() {
        return id;
    }

    public String getMode() {
        return mode;
    }

    public String getCreateTime() {
        return createTime;
    }

    public String getLastAccessTime() {
        return lastAccessTime;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public String getTransitionTime() {
        return transitionTime;
    }
}
