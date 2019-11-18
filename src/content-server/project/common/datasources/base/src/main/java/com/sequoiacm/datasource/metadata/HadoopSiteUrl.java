package com.sequoiacm.datasource.metadata;

import java.util.List;
import java.util.Map;

import com.sequoiacm.datasource.ScmDatasourceException;

public class HadoopSiteUrl extends ScmSiteUrl {

    private Map<String, String> dataConf;

    public HadoopSiteUrl(String dataSourceType, List<String> urlList, String user, String passwd,
            Map<String, String> dataConf) throws ScmDatasourceException {
        super(dataSourceType, urlList, user, passwd);
        this.dataConf = dataConf;
    }

    public Map<String, String> getDataConf() {
        return dataConf;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("dataConf=" + dataConf.toString());
        return sb.toString();
    }
}
