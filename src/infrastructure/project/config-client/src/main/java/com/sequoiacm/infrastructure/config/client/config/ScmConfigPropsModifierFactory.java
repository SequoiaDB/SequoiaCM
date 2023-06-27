package com.sequoiacm.infrastructure.config.client.config;


import com.sequoiacm.infrastructure.config.util.ScmConfigPropsModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

@Component
public class ScmConfigPropsModifierFactory {

    @Value("${spring.config.location}")
    private String configPropsPath;

    public ScmConfigPropsModifier createConfigPropsDao() throws ScmConfigException {
        return new ScmConfigPropsModifier(configPropsPath);
    }
}
