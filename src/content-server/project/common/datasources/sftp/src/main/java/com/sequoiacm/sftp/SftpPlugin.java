package com.sequoiacm.sftp;

import com.sequoiacm.datasource.DatasourcePlugin;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.sftp.dataopertion.SftpDataOpFactoryImpl;
import com.sequoiacm.sftp.dataservice.SftpDataService;

public class SftpPlugin implements DatasourcePlugin {

    @Override
    public ScmService createService(int siteId, ScmSiteUrl siteUrl) throws SftpDataException {
        return new SftpDataService(siteId, siteUrl);
    }

    @Override
    public ScmDataOpFactory createDataOpFactory() {
        return new SftpDataOpFactoryImpl();
    }
}
