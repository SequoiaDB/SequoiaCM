package com.sequoiacm.config.framework.operator;

import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.infrastructure.config.core.msg.Config;

public class ScmConfOperateResult {
    private Config config;
    private ScmConfEvent event;

    public ScmConfOperateResult() {

    }

    public ScmConfOperateResult(Config config, ScmConfEvent e) {
        this.config = config;
        this.event = e;
    }

    public Config getConfig() {
        return config;
    }

    public ScmConfEvent getEvent() {
        return event;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void setEvent(ScmConfEvent event) {
        this.event = event;
    }


}
