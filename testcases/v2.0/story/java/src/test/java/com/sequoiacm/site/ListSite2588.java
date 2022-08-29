package com.sequoiacm.site;

import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description: SCM-2588 :: 通过注册中心获取站点列表
 * @author fanyu
 * @Date:2019年09月03日
 * @version:1.0
 */
public class ListSite2588 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        site = ScmInfo.getSite();
    }

    @Test(groups = { GroupTags.base })
    private void testListNodes() throws Exception {
        List< SiteWrapper > siteWrappers = ScmInfo.getAllSites();
        ScmSession session = null;
        List< String > sites = null;
        try {
            session = TestScmTools.createSession( site );
            sites = ScmSystem.ServiceCenter.getSiteList( session );
            Assert.assertEquals( sites.size(), siteWrappers.size() );
            for ( SiteWrapper siteWrapper : siteWrappers ) {
                Assert.assertEquals(
                        sites.contains( siteWrapper.getSiteName() ), true );
            }
        } catch ( AssertionError e ) {
            throw new Exception( "sites = " + sites.toString()
                    + "\n, siteWrappers = " + siteWrappers.toString(), e );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test(groups = { GroupTags.base })
    private void testSessionIsNull() throws Exception {
        try {
            ScmSystem.ServiceCenter.getSiteList( null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test(groups = { GroupTags.base })
    private void testSessionIsClosed() throws Exception {
        ScmSession session = TestScmTools.createSession( site );
        session.close();
        try {
            ScmSystem.ServiceCenter.getSiteList( session );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }
}
