package com.sequoiacm.schedule;

import com.sequoiacm.infrastructure.audit.EnableAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditPropsVerifier;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.verifier.PreventingModificationVerifier;
import com.sequoiacm.infrastructure.discovery.EnableScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.discovery.ScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;
import com.sequoiacm.infrastructure.security.privilege.impl.EnableScmPrivClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import com.sequoiacm.schedule.bizconf.ScheduleStrategyMgr;
import com.sequoiacm.schedule.bizconf.ScmNodeConfSubscriber;
import com.sequoiacm.schedule.bizconf.ScmSiteConfSubscriber;
import com.sequoiacm.schedule.bizconf.ScmWorkspaceConfSubscriber;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.core.ScheduleMgrWrapper;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.elect.ScheduleElector;
import com.sequoiacm.schedule.dao.*;
import com.sequoiacm.schedule.privilege.ScmSchedulePriv;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

@SpringBootApplication
@EnableScmPrivClient
@EnableScmMonitorServer
@EnableFeignClients("com.sequoiacm.cloud.security.privilege.impl")
@EnableScmServiceDiscoveryClient
@EnableAudit
@EnableConfClient
@ComponentScan(basePackages = { "com.sequoiacm.infrastructure.security.privilege.impl",
        "com.sequoiacm.schedule" })
@EnableHystrix
public class ScheduleApplication implements ApplicationRunner {
    private final static Logger logger = LoggerFactory.getLogger(ScheduleApplication.class);

    @Autowired
    ScheduleApplicationConfig config;

    @Autowired
    private FileServerDao fileServerDao;

    @Autowired
    private SiteDao siteDao;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private WorkspaceDao workspaceDao;

    @Autowired
    private ScheduleDao scheduleDao;

    @Autowired
    private StrategyDao strategyDao;

    @Autowired
    private ScheduleClientFactory clientFactory;

    @Autowired
    private ScmPrivClient privClient;

    @Autowired
    private ScmConfClient confClient;

    @Autowired
    private Registration localInstance;

    @Autowired
    private ScmServiceDiscoveryClient discoveryClient;

    @Value("${server.port}")
    private int serverPort;

    public static void main(String[] args) {
        new SpringApplicationBuilder(ScheduleApplication.class).bannerMode(Banner.Mode.OFF)
                .run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("arguments:");
        for (String o : args.getOptionNames()) {
            logger.info("{}={}", o, args.getOptionValues(o));
        }
//
//        if (!args.containsOption(ScheduleDefine.APPLICATION_PROPERTIES_LOCATION)
//                || !args.containsOption(ScheduleDefine.LOGGING_CONFIG)) {
//            logger.error("{} or {} must be specified in command line",
//                    ScheduleDefine.APPLICATION_PROPERTIES_LOCATION, ScheduleDefine.LOGGING_CONFIG);
//            throw new Exception(ScheduleDefine.APPLICATION_PROPERTIES_LOCATION + " or "
//                    + ScheduleDefine.LOGGING_CONFIG + " must be specified");
//        }

        initSystem(config);
        logger.info("zookeeper={},server.port:{}", config.getZookeeperUrl(),
                config.getServerPort());
    }

    private void initSystem(ScheduleApplicationConfig config) throws Exception {
        ScmIdGenerator.FileId.init(0, 101);
        // subscribe ws conig
        confClient.subscribeWithAsyncRetry(new ScmWorkspaceConfSubscriber(
                localInstance.getServiceId(), config.getWorkspaceHeartbeat()));
        // subscribe site config
        confClient.subscribeWithAsyncRetry(
                new ScmSiteConfSubscriber(localInstance.getServiceId(), config.getSiteHeartbeat()));
        // subscribe node config
        confClient.subscribeWithAsyncRetry(new ScmNodeConfSubscriber(localInstance.getServiceId(),
                config.getSreverNodeHeartbeat()));

        ScheduleServer.getInstance().init(siteDao, workspaceDao, fileServerDao, taskDao,
                strategyDao, discoveryClient);
        ScheduleMgrWrapper.getInstance().init(scheduleDao, clientFactory, config, discoveryClient);
        ScheduleElector.getInstance().init(config.getZookeeperUrl(),
                ScheduleDefine.SCHEDULE_ELETOR_PATH,
                ScheduleCommonTools.getHostName() + ":" + config.getServerPort(),
                config.getRevoteInitialInterval(), config.getRevoteMaxInterval(),
                config.getRevoteIntervalMultiplier());

        // ScheduleLockFactory.getInstance().init(config.getZookeeperUrl(), 4);

        // init strategy
        List<BSONObject> strategyList = ScheduleServer.getInstance().getAllStrategy();
        ScheduleStrategyMgr.getInstance().init(strategyList,
                ScheduleServer.getInstance().getRootSite());
        // initial privilege sevice
        logger.info("ScmPrivClient={}", privClient);
        ScmSchedulePriv.getInstance().init(privClient, config.getPriHBInterval());

        // first register have higher priority.
        confClient.registerConfigPropVerifier(new ScmAuditPropsVerifier());
        confClient.registerConfigPropVerifier(new PreventingModificationVerifier("scm."));
    }
}
