package com.sequoiacm.contentserver.contentmodule;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scm.content-module")
public class ContentModuleConfig {

    private String site;

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    @Override
    public String toString() {
        return "ContentModuleConfig{" + "site='" + site + '\'' + '}';
    }
}
