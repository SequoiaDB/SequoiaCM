package com.sequoiacm.contentserver.config;

import com.sequoiacm.common.PropertiesDefine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.CommonDefine;

@Component
public class RootSiteMetaConfig {
    @Value("${" + PropertiesDefine.PROPERTY_ROOTSITE_URL_NEW + ":" + "${"
            + PropertiesDefine.PROPERTY_ROOTSITE_URL + ":}" + "}")
    private String url = CommonDefine.DefaultValue.ROOT_SITE_URL;
    @Value("${" + PropertiesDefine.PROPERTY_ROOTSITE_USER_NEW + ":" + "${"
            + PropertiesDefine.PROPERTY_ROOTSITE_USER + ":}" + "}")
    private String user = CommonDefine.DefaultValue.ROOT_SITE_USER;
    @Value("${" + PropertiesDefine.PROPERTY_ROOTSITE_PASSWD_NEW + ":" + "${"
            + PropertiesDefine.PROPERTY_ROOTSITE_PASSWD + ":}" + "}")
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
