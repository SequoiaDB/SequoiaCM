package com.sequoiacm.contentserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.CommonDefine;

@Component
@ConfigurationProperties(prefix = "scm.rootsite.meta")
public class RootSiteMetaConfig {
    private String url = CommonDefine.DefaultValue.ROOT_SITE_URL;
    private String user = CommonDefine.DefaultValue.ROOT_SITE_USER;
    private String password = CommonDefine.DefaultValue.ROOT_SITE_PASSWORD;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
