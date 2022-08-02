package com.sequoiacm.breakpointfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:upload and Get the same breakpoint file concurrently testlink
 * case:seqDB-1403
 * 
 * @author wuyan
 * @Date 2018.05.22
 * @version 1.00
 */

public class UploadAndGetFile1403 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    private String fileName = "breakpointfile1403";
    private int fileSize = 1024 * 1024 * 1;
    private int uploadedSize = 1024 * 800;
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;

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
        UploadBreakpointFileThread uploadBreakpointFile = new UploadBreakpointFileThread();
        GetBreakpointFileThread getBreakpointFile = new GetBreakpointFileThread();
        uploadBreakpointFile.start();
        getBreakpointFile.start();
        Assert.assertTrue( uploadBreakpointFile.isSuccess(),
                uploadBreakpointFile.getErrorMsg() );
        Assert.assertTrue( getBreakpointFile.isSuccess(),
                getBreakpointFile.getErrorMsg() );
        checkFileData();
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
                .createInstance( ws, fileName, ScmChecksumType.CRC32 );
        InputStream inputStream = new BreakpointStream( filePath,
                uploadedSize );
        breakpointFile.incrementalUpload( inputStream, false );
        inputStream.close();
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

    private class GetBreakpointFileThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .getInstance( ws, fileName );
                Assert.assertEquals( breakpointFile.getFileName(), fileName );
                Assert.assertEquals( breakpointFile.getChecksumType(),
                        ScmChecksumType.CRC32 );
                long uploadsize = breakpointFile.getUploadSize();
                if ( uploadsize < uploadedSize ) {
                    Assert.fail( "get size must be larger than uploaded size:"
                            + uploadsize );
                }

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