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

    public static ScmWorkspaceLifeCycleConfig fromUser(BSONObject obj) throws ScmException {
        ScmWorkspaceLifeCycleConfig config = new ScmWorkspaceLifeCycleConfig();
        Object temp = null;

        BSONObject workspaceLifeCycleConfig = BsonUtils.getBSONObjectChecked(obj,
                "WorkspaceLifeCycleConfig");
        BSONObject transitionConfiguration = BsonUtils.getBSONObjectChecked(workspaceLifeCycleConfig,
                "TransitionConfiguration");

        temp = workspaceLifeCycleConfig.get("Name");
        if (null != temp){
            config.workspaceName = ((String) temp);
        }

        temp = transitionConfiguration.get("Transition");
        if (null != temp) {
            List<ScmLifeCycleTransition> transitionConfig = new ArrayList<ScmLifeCycleTransition>();
            if (temp instanceof BasicBSONObject) {
                transitionConfig.add(ScmLifeCycleTransition.fromUser((BSONObject) temp));
            }
            else if (temp instanceof BasicBSONList) {
                BasicBSONList l = (BasicBSONList) temp;
                for (Object o : l) {
                    transitionConfig.add(ScmLifeCycleTransition.fromUser((BSONObject) o));
                }
            }
            else {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "can not analysis TransitionConfiguration");
            }
            config.transitionConfig = transitionConfig;
        }

        return config;
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
