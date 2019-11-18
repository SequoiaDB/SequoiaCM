package com.sequoiacm.sequoiadb;

import com.sequoiacm.datasource.DatasourcePlugin;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.sequoiadb.dataopertion.SdbDataOpFactoryImpl;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;

public class SequoiadbPlugin implements DatasourcePlugin {

    @Override
    public ScmService createService(int siteId, ScmSiteUrl siteUrl) throws SequoiadbException {
        return new SdbDataService(siteId, siteUrl);
    }

    @Override
    public ScmDataOpFactory createDataOpFactory() {
        return new SdbDataOpFactoryImpl();
    }
}
