package com.sequoiacm.config.server.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.sequoiacm.config.framework.EnableFramework;
import com.sequoiacm.config.framework.ScmConfFramework;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.config.framework.subscriber.ScmConfSubscriberFactory;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

@Component
@EnableFramework
public class ScmConfFrameworkMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfFrameworkMgr.class);
    private Map<String, ScmConfOperator> operators = new HashMap<>();
    private Map<String, ScmConfSubscriberFactory> subscriberFactorys = new HashMap<>();

    @Autowired
    public ScmConfFrameworkMgr(List<ScmConfFramework> frameworks) {
        for (ScmConfFramework framework : frameworks) {
            registerFramework(framework);
        }
    }

    public ScmConfOperator getConfOperator(String configName) throws ScmConfigException {
        ScmConfOperator op = operators.get(configName);
        if (op == null) {
            throw new ScmConfigException(ScmConfError.NO_SUCH_CONFIG,
                    "no such config:configName=" + configName);
        }
        return op;
    }

    public ScmConfSubscriberFactory getSubscriberFactory(String configName) {
        ScmConfSubscriberFactory factory = subscriberFactorys.get(configName);
        Assert.notNull(factory, "no such subscriberFactory:" + configName);
        return factory;
    }

    private void registerFramework(ScmConfFramework framework) {
        logger.info("register framework module:" + framework.getConfigName());
        operators.put(framework.getConfigName(), framework.getConfOperator());
        subscriberFactorys.put(framework.getConfigName(), framework.getSubscriberFactory());
    }
}
