package com.sequoiacm.contentserver.contentmodule;

import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class ContentModuleInitRunner {
    private static final Logger logger = LoggerFactory.getLogger(ContentModuleInitRunner.class);

    @Autowired
    private ScmPrivClient privClient;
    @Autowired
    private ScmConfClient confClient;
    @Autowired
    private Registration localService;
    @Autowired
    private ContentModuleConfig config;
    @Autowired
    private BucketInfoManager bucketInfoMgr;

    private boolean isInit = false;
    @Autowired
    private IDirService dirService;

    @PostConstruct
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public synchronized void init() throws Exception {
        if (isInit) {
            return;
        }
        ContentModuleInitializer initializer = new ContentModuleInitializer(privClient, confClient,
                localService.getServiceId(), config.getSite(), null, bucketInfoMgr, dirService);
        initializer.initBizComponent();
        String instance = localService.getHost() + localService.getPort();
        int instanceHash = instance.hashCode();
        // id generator 只使用低 16 位，这里做一次异或扰乱低16位
        short instanceHashShort = (short) ((instanceHash >>> 16) ^ instanceHash);
        // id 不接受负数, 取一个绝对值
        instanceHashShort = (short) Math.abs(instanceHashShort);
        logger.info("init id generator: instance={}, serverId(hashCode)={}", instance,
                instanceHashShort);
        initializer.initIdGenerator(instanceHashShort);
        isInit = true;
    }

    @PreDestroy
    public void destroy() throws ScmServerException {
        ScmLockManager.getInstance().close();
        ScmContentModule.getInstance().getSiteMgr().clear();
    }
}
