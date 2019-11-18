package com.sequoiacm.perf;

import com.sequoiacm.perf.operation.PerfStrategy;
import com.sequoiacm.perf.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PerfRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(PerfRunner.class);

    @Autowired
    private Config config;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Performance running");
        logger.info(config.toString());
        PerfStrategy.run(config);
    }

}
