package com.sequoiacm.contentserver.config;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "scm")
@Configuration
public class CephS3DatasourceConfig {
    private Map<String, String> cephs3;

    public Map<String, String> getCephs3() {
        return cephs3;
    }

    public Map<String, String> getCephs3NotNull() {
        return cephs3 == null ? Collections.<String, String> emptyMap() : cephs3;
    }

    public void setCephs3(Map<String, String> cephs3) {
        this.cephs3 = cephs3;
    }
}
