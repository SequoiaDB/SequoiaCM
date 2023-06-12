package com.sequoiacm.schedule;

import java.util.Date;
import java.util.List;

import com.sequoiacm.infrastructure.config.client.NotifyCallback;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
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

import com.sequoiacm.infrastructure.audit.EnableAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditPropsVerifier;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.discovery.EnableScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.discovery.ScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.lock.EnableScmLock;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;
import com.sequoiacm.infrastructure.security.privilege.impl.EnableScmPrivClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import com.sequoiacm.schedule.bizconf.ScheduleStrategyMgr;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.LifeCycleConfigDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.LifeCycleConfigFullEntity;
import com.sequoiacm.schedule.core.ScheduleMgrWrapper;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.ScmCheckCorrectionTools;
import com.sequoiacm.schedule.core.elect.ScheduleElector;
import com.sequoiacm.schedule.dao.FileServerDao;
import com.sequoiacm.schedule.dao.LifeCycleConfigDao;
import com.sequoiacm.schedule.dao.LifeCycleScheduleDao;
import com.sequoiacm.schedule.dao.ScheduleDao;
import com.sequoiacm.schedule.dao.SiteDao;
import com.sequoiacm.schedule.dao.StrategyDao;
import com.sequoiacm.schedule.dao.TaskDao;
import com.sequoiacm.schedule.dao.TransactionFactory;
import com.sequoiacm.schedule.dao.WorkspaceDao;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiacm.schedule.privilege.ScmSchedulePriv;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

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
@EnableScmLock
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

    @Autowired
    private LifeCycleConfigDao lifeCycleConfigDao;

    @Autowired
    private LifeCycleScheduleDao lifeCycleScheduleDao;

    @Autowired
    private TransactionFactory transactionFactory;

    @Autowired
    private ScmLockManager scmLockManager;

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

        initSystem(config);

        initLifeCycleConfig();
        logger.info("zookeeper={},server.port:{}", config.getZookeeperUrl(),
                config.getServerPort());
    }

    private void initSystem(ScheduleApplicationConfig config)
            throws Exception {
        ScmIdGenerator.FileId.init(0, 101);

        subscribeConfig();

        ScheduleServer.getInstance().init(siteDao, workspaceDao, fileServerDao, taskDao,
                strategyDao, discoveryClient);
        ScheduleMgrWrapper.getInstance().init(scheduleDao, clientFactory, config, discoveryClient,
                lifeCycleConfigDao, lifeCycleScheduleDao, transactionFactory, scmLockManager);
        ScmCheckCorrectionTools.getInstance().init(lifeCycleConfigDao, lifeCycleScheduleDao,
                scheduleDao, scmLockManager);
        ScheduleElector.getInstance().init(config.getZookeeperUrl(), config.getZookeeperAcl(),
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

        confClient.registerConfigPropVerifier(new ScmAuditPropsVerifier());
    }

    private void subscribeConfig() throws ScmConfigException {
        confClient.subscribe(ScmBusinessTypeDefine.WORKSPACE,
                new NotifyCallback() {
                    @Override
                    public void processNotify(EventType type, String businessName,
                            NotifyOption notification) throws Exception {
                        if (type == EventType.DELTE) {
                            ScheduleServer.getInstance().removeWorkspace(businessName);
                            ScheduleMgrWrapper.getInstance()
                                    .deleteScheduleByWorkspace(businessName);
                        }
                        else {
                            ScheduleServer.getInstance().reloadWorkspace(businessName);
                        }
                    }
                });

        confClient.subscribe(ScmBusinessTypeDefine.SITE,
                new NotifyCallback() {
                    @Override
                    public void processNotify(EventType type, String businessName,
                            NotifyOption notification) throws Exception {
                        if (type == EventType.DELTE) {
                            ScheduleServer.getInstance().removeSite(businessName);
                        }
                        else {
                            ScheduleServer.getInstance().reloadSite(businessName);
                        }
                    }
                });
        confClient.subscribe(ScmBusinessTypeDefine.NODE,
                new NotifyCallback() {
                    @Override
                    public void processNotify(EventType type, String businessName,
                            NotifyOption notification) throws Exception {
                        if (type == EventType.DELTE) {
                            ScheduleServer.getInstance().removeNode(businessName);
                        }
                        else {
                            ScheduleServer.getInstance().reloadNode(businessName);
                        }
                    }
                });
    }

    private void initLifeCycleConfig() throws Exception {
        ScmLock lock = null;
        ScmBSONObjectCursor query = null;
        try {
            lock = lockGlobal();
            query = lifeCycleConfigDao.query(null);
            if (query.hasNext()) {
                return;
            }

            BasicBSONList innerStageTag = innerStageTag();
            LifeCycleConfigFullEntity fullEntity = new LifeCycleConfigFullEntity();
            Date date = new Date();
            fullEntity.setStageTagConfig(innerStageTag);
            fullEntity.setCreateUser("admin");
            fullEntity.setUpdateUser("admin");
            fullEntity.setTransitionConfig(new BasicBSONList());
            fullEntity.setCreateTime(date.getTime());
            fullEntity.setUpdateTime(date.getTime());

            initDefaultConfig(fullEntity);
        }
        finally {
            if (null != lock) {
                lock.unlock();
            }
            if (query != null) {
                query.close();
            }
        }
    }

    private BasicBSONList innerStageTag() {
        BSONObject hot = new BasicBSONObject();
        hot.put(FieldName.LifeCycleConfig.FIELD_STAGE_NAME,
                LifeCycleConfigDefine.ScmSystemStageTagType.HOT);
        hot.put(FieldName.LifeCycleConfig.FIELD_STAGE_DESC,
                LifeCycleConfigDefine.ScmSystemStageTagType.HOT);

        BSONObject warm = new BasicBSONObject();
        warm.put(FieldName.LifeCycleConfig.FIELD_STAGE_NAME,
                LifeCycleConfigDefine.ScmSystemStageTagType.WARM);
        warm.put(FieldName.LifeCycleConfig.FIELD_STAGE_DESC,
                LifeCycleConfigDefine.ScmSystemStageTagType.WARM);

        BSONObject cold = new BasicBSONObject();
        cold.put(FieldName.LifeCycleConfig.FIELD_STAGE_NAME,
                LifeCycleConfigDefine.ScmSystemStageTagType.COLD);
        cold.put(FieldName.LifeCycleConfig.FIELD_STAGE_DESC,
                LifeCycleConfigDefine.ScmSystemStageTagType.COLD);

        BasicBSONList innerStageTag = new BasicBSONList();
        innerStageTag.add(hot);
        innerStageTag.add(warm);
        innerStageTag.add(cold);

        return innerStageTag;
    }

    private void initDefaultConfig(LifeCycleConfigFullEntity info) throws Exception {
        try {
            lifeCycleConfigDao.insert(info);
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                throw e;
            }
        }
    }

    private ScmLock lockGlobal() throws ScmLockException {
        String[] lockPath = { "global_life_cycle" };
        return scmLockManager.acquiresLock(new ScmLockPath(lockPath));
    }
}
