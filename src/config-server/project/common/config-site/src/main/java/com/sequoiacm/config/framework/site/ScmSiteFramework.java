package com.sequoiacm.config.framework.site;

import org.springframework.beans.factory.annotation.Autowired;

import com.sequoiacm.config.framework.Framework;
import com.sequoiacm.config.framework.ScmConfFrameworkBase;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.config.framework.site.operator.ScmSiteConfOperator;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;

@Framework
public class ScmSiteFramework extends ScmConfFrameworkBase {

    @Autowired
    private ScmSiteConfOperator siteOp;

    public ScmSiteFramework() {
        super(ScmConfigNameDefine.SITE);
    }

    @Override
    public ScmConfOperator getConfOperator() {
        return siteOp;
    }

    @Override
    public String getConfigName() {
        return ScmConfigNameDefine.SITE;
    }

}
