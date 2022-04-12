package com.sequoiacm.s3;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

import com.sequoiacm.contentserver.contentmodule.EnableContentModule;
import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiadb.infrastructure.map.client.EnableMapClient;

@EnableDiscoveryClient
@EnableMapClient
@SpringBootApplication
@EnableHystrix
@EnableContentModule
public class SequoiacmS3Application implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SequoiacmS3Application.class);

    @Autowired
    private MetaSourceService metaSourceService;

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
    }
}
