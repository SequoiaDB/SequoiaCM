package com.sequoiacm.common;

import java.util.Arrays;

import com.sequoiacm.contentserver.datasourcemgr.ScmDataSourceType;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.pipeline.file.TestDefine;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbSiteUrl;
import com.sequoiacm.exception.ScmServerException;

public class TestFactory {

    private static TestFactory INSTANCE = null;

    private ScmMetaService metaService;

    private TestFactory() throws ScmDatasourceException, ScmServerException {
        // 准备元数据服务（SCM 内部流程所需）
        ScmSiteUrl siteUrl = new SdbSiteUrl(ScmDataSourceType.SEQUOIADB.getName(),
                Arrays.asList(ScmTestBase.SDB_URL), ScmTestBase.SDB_USER, ScmTestBase.SDB_PASSWD,
                null, null);
        metaService = new ScmMetaService(TestDefine.ROOT_SITE_ID, siteUrl);
    }

    public static TestFactory getInstance() throws ScmServerException, ScmDatasourceException {
        if (INSTANCE == null) {
            synchronized (TestFactory.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TestFactory();
                }
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (INSTANCE.metaService != null) {
                            INSTANCE.metaService.close();
                        }
                    }
                }));
            }
        }
        return INSTANCE;
    }

    public ScmMetaService getMetaService() {
        return this.metaService;
    }
}
