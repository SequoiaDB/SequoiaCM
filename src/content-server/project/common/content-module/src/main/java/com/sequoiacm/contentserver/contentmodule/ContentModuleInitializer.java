package com.sequoiacm.contentserver.contentmodule;

import java.util.List;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.ScmServer;
import com.sequoiacm.contentserver.bizconfig.BucketConfSubscriber;
import com.sequoiacm.contentserver.bizconfig.ContenserverConfClient;
import com.sequoiacm.contentserver.bizconfig.MetaDataConfSubscriber;
import com.sequoiacm.contentserver.bizconfig.NodeConfSubscriber;
import com.sequoiacm.contentserver.bizconfig.SiteConfSubscriber;
import com.sequoiacm.contentserver.bizconfig.WorkspaceConfSubscriber;
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

public class ContentModuleInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ContentModuleInitializer.class);
    private final String serviceName;
    private final String bindingSite;
    private final IMetaSourceHandler metaSouceHandler;
    private final BucketInfoManager bucketInfoMgr;
    private final IDirService dirService;

    private ScmPrivClient privClient;

    private ScmConfClient confClient;

    public ContentModuleInitializer(ScmPrivClient privClient, ScmConfClient confClient,
            String serviceName, String bindingSite, IMetaSourceHandler metaSourceHandler,
            BucketInfoManager bucketMgr, IDirService dirService) {
        this.privClient = privClient;
        this.confClient = confClient;
        this.serviceName = serviceName;
        this.bindingSite = bindingSite;
        this.metaSouceHandler = metaSourceHandler;
        this.bucketInfoMgr = bucketMgr;
        this.dirService = dirService;
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
        contentserverConfClient.subscribeWithAsyncRetry(new WorkspaceConfSubscriber(bucketInfoMgr, serviceName,
                PropertiesUtils.getWorkspaceVersionHeartbeat()));
        // subscribe metadata config
        contentserverConfClient.subscribeWithAsyncRetry(new MetaDataConfSubscriber(serviceName,
                PropertiesUtils.getMetaDataVersionHearbeat()));
        // subscribe site config
        contentserverConfClient.subscribeWithAsyncRetry(
                new SiteConfSubscriber(serviceName, PropertiesUtils.getSiteVersionHeartbeat()));
        // subscribe node config
        contentserverConfClient.subscribeWithAsyncRetry(
                new NodeConfSubscriber(serviceName, PropertiesUtils.getNodeVersionHeartbeat()));

        contentserverConfClient.subscribeWithAsyncRetry(new BucketConfSubscriber(bucketInfoMgr,
                serviceName, PropertiesUtils.getSiteVersionHeartbeat()));

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
    }

    public void initIdGenerator(int serverId) throws Exception {
        ScmIdGenerator.FileId.init(0, serverId);
    }
}
