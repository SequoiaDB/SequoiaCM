package com.sequoiacm.breakpointfile.concurrent;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:delete the same breakpoint file concurrently testlink
 * case:seqDB-1398
 * 
 * @author wuyan
 * @Date 2018.05.22
 * @version 1.00
 */

public class DeleteSameBreakpointFile1398 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "breakpointfile1398";
    private int fileSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private AtomicInteger sameDeleteOKCount = new AtomicInteger( 0 );

    @BeforeClass
    private void setUp() {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = DBSites.get( new Random().nextInt( DBSites.size() ) );
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            createBreakPointFile();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {

        DeleteBreakpointFileThread deleteBreakpointFile = new DeleteBreakpointFileThread();
        deleteBreakpointFile.start( 10 );
        Assert.assertTrue( deleteBreakpointFile.isSuccess(),
                deleteBreakpointFile.getErrorMsg() );
        // delete the same breakpointfile only one success
        int expSuccessNum = 1;
        Assert.assertEquals( sameDeleteOKCount.get(), expSuccessNum );
        checkResult();
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createBreakPointFile() throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, ScmChecksumType.ADLER32 );
        breakpointFile.upload( new File( filePath ) );
    }

    private void checkResult() throws Exception {
        // the breakpointfile is not exist
        try {
            ScmFactory.BreakpointFile.getInstance( ws, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                Assert.fail( "expErrorCode:-262  actError:" + e.getError()
                        + e.getMessage() );
            }
        }
    }

    private class DeleteBreakpointFileThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName );

                // recorded the numbers of upload file successful
                sameDeleteOKCount.getAndIncrement();
            } catch ( ScmException e ) {
                if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                    Assert.fail( "expErrorCode:-262  actError:"
                            + e.getErrorCode() + ":" + e.getMessage() );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}