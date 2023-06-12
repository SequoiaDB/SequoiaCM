package com.sequoiacm.contentserver.contentmodule;

import java.util.List;

import com.sequoiacm.contentserver.bizconfig.MetaDataNotifyCallback;
import com.sequoiacm.contentserver.bizconfig.NodeConfNotifyCallback;
import com.sequoiacm.contentserver.bizconfig.WorkspaceConfNotifyCallback;
import com.sequoiacm.contentserver.common.IDGeneratorDao;
import com.sequoiacm.contentserver.tag.TagLibMgr;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.ScmServer;
import com.sequoiacm.contentserver.bizconfig.ContenserverConfClient;
import com.sequoiacm.contentserver.bizconfig.SiteConfNotifyCallback;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.infrastructure.audit.ScmAuditPropsVerifier;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import com.sequoiacm.metasource.sequoiadb.IMetaSourceHandler;
import org.springframework.context.ApplicationContext;

public class ContentModuleInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ContentModuleInitializer.class);
    private final String serviceName;
    private final String bindingSite;
    private final IMetaSourceHandler metaSouceHandler;
    private final BucketInfoManager bucketInfoMgr;
    private final IDirService dirService;
    private final IDGeneratorDao idGeneratorDao;
    private final TagLibMgr tagLibMgr;

    private ScmPrivClient privClient;

    private ScmConfClient confClient;

    private ApplicationContext applicationContext;

    public ContentModuleInitializer(ApplicationContext applicationContext, ScmPrivClient privClient,
            ScmConfClient confClient, String serviceName, String bindingSite,
            IMetaSourceHandler metaSourceHandler, BucketInfoManager bucketMgr,
            IDirService dirService, IDGeneratorDao idGeneratorDao, TagLibMgr tagLibMgr) {
        this.applicationContext = applicationContext;
        this.privClient = privClient;
        this.confClient = confClient;
        this.serviceName = serviceName;
        this.bindingSite = bindingSite;
        this.metaSouceHandler = metaSourceHandler;
        this.bucketInfoMgr = bucketMgr;
        this.dirService = dirService;
        this.idGeneratorDao = idGeneratorDao;
        this.tagLibMgr = tagLibMgr;
    }

    public void initBizComponent() throws Exception {
        ScmContentModule.bindSite(bindingSite);
        ScmContentModule.metaSourceHandler(metaSouceHandler);
        String jarDir = ScmSystemUtils.getMyDir(ScmServer.class);
        logger.info("jarDir=" + jarDir);
        PropertiesUtils.setJarPath(jarDir);
        ScmSystemUtils.logJVM();
        logger.info("starting content-module...");

        ScmLockManager.getInstance().init(PropertiesUtils.getScmLockConfig());

        confClient.registerConfigPropVerifier(new ScmAuditPropsVerifier());

        ContenserverConfClient contentserverConfClient = ContenserverConfClient.getInstance()
                .init(confClient, bucketInfoMgr);

        // subscribe ws config
        contentserverConfClient.subscribe(ScmBusinessTypeDefine.WORKSPACE,
                new WorkspaceConfNotifyCallback(tagLibMgr));

        // subscribe metadata config
        contentserverConfClient.subscribe(ScmBusinessTypeDefine.META_DATA,
                new MetaDataNotifyCallback());

        // subscribe site config
        contentserverConfClient.subscribe(ScmBusinessTypeDefine.SITE, new SiteConfNotifyCallback());

        // subscribe node config
        contentserverConfClient.subscribe(ScmBusinessTypeDefine.NODE, new NodeConfNotifyCallback());

        logger.info("init strategy");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        List<BSONObject> strategyList = contentModule.getMetaService().getAllStrategyInfo();
        ScmStrategyMgr.getInstance().init(strategyList, contentModule.getMainSite());

        logger.info("init metaData into sys memory...");
        // init metaData into sys memory
        MetaDataManager.getInstence().initMetaDataInfomation();

        // initial privilege sevice
        logger.info("ScmPrivClient={}", privClient);
        ScmFileServicePriv.getInstance().init(bucketInfoMgr, dirService, privClient,
                PropertiesUtils.getPrivilegeHeartBeatInterval());

        idGeneratorDao.ensureTable();
    }

    public void initIdGenerator(int serverId) throws Exception {
        ScmIdGenerator.FileId.init(0, serverId);
    }
}
