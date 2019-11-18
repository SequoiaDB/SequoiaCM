package com.sequoiacm.config.framework.node;

import org.springframework.beans.factory.annotation.Autowired;

import com.sequoiacm.config.framework.Framework;
import com.sequoiacm.config.framework.ScmConfFrameworkBase;
import com.sequoiacm.config.framework.node.operator.ScmNodeConfOperator;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;

@Framework
public class ScmNodeFramework extends ScmConfFrameworkBase {

    @Autowired
    private ScmNodeConfOperator nodeOp;

    public ScmNodeFramework() {
        super(ScmConfigNameDefine.NODE);
    }

    @Override
    public ScmConfOperator getConfOperator() {
        return nodeOp;
    }

    @Override
    public String getConfigName() {
        return ScmConfigNameDefine.NODE;
    }
}
