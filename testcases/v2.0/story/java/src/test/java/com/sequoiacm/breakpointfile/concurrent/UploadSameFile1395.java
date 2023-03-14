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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:upload the same breakpoint file concurrently testlink
 * case:seqDB-1395
 * 
 * @author wuyan
 * @Date 2018.05.22
 * @version 1.00
 */

public class UploadSameFile1395 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    private String fileName = "breakpointfile1395";
    private int fileSize = 1024 * 500;
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;
    private AtomicInteger sameUploadOKCount = new AtomicInteger( 0 );

    @BeforeClass
    private void setUp() {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
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
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        UploadBreakpointFileThread uploadBreakpointFile = new UploadBreakpointFileThread();
        uploadBreakpointFile.start( 20 );
        Assert.assertTrue( uploadBreakpointFile.isSuccess(),
                uploadBreakpointFile.getErrorMsg() );
        // upload the same breakpointfile only one success
        int expSuccessNum = 1;
        Assert.assertEquals( sameUploadOKCount.get(), expSuccessNum );
        checkFileData();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( ws, fileId, true );
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.removeFile( downloadPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
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
    }

    private class UploadBreakpointFileThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .createInstance( ws, fileName,
                                ScmChecksumType.ADLER32 );
                breakpointFile.upload( new File( filePath ) );

                // recorded the numbers of upload file successful
                sameUploadOKCount.getAndIncrement();
            } catch ( ScmException e ) {
                if ( ScmError.FILE_EXIST != e.getError() ) {
                    Assert.assertTrue( false, "same file upload fail "
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