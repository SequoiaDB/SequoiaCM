package org.sequoiacm.om.omserver.test;

import java.io.IOException;
import java.net.URLDecoder;

import javax.annotation.PostConstruct;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.feign.ScmFeignDecoder;
import com.sequoiacm.infrastructure.feign.ScmFeignErrorDecoder;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.om.omserver.core.ScmOmPasswordMgr;
import com.sequoiacm.om.omserver.core.ScmOmPasswordMgrImpl;

import feign.Feign;
import feign.Response;
import feign.Retryer;
import feign.form.spring.SpringFormEncoder;

@SpringBootTest(classes = OmServerTest.class)
public class OmServerTest {

    private static final Logger logger = LoggerFactory.getLogger(OmServerTest.class);

    @Autowired
    private ScmOmTestConfig config;

    @Autowired
    ScmOmClient client;

    @Autowired
    ClientRespChecker checker;

    @PostConstruct
    public void initEnv() throws ScmFeignException {
        Response resp = client.dock(config.getGatewayAddr(), config.getMyRegion(),
                config.getMyZone(), config.getScmUser(), config.getScmPassword());
        try {
            checker.check(resp);
            logger.info("dock success:{}", resp.toString());
        }
        catch (Exception e) {
            logger.warn("failed to dock", e);
        }
    }

    @Bean
    public ScmOmPasswordMgr passwordMgr() {
        return new ScmOmPasswordMgrImpl();
    }

    @Bean
    public ScmSession session() throws ScmException {
        return ScmFactory.Session.createSession(
                new ScmConfigOption(config.getGatewayAddr().get(0) + "/" + config.getRootSite(),
                        config.getScmUser(), config.getScmSrcPassword()));
    }

    @Bean
    public ScmOmClient omClient(ScmFeignErrorDecoder decoder, ObjectMapper objMapper) {
        return Feign.builder().encoder(new SpringFormEncoder()).contract(new SpringMvcContract())
                .retryer(Retryer.NEVER_RETRY).errorDecoder(decoder)
                .decoder(new ScmFeignDecoder(objMapper, null))
                .target(ScmOmClient.class, "http://localhost:8080");
    }

    @Bean
    @ConfigurationProperties(prefix = "scm.omserver.test")
    public ScmOmTestConfig config() {
        return new ScmOmTestConfig();
    }

    @Bean
    public ScmFeignErrorDecoder decoder() {
        return new ScmFeignErrorDecoder();
    }

    @Bean
    public ClientRespChecker clientRespChecker(ScmFeignErrorDecoder errorDecoder) {
        return new ClientRespChecker(errorDecoder);
    }

    @Bean
    public ObjectMapper objMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(BSONObject.class, new JsonDeserializer<BSONObject>() {

            @Override
            public BSONObject deserialize(JsonParser p, DeserializationContext ctxt)
                    throws IOException, JsonProcessingException {
                JsonNode node = p.getCodec().readTree(p);
                String value = node.toString();
                String decodedValue = URLDecoder.decode(value, "UTF-8");
                BSONObject obj = (BSONObject) JSON.parse(decodedValue);
                return obj;
            }
        });

        mapper.registerModule(module);
        return mapper;
    }
}
