package com.sequoiacm.cephswift;

import com.sequoiacm.cephswift.dataoperation.CephSwiftDataOpFactoryImpl;
import com.sequoiacm.cephswift.dataservice.CephSwiftDataService;
import com.sequoiacm.datasource.DatasourcePlugin;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;

public class CephSwiftPlugin implements DatasourcePlugin {

    @Override
    public ScmService createService(int siteId, ScmSiteUrl siteUrl) throws CephSwiftException {
        return new CephSwiftDataService(siteId, siteUrl);
    }

    @Override
    public ScmDataOpFactory createDataOpFactory() {
        return new CephSwiftDataOpFactoryImpl();
    }
}
