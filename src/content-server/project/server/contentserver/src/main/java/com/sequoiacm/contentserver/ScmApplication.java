package com.sequoiacm.contentserver;

import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.contentmodule.ContentModuleExcludeMarker;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.infrastructure.audit.EnableAudit;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;
import com.sequoiacm.infrastructure.security.privilege.impl.EnableScmPrivClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import com.sequoiadb.infrastructure.map.server.EnableMapServerWithoutDataSource;
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
import org.springframework.context.annotation.ComponentScan;

import java.io.File;

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
            confClient.setConfFilePaht(confRelativePath);

            ScmServer ss = ScmServer.getInstance();
            ss.init(privClient, confClient, siteName, bucketInfoManager, dirService);
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
