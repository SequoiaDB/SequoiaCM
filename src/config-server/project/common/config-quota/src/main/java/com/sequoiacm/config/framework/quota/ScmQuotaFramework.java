package com.sequoiacm.config.framework.quota;

import com.sequoiacm.config.framework.Framework;
import com.sequoiacm.config.framework.ScmConfFrameworkBase;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.config.framework.quota.operator.ScmQuotaConfOperator;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import org.springframework.beans.factory.annotation.Autowired;

@Framework
public class ScmQuotaFramework extends ScmConfFrameworkBase {

    @Autowired
    private ScmQuotaConfOperator quotaOperator;

    public ScmQuotaFramework() {
        super(ScmConfigNameDefine.QUOTA);
    }

    @Override
    public ScmConfOperator getConfOperator() {
        return quotaOperator;
    }
}
