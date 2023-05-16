package com.sequoiacm.s3;

import java.io.File;
import java.util.Arrays;

import com.sequoiacm.contentserver.quota.EnableQuotaMsgClient;
import com.sequoiacm.contentserver.common.IDGeneratorDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

import com.sequoiacm.contentserver.contentmodule.EnableContentModule;
import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.client.core.role.EnableRoleSubscriber;
import com.sequoiacm.infrastructure.config.client.core.user.EnableUserSubscriber;
import com.sequoiacm.infrastructure.config.core.verifier.PreventingModificationVerifier;
import com.sequoiacm.infrastructure.lock.EnableScmLock;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.dao.PartDao;
import com.sequoiacm.s3.dao.UploadDao;
import com.sequoiadb.infrastructure.map.client.EnableMapClient;

@EnableScmLock
@EnableDiscoveryClient
@EnableMapClient
@EnableUserSubscriber
@EnableRoleSubscriber
@SpringBootApplication
@EnableHystrix
@EnableContentModule
@EnableQuotaMsgClient
public class SequoiacmS3Application implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SequoiacmS3Application.class);

    @Autowired
    private MetaSourceService metaSourceService;

    @Autowired
    IDGeneratorDao idGeneratorDao;

    @Autowired
    UploadDao uploadDao;

    @Autowired
    PartDao partDao;

    @Autowired
    private ScmConfClient confClient;

    @Value("${server.port}")
    private int serverPort;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SequoiacmS3Application.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        MetaAccessor regionAccessor = metaSourceService.getMetaSource()
                .createMetaAccessor(S3CommonDefine.DEFAULT_REGION_TABLE_NAME);
        regionAccessor.ensureTable(null, null);

        MetaAccessor contextAccessor = metaSourceService.getMetaSource()
                .createMetaAccessor(S3CommonDefine.LIST_OBJECT_CONTEXT_TABLE_NAME);
        contextAccessor.ensureTable(null, Arrays.asList(S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_TOKEN));

        uploadDao.initUploadMetaTable();
        partDao.initPartsTable();

        String confRelativePath = ".." + File.separator + "conf" + File.separator + "s3-server"
                + File.separator + serverPort + File.separator + "application.properties";
        confClient.setConfFilePath(confRelativePath);
        confClient.registerConfigPropVerifier(
                new PreventingModificationVerifier("eureka.instance.metadata-map.isS3Server",
                        "eureka.instance.metadata-map.bindingSite", "scm.content-module.site"));
    }
}