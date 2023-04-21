package com.sequoiacm.cloud.adminserver;

import com.sequoiacm.cloud.adminserver.config.PrivilegeHeartBeatConfig;
import com.sequoiacm.cloud.adminserver.lock.LockPathFactory;
import com.sequoiacm.infrastructure.config.client.core.bucket.BucketConfSubscriber;
import com.sequoiacm.infrastructure.config.client.core.bucket.EnableBucketSubscriber;
import com.sequoiacm.infrastructure.config.client.core.role.EnableRoleSubscriber;
import com.sequoiacm.infrastructure.config.client.core.user.EnableUserSubscriber;
import com.sequoiacm.infrastructure.config.client.core.workspace.EnableWorkspaceSubscriber;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.security.privilege.impl.EnableScmPrivClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.ComponentScan;

import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.core.StatisticsServer;
import com.sequoiacm.cloud.adminserver.core.job.BreakpointFileCleanJobManager;
import com.sequoiacm.cloud.adminserver.core.job.StatisticsJobManager;
import com.sequoiacm.cloud.adminserver.dao.BreakpointFileStatisticsDao;
import com.sequoiacm.cloud.adminserver.dao.ContentServerDao;
import com.sequoiacm.cloud.adminserver.dao.StatisticsDao;
import com.sequoiacm.cloud.adminserver.dao.WorkspaceDao;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.lock.EnableScmLock;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;

import de.codecentric.boot.admin.config.EnableAdminServer;

@EnableDiscoveryClient
@EnableAdminServer
@SpringBootApplication
@EnableScmMonitorServer
@EnableConfClient
@EnableScmLock
@ComponentScan(basePackages = { "com.sequoiacm.cloud.adminserver" })
@EnableHystrix
@EnableBucketSubscriber
@EnableWorkspaceSubscriber
@EnableScmPrivClient
@EnableUserSubscriber
@EnableRoleSubscriber
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

    @Autowired
    private BucketConfSubscriber bucketConfSubscriber;

    @Autowired
    private ScmLockManager lockManager;

    @Autowired
    private LockPathFactory lockPathFactory;

    @Autowired
    private ScmPrivClient privClient;

    @Autowired
    private PrivilegeHeartBeatConfig privilegeHeartBeatConfig;
    
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

        initSystem(config);
        privClient.updateHeartbeatInterval(privilegeHeartBeatConfig.getInterval());
    }

   private void initSystem(AdminServerConfig config) throws Exception {
       StatisticsServer.getInstance().init(contentServerDao, statisticsDao, workspaceDao,
               bucketConfSubscriber, lockManager, lockPathFactory);
        StatisticsJobManager.getInstance().startTrafficJob(config.getJobFirstTime(),
                config.getJobPeriod());
        if (config.isFileDeltaEnabled()) {
            StatisticsJobManager.getInstance().startFileDeltaJob(config.getJobFirstTime(),
                    config.getJobPeriod());
        }
        if (config.isObjectDeltaEnabled()) {
            StatisticsJobManager.getInstance().startObjectDeltaJob(config.getJobFirstTime(),
                    config.getJobPeriod());
        }
        BreakpointFileCleanJobManager.getInstance().startCleanJob(breakpointFileStatisticsDao,
                config.getBreakpointFileCleanPeriod(), config.getBreakpointFileStayDays());
   }
  
}

