package com.sequoiacm.datasource.metadata;

import com.sequoiacm.datasource.ScmDatasourceException;

import java.util.List;
import java.util.Map;

public class HadoopSiteUrl extends ScmSiteUrlWithConf {

    public HadoopSiteUrl(String dataSourceType, List<String> urlList, String user, String passwd,
            Map<String, String> dataConf) throws ScmDatasourceException {
        super(dataSourceType, urlList, user, passwd, dataConf);
    }

    @Override
    public String toString() {
        return "dataConf=" + this.getDataConf();
    }
}
