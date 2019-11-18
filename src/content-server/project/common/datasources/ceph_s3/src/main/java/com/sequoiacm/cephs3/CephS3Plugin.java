package com.sequoiacm.cephs3;

import com.sequoiacm.cephs3.dataoperation.CephS3DataOpFactoryImpl;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.datasource.DatasourcePlugin;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;

public class CephS3Plugin implements DatasourcePlugin {

    @Override
    public ScmService createService(int siteId, ScmSiteUrl siteUrl) throws CephS3Exception {
        return new CephS3DataService(siteId, siteUrl);
    }

    @Override
    public ScmDataOpFactory createDataOpFactory() {
        return new CephS3DataOpFactoryImpl();
    }
}
