package com.sequoiacm.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// AutoConfig by resources/META-INF/spring.factories
@Component
public class ApplicationPropertyInjection implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationPropertyInjection.class);
    private final String CONFIG_LOCATION_OPTION = "spring.config.location";

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        List<String> optionValues = applicationArguments.getOptionValues(CONFIG_LOCATION_OPTION);
        if (optionValues == null) {
            return;
        }
        if (optionValues.size() != 1) {
            throw new Exception("option " + CONFIG_LOCATION_OPTION + " is only one,list=" + optionValues.toString());
        }
        String result = optionValues.get(0);
        System.setProperty(CONFIG_LOCATION_OPTION, result);
        logger.info("set property success, {}={}", CONFIG_LOCATION_OPTION, result);
    }
}
