package com.sequoiacm.om.omserver.module.lifecycle;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.core.ScmTransitionSchedule;
import com.sequoiacm.om.omserver.module.OmScheduleBasicInfo;

public class OmTransitionSchedule {

    @JsonProperty("id")
    private String id;

    @JsonProperty("workspace")
    private String workspace;

    @JsonProperty("create_user")
    private String createUser;

    @JsonProperty("update_user")
    private String updateUser;

    @JsonProperty("create_time")
    private long createTime;

    @JsonProperty("update_time")
    private long updateTime;

    @JsonProperty("is_customized")
    private boolean isCustomized;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("preferred_region")
    private String preferredRegion;

    @JsonProperty("preferred_zone")
    private String preferredZone;

    @JsonProperty("schedules")
    private List<OmScheduleBasicInfo> schedules;

    @JsonProperty("transition")
    private OmTransitionBasic omTransitionBasic;

    public OmTransitionSchedule(ScmTransitionSchedule transitionSchedule,
            List<OmScheduleBasicInfo> schedules) {
        this.id = transitionSchedule.getId();
        this.workspace = transitionSchedule.getWorkspace();
        this.createTime = transitionSchedule.getCreateTime();
        this.createUser = transitionSchedule.getCreateUser();
        this.updateTime = transitionSchedule.getUpdateTime();
        this.updateUser = transitionSchedule.getUpdateUser();
        this.isCustomized = transitionSchedule.isCustomized();
        this.enabled = transitionSchedule.isEnable();
        this.preferredRegion = transitionSchedule.getPreferredRegion();
        this.preferredRegion = transitionSchedule.getPreferredZone();
        this.omTransitionBasic = new OmTransitionBasic(transitionSchedule.getTransition());
        this.schedules = schedules;
    }

    public void setSchedules(List<OmScheduleBasicInfo> schedules) {
        this.schedules = schedules;
    }
}
