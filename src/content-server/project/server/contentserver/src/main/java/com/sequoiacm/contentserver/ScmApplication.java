package com.sequoiacm.contentserver;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

import com.sequoiacm.contentserver.bizconfig.ContenserverConfClient;
import com.sequoiacm.contentserver.bizconfig.MetaDataConfSubscriber;
import com.sequoiacm.contentserver.bizconfig.NodeConfSubscriber;
import com.sequoiacm.contentserver.bizconfig.SiteConfSubscriber;
import com.sequoiacm.contentserver.bizconfig.WorkspaceConfSubscriber;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.infrastructure.audit.EnableAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditPropsVerifier;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.verifier.PreventingModificationVerifier;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;
import com.sequoiacm.infrastructure.security.privilege.impl.EnableScmPrivClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import com.sequoiadb.infrastructure.map.server.EnableMapServerWithoutDataSource;

@EnableScmMonitorServer
@EnableScmPrivClient
@SpringBootApplication
@EnableFeignClients("com.sequoiacm.cloud.security.privilege.impl")
@EnableDiscoveryClient
@EnableConfClient
@EnableMapServerWithoutDataSource
@EnableAudit
@ComponentScan(basePackages = { "com.sequoiacm.infrastructure.security.privilege.impl",
        "com.sequoiacm.contentserver" })
public class ScmApplication implements ApplicationRunner {
    private final static Logger logger = LoggerFactory.getLogger(ScmApplication.class);

    @Autowired
    private ScmPrivClient privClient;

    @Autowired
    private ScmConfClient confClient;

    @Autowired
    private Registration localInstance;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        start(args);
    }

    public void start(ApplicationArguments args) throws Exception {
        try {

            logger.info("arguments:");
            for (String o : args.getOptionNames()) {
                logger.info("{}={}", o, args.getOptionValues(o));
            }

            // if
            // (!args.containsOption(PropertiesDefine.APPLICATION_PROPERTIES_LOCATION)
            // || !args.containsOption(PropertiesDefine.LOGGING_CONFIG)) {
            // logger.error("{} or {} must be specified in command line",
            // PropertiesDefine.APPLICATION_PROPERTIES_LOCATION,
            // PropertiesDefine.LOGGING_CONFIG);
            // throw new
            // ScmSystemException(PropertiesDefine.APPLICATION_PROPERTIES_LOCATION
            // + " or " + PropertiesDefine.LOGGING_CONFIG + " must be
            // specified");
            // }

            String jarDir = ScmSystemUtils.getMyDir(ScmServer.class);
            logger.info("jarDir=" + jarDir);
            PropertiesUtils.setJarPath(jarDir);
            ScmServer.loadConfiguration();
            ScmServer.loadVersionAndStatus();
            ScmSystemUtils.logJVM();
            logger.info("starting SequoiaCM...");

            ScmLockManager.getInstance().init();

            String confRelativePath = ".." + File.separator + "conf" + File.separator
                    + "content-server" + File.separator + PropertiesUtils.getServerPort()
                    + File.separator + "application.properties";
            confClient.setConfFilePaht(confRelativePath);

            // first register have higher priority.
            confClient.registerConfigPropVerifier(new ScmAuditPropsVerifier());
            confClient.registerConfigPropVerifier(new PreventingModificationVerifier("scm."));

            ContenserverConfClient contentserverConfClient = ContenserverConfClient.getInstance()
                    .init(confClient);

            // subscribe ws config
            contentserverConfClient.subscribeWithAsyncRetry(new WorkspaceConfSubscriber(
                    localInstance.getServiceId(), PropertiesUtils.getWorkspaceVersionHeartbeat()));
            // subscribe metadata config
            contentserverConfClient.subscribeWithAsyncRetry(new MetaDataConfSubscriber(
                    localInstance.getServiceId(), PropertiesUtils.getMetaDataVersionHearbeat()));
            // subscribe site config
            contentserverConfClient.subscribeWithAsyncRetry(new SiteConfSubscriber(
                    localInstance.getServiceId(), PropertiesUtils.getSiteVersionHeartbeat()));
            // subscribe node config
            contentserverConfClient.subscribeWithAsyncRetry(new NodeConfSubscriber(
                    localInstance.getServiceId(), PropertiesUtils.getNodeVersionHeartbeat()));

            ScmServer ss = new ScmServer();
            ss.start();

            logger.info("init metaData into sys memory...");
            // init metaData into sys memory
            MetaDataManager.getInstence().initMetaDataInfomation();

            // initial privilege sevice
            logger.info("ScmPrivClient={}", privClient);
            ScmFileServicePriv.getInstance().init(privClient,
                    PropertiesUtils.getPrivilegeHeartBeatInterval());
        }
        catch (Exception e) {
            logger.error("server exit with error", e);
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(ScmApplication.class).bannerMode(Banner.Mode.OFF).run(args);
    }
}
