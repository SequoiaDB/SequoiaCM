package com.sequoiacm.breakpointfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
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
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:upload and delete the same breakpoint file concurrently testlink
 * case:seqDB-1400
 * 
 * @author wuyan
 * @Date 2018.05.22
 * @version 1.00
 */

public class UploadAndDeleteFile1400 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    private String fileName = "breakpointfile1400";
    private int fileSize = 1024 * 1024 * 51;
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;

    @BeforeClass(enabled = true)
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        createBreakPointFile();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        UploadBreakpointFileThread uploadBreakpointFile = new UploadBreakpointFileThread();
        DeleteBreakpointFileThread deleteBreakpointFile = new DeleteBreakpointFileThread();
        uploadBreakpointFile.start();
        deleteBreakpointFile.start();
        if ( uploadBreakpointFile.isSuccess() ) {
            if ( !deleteBreakpointFile.isSuccess() ) {
                Assert.assertTrue( !deleteBreakpointFile.isSuccess(),
                        deleteBreakpointFile.getErrorMsg() );
                ScmException e = ( ScmException ) deleteBreakpointFile
                        .getExceptions().get( 0 );
                Assert.assertEquals( ScmError.INVALID_ARGUMENT, e.getError(),
                        "delete must fail:"
                                + deleteBreakpointFile.getErrorMsg() );
                checkFileData();
            } else {
                Assert.assertFalse( true, "delete must be fail" );
            }
        } else if ( !uploadBreakpointFile.isSuccess() ) {
            Assert.assertTrue( deleteBreakpointFile.isSuccess(),
                    deleteBreakpointFile.getErrorMsg() );
            ScmException e = ( ScmException ) uploadBreakpointFile
                    .getExceptions().get( 0 );

            if ( ScmError.INVALID_ARGUMENT != e.getError()
                    && ScmError.FILE_NOT_FOUND != e.getError()
                    && ScmError.HTTP_NOT_FOUND != e.getError() ) {
                Assert.assertFalse( true,
                        "upload should fail e:" + e.getErrorCode() + ":"
                                + uploadBreakpointFile.getErrorMsg() );
            }
            checkDeleteResult();
        }
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

    private void createBreakPointFile() throws ScmException, IOException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, ScmChecksumType.ADLER32 );
        InputStream inputStream = new BreakpointStream( filePath, 1024 * 51 );
        breakpointFile.incrementalUpload( inputStream, false );
        inputStream.close();
    }

    private void checkDeleteResult() throws Exception {
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

    private void checkFileData() throws Exception {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );

        // save to file, than down file check the file data
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        file.setTitle( fileName );
        fileId = file.save();

        // down file
        downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );

        // check results
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( downloadPath ) );

        // delete file
        ScmFactory.File.deleteInstance( ws, fileId, true );
        TestTools.LocalFile.removeFile( downloadPath );
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

            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UploadBreakpointFileThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .getInstance( ws, fileName );
                breakpointFile.upload( new File( filePath ) );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}