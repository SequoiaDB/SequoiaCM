package com.sequoiacm.client.element.lifecycle;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;

public class ScmWorkspaceLifeCycleConfig {
    private String workspaceName;

    private List<ScmLifeCycleTransition> transitionConfig;

    public ScmWorkspaceLifeCycleConfig(){

    }
    public ScmWorkspaceLifeCycleConfig(BSONObject obj) throws ScmException {
        Object temp = null;

        BSONObject workspaceLifeCycleConfig = BsonUtils.getBSONObjectChecked(obj,
                "WorkspaceLifeCycleConfig");
        BSONObject transitionConfiguration = BsonUtils.getBSONObjectChecked(workspaceLifeCycleConfig,
                "TransitionConfiguration");

        temp = workspaceLifeCycleConfig.get("Name");
        if (null != temp){
            setWorkspaceName((String) temp);
        }

        temp = transitionConfiguration.get("Transition");
        if (null != temp) {
            transitionConfig = new ArrayList<ScmLifeCycleTransition>();
            if (temp instanceof BasicBSONObject) {
                transitionConfig.add(new ScmLifeCycleTransition((BSONObject) temp));
            }
            else if (temp instanceof BasicBSONList) {
                BasicBSONList l = (BasicBSONList) temp;
                for (Object o : l) {
                    transitionConfig.add(new ScmLifeCycleTransition((BSONObject) o));
                }
            }
            else {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "can not analysis TransitionConfiguration");
            }
        }
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public List<ScmLifeCycleTransition> getTransitionConfig() {
        return transitionConfig;
    }

    public void setTransitionConfig(List<ScmLifeCycleTransition> transitionConfig) {
        this.transitionConfig = transitionConfig;
    }
}
