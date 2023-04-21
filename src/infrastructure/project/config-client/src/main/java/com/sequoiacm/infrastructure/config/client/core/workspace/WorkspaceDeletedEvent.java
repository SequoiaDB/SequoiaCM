package com.sequoiacm.infrastructure.config.client.core.workspace;

import org.springframework.context.ApplicationEvent;

public class WorkspaceDeletedEvent extends ApplicationEvent {

    public WorkspaceDeletedEvent(String deletedWorkspace) {
        super(deletedWorkspace);
    }

    public String getDeletedWorkspace(){
        return (String) getSource();
    }
}
