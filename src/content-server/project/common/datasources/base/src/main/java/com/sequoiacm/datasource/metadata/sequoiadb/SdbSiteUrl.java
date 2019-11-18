package com.sequoiacm.datasource.metadata.sequoiadb;

import java.util.List;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.net.ConfigOptions;

public class SdbSiteUrl extends ScmSiteUrl {
    private ConfigOptions config;
    private DatasourceOptions datasourceOption;

    public SdbSiteUrl(String dataSourceType, List<String> urlList, String user, String passwd,
            ConfigOptions config, DatasourceOptions datasourceOption)
            throws ScmDatasourceException {
        super(dataSourceType, urlList, user, passwd);

        this.config = config;
        this.datasourceOption = datasourceOption;
    }

    public ConfigOptions getConfig() {
        return config;
    }

    public DatasourceOptions getDatasourceOption() {
        return datasourceOption;
    }
}
