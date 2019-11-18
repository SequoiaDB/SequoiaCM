package com.sequoiacm.hbase;

import com.sequoiacm.datasource.DatasourcePlugin;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.hbase.dataoperation.HbaseDataOpFactoryImpl;
import com.sequoiacm.hbase.dataservice.HbaseDataService;

public class HbasePlugin implements DatasourcePlugin {

    @Override
    public ScmService createService(int siteId, ScmSiteUrl siteUrl) throws ScmDatasourceException {
        return new HbaseDataService(siteId, siteUrl);
    }

    @Override
    public ScmDataOpFactory createDataOpFactory() {
        return new HbaseDataOpFactoryImpl();
    }
}
