package com.sequoiacm.config.server;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import com.sequoiacm.config.framework.lock.ScmLockManager;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.verifier.PreventingModificationVerifier;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;

@EnableDiscoveryClient
@EnableScmMonitorServer
@EnableAsync
@SpringBootApplication
@EnableConfClient
@ComponentScan(basePackages = { "com.sequoiacm.config.server", "com.sequoiacm.config.framework" })
public class ScmConfigApplication implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfigApplication.class);
    @Autowired
    AppConfig appConfig;
    
    @Autowired
    ScmConfClient confClient;

    public static void main(String[] args) {
        new SpringApplicationBuilder(ScmConfigApplication.class).bannerMode(Banner.Mode.OFF)
                .run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("arguments:");
        for (String o : args.getOptionNames()) {
            logger.info("{}={}", o, args.getOptionValues(o));
        }

        LockConfig lockConfig = appConfig.getLockConfig();
        ScmLockManager.getInstance().init(lockConfig.getUrls(), lockConfig.getClientNum(),
                lockConfig.getCleanJobPeriod(), lockConfig.getCleanJobResidualTime());
        
        confClient.registerConfigPropVerifier(new PreventingModificationVerifier("scm."));

        // TODO: use IP + port for contentserverId
        ScmIdGenerator.FileId.init(0, 102);
        
    }
    
}
