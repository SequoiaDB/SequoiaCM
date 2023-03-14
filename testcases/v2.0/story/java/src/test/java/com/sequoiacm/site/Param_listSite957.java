/**
 *
 */
package com.sequoiacm.site;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @Description:listSite接口测试
 * @author fanyu
 * @Date:2017年11月9日
 * @version:1.0
 */
public class Param_listSite957 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testSessionIsNull() {
        try {
            ScmFactory.Site.listSite( null );
            Assert.fail( "can not lsitsite when session is null" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT
                    .getErrorCode() ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testSessionIsInvalid() {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( site );
            session.close();
            ScmFactory.Site.listSite( session );
            Assert.fail( "can not listsite when session was closed" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.SESSION_CLOSED.getErrorCode() ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() + session.getSessionId() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }
}
