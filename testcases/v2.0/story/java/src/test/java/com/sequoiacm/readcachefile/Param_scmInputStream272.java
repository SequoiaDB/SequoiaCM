package com.sequoiacm.readcachefile;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Testcase:SCM-272 ScmInputStream参数校验 （A/B网络不通） 有效参数：文件存在（基本功能已覆盖）；
 *                   无效参数：文件不存在、null
 * @author fanyu init in 2017.05.10; huangxiaoni modify in 2017.6.6
 */

public class Param_scmInputStream272 extends TestScmBase {
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException {

        try {
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession();
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite", "star" })
    private void testParamIsNull() throws ScmException {
        ScmInputStream sis = null;
        try {
            sis = ScmFactory.File.createInputStream( null );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT
                    || !e.getMessage().contains( "scmFile is null" ) ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( sis != null ) {
                sis.close();
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite", "star" })
    private void testScmFileNotExist() throws ScmException {
        ScmInputStream sis = null;
        try {
            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws,
                    new ScmId( "a1ffb2ffc3ffd4ff56ffe7ff" ) );
            sis = ScmFactory.File.createInputStream( scmfile );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );

        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.FILE_NOT_FOUND.getErrorCode(), e.getMessage() );
        } finally {
            if ( sis != null ) {
                sis.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            //
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

}
