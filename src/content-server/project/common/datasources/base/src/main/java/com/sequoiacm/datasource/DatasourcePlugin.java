package com.sequoiacm.datasource;

import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;

public interface DatasourcePlugin {
    //make sure service is available, throw exception if service is unavailable.
    ScmService createService(int siteId, ScmSiteUrl siteUrl) throws ScmDatasourceException;

    ScmDataOpFactory createDataOpFactory() throws ScmDatasourceException;
}
