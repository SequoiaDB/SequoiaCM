package com.sequoiacm.config.framework.bucket;

import org.springframework.beans.factory.annotation.Autowired;

import com.sequoiacm.config.framework.Framework;
import com.sequoiacm.config.framework.ScmConfFrameworkBase;
import com.sequoiacm.config.framework.bucket.operator.ScmBucketConfOperator;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;

@Framework
public class ScmBucketFramework extends ScmConfFrameworkBase {

    @Autowired
    private ScmBucketConfOperator bucketOp;

    public ScmBucketFramework() {
        super(ScmConfigNameDefine.BUCKET);
    }

    @Override
    public ScmConfOperator getConfOperator() {
        return bucketOp;
    }

    @Override
    public String getConfigName() {
        return ScmConfigNameDefine.BUCKET;
    }

}
