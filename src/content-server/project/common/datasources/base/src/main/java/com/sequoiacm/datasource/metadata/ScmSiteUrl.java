package com.sequoiacm.datasource.metadata;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.datasource.ScmDatasourceException;

public class ScmSiteUrl {
    private static final Logger logger = LoggerFactory.getLogger(ScmSiteUrl.class);

    private String type;
    private List<String> urls;
    private String user = "";
    private String password = "";

    public ScmSiteUrl(String dataSourceType, List<String> urlList, String user, String passwd)
            throws ScmDatasourceException {
        this.type = dataSourceType;
        this.urls = urlList;
        this.user = user;
        this.password = passwd;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getUrls() {
        return urls;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ScmSiteUrl)) {
            return false;
        }

        ScmSiteUrl right = (ScmSiteUrl) o;
        return (this.type.equals(right.type) && this.user.equals(right.user)
                && CommonHelper.equals(this.urls, right.urls) && password.equals(right.password));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("url=" + urls);
        return sb.toString();
    }
}