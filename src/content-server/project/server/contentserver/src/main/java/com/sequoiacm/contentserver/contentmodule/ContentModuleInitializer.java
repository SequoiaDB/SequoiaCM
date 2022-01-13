package com.sequoiacm.contentserver.contentmodule;

import com.sequoiacm.contentserver.ScmServer;
import com.sequoiacm.contentserver.bizconfig.*;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.infrastructure.audit.ScmAuditPropsVerifier;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.verifier.PreventingModificationVerifier;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import com.sequoiacm.metasource.sequoiadb.IMetaSourceHandler;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ContentModuleInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ContentModuleInitializer.class);
    private final String serviceName;
    private final String bindingSite;
    private final IMetaSourceHandler metaSouceHandler;

    private ScmPrivClient privClient;

    private ScmConfClient confClient;

    public ContentModuleInitializer(ScmPrivClient privClient, ScmConfClient confClient,
            String serviceName, String bindingSite, IMetaSourceHandler metaSourceHandler) {
        this.privClient = privClient;
        this.confClient = confClient;
        this.serviceName = serviceName;
        this.bindingSite = bindingSite;
        this.metaSouceHandler = metaSourceHandler;
    }

    public void initBizComponent() throws Exception {
        ScmContentServer.bindSite(bindingSite);
        ScmContentServer.metaSourceHandler(metaSouceHandler);
        String jarDir = ScmSystemUtils.getMyDir(ScmServer.class);
        logger.info("jarDir=" + jarDir);
        PropertiesUtils.setJarPath(jarDir);
        ScmSystemUtils.logJVM();
        logger.info("starting content-module...");

        ScmLockManager.getInstance().init();

        // first register have higher priority.
        confClient.registerConfigPropVerifier(new ScmAuditPropsVerifier());
        confClient.registerConfigPropVerifier(new PreventingModificationVerifier("scm."));

        ContenserverConfClient contentserverConfClient = ContenserverConfClient.getInstance()
                .init(confClient);

        // subscribe ws config
        contentserverConfClient.subscribeWithAsyncRetry(new WorkspaceConfSubscriber(serviceName,
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

        logger.info("init strategy");
        ScmContentServer contentServer = ScmContentServer.getInstance();
        List<BSONObject> strategyList = contentServer.getMetaService().getAllStrategyInfo();
        ScmStrategyMgr.getInstance().init(strategyList, contentServer.getMainSite());

        logger.info("init metaData into sys memory...");
        // init metaData into sys memory
        MetaDataManager.getInstence().initMetaDataInfomation();

        // initial privilege sevice
        logger.info("ScmPrivClient={}", privClient);
        ScmFileServicePriv.getInstance().init(privClient,
                PropertiesUtils.getPrivilegeHeartBeatInterval());
    }

    public void initIdGenerator(int serverId) throws Exception {
        ScmIdGenerator.FileId.init(0, serverId);
    }
}
