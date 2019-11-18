package com.sequoiacm.config.framework.workspace;

import org.springframework.beans.factory.annotation.Autowired;

import com.sequoiacm.config.framework.Framework;
import com.sequoiacm.config.framework.ScmConfFrameworkBase;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.config.framework.workspace.operator.ScmWorkspaceConfOperator;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;

@Framework
public class ScmWorkspaceFramework extends ScmConfFrameworkBase {

    @Autowired
    private ScmWorkspaceConfOperator wsOp;

    public ScmWorkspaceFramework() {
        super(ScmConfigNameDefine.WORKSPACE);
    }

    @Override
    public ScmConfOperator getConfOperator() {
        return wsOp;
    }

    @Override
    public String getConfigName() {
        return ScmConfigNameDefine.WORKSPACE;
    }

}
