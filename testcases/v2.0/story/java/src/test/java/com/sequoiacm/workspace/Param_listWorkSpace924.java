/**
 *
 */
package com.sequoiacm.workspace;

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
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description:有效参数：ss存在（基本功能已覆盖） 无效参数：ss不存在、null
 * @author fanyu
 * @Date:2017年9月20日
 * @version:1.0
 */

public class Param_listWorkSpace924 extends TestScmBase {
    private static SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        site = ScmInfo.getSite();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testSSIsNull() {
        try {
            ScmFactory.Workspace.listWorkspace( null );
            Assert.fail( "expect result is fail but actual is success, when "
                    + "session is null." );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT.getErrorCode()
                    || !e.getMessage().contains( "session is null" ) ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testSSNoExist() {
        ScmSession session;
        try {
            session = TestScmTools.createSession( site );
            session.close();
            ScmFactory.Workspace.listWorkspace( session );
            Assert.fail( "expect result is fail but actual is success, when "
                    + "session is unexist." );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.SESSION_CLOSED.getErrorCode() ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }
}
