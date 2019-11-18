package com.sequoiacm.hdfs;

import com.sequoiacm.datasource.DatasourcePlugin;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.hdfs.dataoperation.HdfsDataOpFactoryImpl;
import com.sequoiacm.hdfs.dataservice.HdfsDataService;

public class HdfsPlugin implements DatasourcePlugin {

    @Override
    public ScmService createService(int siteId, ScmSiteUrl siteUrl) throws HdfsException {
        return new HdfsDataService(siteId, siteUrl);
    }

    @Override
    public ScmDataOpFactory createDataOpFactory() throws HdfsException {
        return new HdfsDataOpFactoryImpl();
    }

}
