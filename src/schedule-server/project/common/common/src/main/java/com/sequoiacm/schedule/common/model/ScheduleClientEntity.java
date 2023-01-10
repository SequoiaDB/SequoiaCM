package com.sequoiacm.schedule.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

// 该类是封装响应给客户端的调度任务信息
public class ScheduleClientEntity extends ScheduleFullEntity {
    @JsonProperty("transition_name")
    private String transitionName;

    public ScheduleClientEntity(ScheduleFullEntity info) {
        super(info.getId(), info.name, info.desc, info.type, info.workspace, info.content,
                info.cron, info.enable, info.getCreate_user(), info.getCreate_time(),
                info.preferredRegion, info.preferredZone, info.transitionId);
    }

    public String getTransitionName() {
        return transitionName;
    }

    public void setTransitionName(String transitionName) {
        this.transitionName = transitionName;
    }
}
