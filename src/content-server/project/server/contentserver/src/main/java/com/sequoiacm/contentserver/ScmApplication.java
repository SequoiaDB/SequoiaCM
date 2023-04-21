package com.sequoiacm.contentserver;

import java.io.File;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.netflix.appinfo.ApplicationInfoManager;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.contentmodule.ContentModuleExcludeMarker;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.audit.EnableAudit;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.client.core.role.EnableRoleSubscriber;
import com.sequoiacm.infrastructure.config.client.core.user.EnableUserSubscriber;
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
@EnableUserSubscriber
@EnableRoleSubscriber
@EnableMapServerWithoutDataSource
@EnableAudit
@ComponentScan(basePackages = { "com.sequoiacm.infrastructure.security.privilege.impl",
        "com.sequoiacm.contentserver" })
@EnableHystrix
@ContentModuleExcludeMarker
public class ScmApplication implements ApplicationRunner {
    private final static Logger logger = LoggerFactory.getLogger(ScmApplication.class);

    @Autowired
    private ScmPrivClient privClient;

    @Autowired
    private ScmConfClient confClient;

    @Autowired
    private Registration localInstance;

    @Value("${spring.application.name}")
    private String siteName;

    @Autowired
    private BucketInfoManager bucketInfoManager;
    @Autowired
    private IDirService dirService;

    @Autowired
    ApplicationInfoManager applicationInfoManager;

    @Autowired
    private ApplicationContext applicationContext;

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

            String confRelativePath = ".." + File.separator + "conf" + File.separator
                    + "content-server" + File.separator + PropertiesUtils.getServerPort()
                    + File.separator + "application.properties";
            confClient.setConfFilePath(confRelativePath);
            confClient.registerConfigPropVerifier(new PreventingModificationVerifier(
                    "eureka.instance.metadata-map.isContentServer",
                    "eureka.instance.metadata-map.isRootSiteInstance",
                    "eureka.instance.metadata-map.siteId"));

            ScmServer ss = ScmServer.getInstance();
            ss.init(applicationContext, privClient, confClient, siteName, bucketInfoManager,
                    dirService);
            eurekaRegisterServiceId(ss);
        }
        catch (Exception e) {
            logger.error("server exit with error", e);
            System.exit(-1);
        }
    }

    public void eurekaRegisterServiceId(ScmServer scmServer) throws ScmServerException {
        HashMap<String, String> map = new HashMap<>();
        map.put("contentServerId", "" + scmServer.getContentServerInfo().getId());
        applicationInfoManager.registerAppMetadata(map);
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(ScmApplication.class).bannerMode(Banner.Mode.OFF).run(args);
    }

}
