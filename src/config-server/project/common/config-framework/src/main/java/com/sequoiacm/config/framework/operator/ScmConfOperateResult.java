package com.sequoiacm.config.framework.operator;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.infrastructure.config.core.msg.Config;

public class ScmConfOperateResult {
    private Config config;
    private List<ScmConfEvent> events = new ArrayList<>();

    public ScmConfOperateResult() {
    }

    public ScmConfOperateResult(Config config, ScmConfEvent e) {
        this.config = config;
        this.events.add(e);
    }

    public Config getConfig() {
        return config;
    }

    public List<ScmConfEvent> getEvent() {
        return events;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void addEvent(ScmConfEvent event) {
        events.add(event);
    }

}
