package com.sequoiacm.config.framework.role;

import com.sequoiacm.config.framework.role.operator.ScmRoleConfOperator;
import org.springframework.beans.factory.annotation.Autowired;

import com.sequoiacm.config.framework.Framework;
import com.sequoiacm.config.framework.ScmConfFrameworkBase;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;

@Framework
public class ScmRoleFramework extends ScmConfFrameworkBase {

    @Autowired
    private ScmRoleConfOperator roleOp;

    public ScmRoleFramework() {
        super(ScmConfigNameDefine.ROLE);
    }

    @Override
    public ScmConfOperator getConfOperator() {
        return roleOp;
    }

    @Override
    public String getConfigName() {
        return ScmConfigNameDefine.ROLE;
    }

}
