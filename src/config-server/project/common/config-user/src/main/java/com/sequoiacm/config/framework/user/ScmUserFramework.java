package com.sequoiacm.config.framework.user;

import com.sequoiacm.config.framework.user.operator.ScmUserConfOperator;
import org.springframework.beans.factory.annotation.Autowired;

import com.sequoiacm.config.framework.Framework;
import com.sequoiacm.config.framework.ScmConfFrameworkBase;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;

@Framework
public class ScmUserFramework extends ScmConfFrameworkBase {

    @Autowired
    private ScmUserConfOperator userOp;

    public ScmUserFramework() {
        super(ScmConfigNameDefine.USER);
    }

    @Override
    public ScmConfOperator getConfOperator() {
        return userOp;
    }

    @Override
    public String getConfigName() {
        return ScmConfigNameDefine.USER;
    }

}
