package com.sequoiacm.datasource.metadata.sequoiadb;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.base.ConfigOptions;

public class SdbSiteUrl extends ScmSiteUrl {
    private ConfigOptions config;
    private DatasourceOptions datasourceOption;
    private SdbConfig sdbConfig;

    public SdbSiteUrl(String dataSourceType, List<String> urlList, String user, String passwd,
            ConfigOptions config, DatasourceOptions datasourceOption, SdbConfig sdbConfig)
            throws ScmDatasourceException {
        super(dataSourceType, urlList, user, passwd);

        this.config = config;
        this.datasourceOption = datasourceOption;
        this.sdbConfig = sdbConfig;
    }

    public SdbSiteUrl(String dataSourceType, List<String> urlList, String user, String passwd,
            ConfigOptions config, DatasourceOptions datasourceOption)
            throws ScmDatasourceException {
        this(dataSourceType, urlList, user, passwd, config, datasourceOption,
                null);
    }

    public ConfigOptions getConfig() {
        return config;
    }

    public DatasourceOptions getDatasourceOption() {
        return datasourceOption;
    }

    public SdbConfig getSdbConfig() {
        return sdbConfig;
    }
}
