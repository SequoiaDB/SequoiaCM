package com.sequoiacm.config.framework.metadata;

import org.springframework.beans.factory.annotation.Autowired;

import com.sequoiacm.config.framework.Framework;
import com.sequoiacm.config.framework.ScmConfFrameworkBase;
import com.sequoiacm.config.framework.metadata.operator.MetaDataConfOperator;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;

@Framework
public class MetaDataConfFramework extends ScmConfFrameworkBase {

    @Autowired
    MetaDataConfOperator op;

    public MetaDataConfFramework() {
        super(ScmConfigNameDefine.META_DATA);
    }

    @Override
    public ScmConfOperator getConfOperator() {
        return op;
    }

}
