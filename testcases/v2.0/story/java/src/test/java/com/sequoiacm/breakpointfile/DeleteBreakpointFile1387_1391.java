package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:delete breakpoint file 
 * testlink-case:SCM-1387/1391
 *
 * @author wuyan
 * @Date 2018.05.11
 * @version 1.00
 */

public class DeleteBreakpointFile1387_1391 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "breakpointfile1387";
    private ScmId fileId = null;

    @BeforeClass
    private void setUp() throws ScmException {
        BreakpointUtil.checkDBDataSource();
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        // testcase1387:delete breakpointfile
        createAndDeleteBreakpointFile();
        // testcase1391:delete breakpointfile of had created file
        DeleteBreakPointFileBySetFile();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( ws, fileId, true );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createAndDeleteBreakpointFile() throws ScmException {
        // create file
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        byte[] data = new byte[ 10 ];
        new Random().nextBytes( data );
        breakpointFile.upload( new ByteArrayInputStream( data ) );

        // delete file,than check the result
        ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
        try {
            ScmFactory.BreakpointFile.getInstance( ws, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                Assert.fail( "expErrorCode:-262  actError:" + e.getError() +
                        e.getMessage() );
            }
        }
    }

    private void DeleteBreakPointFileBySetFile() throws ScmException {
        // create file
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, ScmChecksumType.ADLER32 );
        byte[] data = new byte[ 10 ];
        new Random().nextBytes( data );
        breakpointFile.upload( new ByteArrayInputStream( data ) );

        // breakpointFile setContet file
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setContent( breakpointFile );
        fileId = file.save();

        // delete the breakpointfile fail
        try {
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                Assert.fail( "expErrorCode:-262  actError:" + e.getErrorCode() +
                        e.getMessage() );
            }
        }
    }
}