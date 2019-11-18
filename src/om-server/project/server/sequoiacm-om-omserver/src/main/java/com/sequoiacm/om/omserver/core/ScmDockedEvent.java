package com.sequoiacm.om.omserver.core;

import org.springframework.context.ApplicationEvent;

public class ScmDockedEvent extends ApplicationEvent {

    public ScmDockedEvent() {
        super(ScmDockedEvent.class);
    }

}
