package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description: SCM-4854 :: 并发更新和获取相同文件
 * @author wuyan
 * @Date 2022.07.22
 * @version 1.00
 */
public class ScmFile4854 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4854";
    private String fileName = "scmfile4854";
    private ScmId fileId = null;
    private int fileSize = 1024 * 2;
    private int updateSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "localFile_" + updateSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, updateSize );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = S3Utils.createFile( scmBucket, fileName, filePath );
    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        UpdateFile updateFile = new UpdateFile();
        GetFile GetFile = new GetFile();
        es.addWorker( updateFile );
        es.addWorker( GetFile );
        es.run();

        checkUpdateResult();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( session, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class UpdateFile {

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                ScmFile file = scmBucket.getFile( fileName );
                file.updateContent( updatePath );
            } finally {
                session.close();
            }
        }
    }

    private class GetFile {
        private ScmFile file = null;
        ScmSession session = null;

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                file = scmBucket.getFile( fileName );
            } catch ( ScmException e ) {
                // -262:"FILE_NOT_FOUND"
                if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND
                        .getErrorCode() ) {
                    throw e;
                }
            }
        }

        @ExecuteOrder(step = 2)
        private void checkGetFileResult() throws Exception {
            try {
                // 如果获取文件成功，则校验获取文件结果
                if ( file != null ) {
                    if ( file.getMajorVersion() == 1 ) {
                        // 获取第一次创建V1版本文件
                        Assert.assertEquals( file.getFileId(), fileId );
                        String downloadPath = TestTools.LocalFile
                                .initDownloadPath( localPath,
                                        TestTools.getMethodName(),
                                        Thread.currentThread().getId() );
                        file.getContent( downloadPath );
                        Assert.assertEquals( TestTools.getMD5( filePath ),
                                TestTools.getMD5( downloadPath ) );
                    } else {
                        // 获取更新后的版本V2文件
                        Assert.assertEquals( file.getMajorVersion(), 2 );
                        Assert.assertEquals( file.getFileId(), fileId );
                        String downloadPath = TestTools.LocalFile
                                .initDownloadPath( localPath,
                                        TestTools.getMethodName(),
                                        Thread.currentThread().getId() );
                        file.getContent( downloadPath );
                        Assert.assertEquals( TestTools.getMD5( updatePath ),
                                TestTools.getMD5( downloadPath ) );
                    }
                }
            } finally {
                session.close();
            }
        }
    }

    private void checkUpdateResult() throws Exception {
        ScmFile file = scmBucket.getFile( fileName );
        Assert.assertEquals( file.getMajorVersion(), 2 );
        Assert.assertEquals( file.getFileId(), fileId );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( updatePath ),
                TestTools.getMD5( downloadPath ) );

    }
}
