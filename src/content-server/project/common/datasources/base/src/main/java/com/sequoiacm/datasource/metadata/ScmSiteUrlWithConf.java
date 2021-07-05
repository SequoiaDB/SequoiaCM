package com.sequoiacm.datasource.metadata;

import com.sequoiacm.datasource.ScmDatasourceException;

import java.util.List;
import java.util.Map;

public class ScmSiteUrlWithConf extends ScmSiteUrl {
    private final Map<String, String> conf;

    public ScmSiteUrlWithConf(String dataSourceType, List<String> urlList, String user,
            String passwd, Map<String, String> conf) throws ScmDatasourceException {
        super(dataSourceType, urlList, user, passwd);
        this.conf = conf;
    }

    public Map<String, String> getDataConf() {
        return conf;
    }
}
