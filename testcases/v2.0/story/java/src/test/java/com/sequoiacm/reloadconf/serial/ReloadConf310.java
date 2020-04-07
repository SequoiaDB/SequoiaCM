package com.sequoiacm.reloadconf.serial;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @FileName SCM-310: reloadConf无效参数校验
 * @Author linsuqiang
 * @Date 2017-05-24
 * @Version 1.00
 */

/*
 * 1、reloadConf接口无效参数校验： scopeType为null id：不存在的id、负数 session为null； 2、检查执行结果；
 */

public class ReloadConf310 extends TestScmBase {
    private static SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        site = ScmInfo.getSite();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void reloadWithNullSession() throws Exception {
        ScmSession session = null;
        try {
            ScmSystem.Configuration
                    .reloadBizConf( ServerScope.ALL_SITE, site.getSiteId(),
                            null );
            Assert.fail(
                    "shouldn't succeed when parameter 'session' is null! " );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.INVALID_ARGUMENT.getErrorCode(), e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void reloadWithNullScopeType() throws Exception {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmSystem.Configuration
                    .reloadBizConf( null, site.getSiteId(), session );
            Assert.fail(
                    "shouldn't succeed when parameter 'ScopeType' is null! " );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.INVALID_ARGUMENT.getErrorCode(), e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void reloadWithNotExistSiteId() throws Exception {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            int invalidId = -1;
            ScmSystem.Configuration
                    .reloadBizConf( ServerScope.SITE, invalidId, session );
            Assert.fail( "shouldn't succeed when parameter 'siteId' is -1! " );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.SITE_NOT_EXIST.getErrorCode(), e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {

    }

}