package com.sequoiacm.config.framework;

import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.config.framework.subscriber.ScmConfSubscriberFactory;

public interface ScmConfFramework {

    ScmConfOperator getConfOperator();

    ScmConfSubscriberFactory getSubscriberFactory();

    String getConfigName();
}
