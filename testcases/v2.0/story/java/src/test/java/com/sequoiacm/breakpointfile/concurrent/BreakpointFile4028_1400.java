package com.sequoiacm.breakpointfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
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
 * @descreption SCM-4028:并发续传和删除同一断点文件 SCM-1400:并发续传和删除同一断点文件
 * @author YiPan
 * @date 2021/10/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BreakpointFile4028_1400 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    private String fileName = "file4028";
    private int fileSize = 1024 * 1024 * 50;
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;
    private boolean runSuccess = false;

    @BeforeClass(enabled = true)
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        createBreakPointFile();
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    private void test() throws Throwable {
        UploadBreakpointFileThread upload = new UploadBreakpointFileThread();
        DeleteBreakpointFileThread delete = new DeleteBreakpointFileThread();

        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( upload );
        t.addWorker( delete );
        t.run();

        if ( upload.getRetCode() == 0 ) {
            Assert.assertEquals( ScmError.INVALID_ARGUMENT.getErrorCode(),
                    delete.getRetCode() );
            checkFileData();
        } else if ( delete.getRetCode() == 0 ) {
            if ( ScmError.INVALID_ARGUMENT.getErrorCode() != upload.getRetCode()
                    && ScmError.FILE_NOT_FOUND.getErrorCode() != upload
                            .getRetCode()
                    && ScmError.HTTP_NOT_FOUND.getErrorCode() != upload
                            .getRetCode() ) {
                throw upload.getThrowable();
            }
            checkDeleteResult();
        } else {
            Assert.fail( "upload thread result code is " + upload.getRetCode()
                    + ",delete thread result code is " + delete.getRetCode() );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
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

    private void checkDeleteResult() throws ScmException {
        // the breakpointfile is not exist
        try {
            ScmFactory.BreakpointFile.getInstance( ws, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                throw e;
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

    private class DeleteBreakpointFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            try ( ScmSession session = TestScmTools.createSession( site )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
            } catch ( ScmException e ) {
                saveResult( e.getError().getErrorCode(), e );
            }
        }
    }

    private class UploadBreakpointFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            try ( ScmSession session = TestScmTools.createSession( site )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .getInstance( ws, fileName );
                breakpointFile.upload( new File( filePath ) );
            } catch ( ScmException e ) {
                saveResult( e.getError().getErrorCode(), e );
            }
        }
    }

}