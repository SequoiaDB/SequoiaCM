package com.sequoiacm.config.server.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.EnableConfOperator;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

@Component
@EnableConfOperator
public class ScmConfOperatorMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfOperatorMgr.class);
    private Map<String, ScmConfOperator> operators = new HashMap<>();

    @Autowired
    public ScmConfOperatorMgr(List<ScmConfOperator> operators) {
        for (ScmConfOperator operator : operators) {
            BusinessType businessType = operator.getClass().getAnnotation(BusinessType.class);
            if (businessType == null) {
                throw new IllegalStateException(
                        "ScmConfOperator must be annotated with @BusinessType: "
                                + operator.getClass().getName());
            }
            registerConfOperator(businessType.value(), operator);
        }
    }

    public ScmConfOperator getConfOperator(String businessType) throws ScmConfigException {
        ScmConfOperator op = operators.get(businessType);
        if (op == null) {
            throw new ScmConfigException(ScmConfError.NO_SUCH_CONFIG,
                    "no such config:businessType=" + businessType);
        }
        return op;
    }

    private void registerConfOperator(String businessType, ScmConfOperator operator) {
        logger.info("register operator module: businessType={}, operator={}", businessType,
                operator.getClass());
        ScmConfOperator old = operators.put(businessType, operator);
        if (old != null) {
            throw new IllegalStateException("duplicate operator module: businessType="
                    + businessType + ", classes=" + old.getClass() + ", " + operator.getClass());
        }
    }
}
