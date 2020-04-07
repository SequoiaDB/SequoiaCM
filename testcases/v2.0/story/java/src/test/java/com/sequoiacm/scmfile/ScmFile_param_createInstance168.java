package com.sequoiacm.scmfile;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-168:createInstance参数校验
 * @author huangxiaoni init
 * @date 2017.4.12
 */

public class ScmFile_param_createInstance168 extends TestScmBase {
    private static SiteWrapper site = null;
    private static ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void TestNotExitWS() {
        boolean rst = false;
        try {
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( "abc", session );
            ScmFactory.File.createInstance( ws );
            rst = true;
            Assert.assertFalse( rst,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void TestWsIsNull() {
        try {
            ScmFile file = ScmFactory.File.createInstance( null );
            file.setFileName( "a" );
            file.setMimeType( "text/plain" );
            file.setTitle( "a" );
            file.save();
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            session.close();
        } catch ( BaseException e ) {
            Assert.fail( e.getMessage() );
        } finally {

        }
    }
}
