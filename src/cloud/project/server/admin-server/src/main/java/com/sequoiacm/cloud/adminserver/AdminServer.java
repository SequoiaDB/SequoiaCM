package com.sequoiacm.cloud.adminserver;

import com.sequoiacm.cloud.adminserver.core.job.BreakpointFileCleanJobManager;
import com.sequoiacm.cloud.adminserver.dao.BreakpointFileStatisticsDao;
import com.sequoiacm.infrastructure.lock.EnableScmLock;
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

import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.core.StatisticsServer;
import com.sequoiacm.cloud.adminserver.core.job.StatisticsJobManager;
import com.sequoiacm.cloud.adminserver.dao.ContentServerDao;
import com.sequoiacm.cloud.adminserver.dao.StatisticsDao;
import com.sequoiacm.cloud.adminserver.dao.WorkspaceDao;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.verifier.PreventingModificationVerifier;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;

import de.codecentric.boot.admin.config.EnableAdminServer;

@EnableDiscoveryClient
@EnableAdminServer
@SpringBootApplication
@EnableScmMonitorServer
@EnableConfClient
@EnableScmLock
@ComponentScan(basePackages = { "com.sequoiacm.cloud.adminserver" })
public class AdminServer implements ApplicationRunner {

    private final static Logger logger = LoggerFactory.getLogger(AdminServer.class);
    
    @Autowired
    AdminServerConfig config;

    @Autowired
    private ContentServerDao contentServerDao;

    @Autowired
    private StatisticsDao statisticsDao;

    @Autowired
    private WorkspaceDao workspaceDao;

    @Autowired
    private BreakpointFileStatisticsDao breakpointFileStatisticsDao;

    @Autowired
    private ScmConfClient confClient;
    
    public static void main(String[] args) {
        new SpringApplicationBuilder(AdminServer.class).bannerMode(Banner.Mode.OFF).run(
                args);
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("arguments:");
        for (String o : args.getOptionNames()) {
            logger.info("{}={}", o, args.getOptionValues(o));
        }

        if (!args.containsOption(StatisticsDefine.APPLICATION_PROPERTIES_LOCATION)
                || !args.containsOption(StatisticsDefine.LOGGING_CONFIG)) {
            logger.error("{} or {} must be specified in command line",
                    StatisticsDefine.APPLICATION_PROPERTIES_LOCATION,
                    StatisticsDefine.LOGGING_CONFIG);
            throw new Exception(StatisticsDefine.APPLICATION_PROPERTIES_LOCATION + " or "
                    + StatisticsDefine.LOGGING_CONFIG + " must be specified");
        }

        confClient.registerConfigPropVerifier(new PreventingModificationVerifier("scm.", "scm.statistics"));

        initSystem(config);
    }

   private void initSystem(AdminServerConfig config) throws Exception {
        StatisticsServer.getInstance().init(contentServerDao, statisticsDao, workspaceDao);

        StatisticsJobManager.getInstance().startTrafficJob(config.getJobFirstTime(),
                config.getJobPeriod());
        StatisticsJobManager.getInstance().startFileDeltaJob(config.getJobFirstTime(),
                config.getJobPeriod());
        BreakpointFileCleanJobManager.getInstance().startCleanJob(breakpointFileStatisticsDao,
                config.getBreakpointFileCleanPeriod(), config.getBreakpointFileStayDays());
   }
  
}

