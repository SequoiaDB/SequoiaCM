package com.sequoiacm.om.omserver.module.lifecycle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;

import java.util.List;

public class OmTransitionWithWs extends OmTransitionBasic {

    @JsonProperty("workspaces")
    private List<String> workspaces;

    @JsonProperty("workspaces_customized")
    private List<String> workspacesCustomized;

    public OmTransitionWithWs() {

    }

    public OmTransitionWithWs(ScmLifeCycleTransition transition) {
        super(transition);
    }

    public OmTransitionWithWs(ScmLifeCycleTransition transition, List<String> workspaces,
            List<String> workspacesCustomized) {
        this(transition);
        this.workspaces = workspaces;
        this.workspacesCustomized = workspacesCustomized;
    }
}
