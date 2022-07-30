package com.sequoiacm.s3.version.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * @descreption SCM-4864 :: 禁用版本控制，并发更新和获取文件列表
 * @author Zhaoyujing
 * @Date 2020/7/21
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4864 extends TestScmBase {
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4864";
    private String keyName = "aa/bb/object4864";
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 20;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;

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

        session = TestScmTools.createSession( ScmInfo.getSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.suspendVersionControl();
        S3Utils.createFile( bucket, keyName, filePath );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new GetFileThread() );
        te.addWorker( new UpdateFileThread() );
        te.run();

        checkFile( bucketName, keyName, updatePath );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFile( String bucketName, String objectName, String path )
            throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        ScmFile file = bucket.getFile( objectName );
        file.getMd5();
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        OutputStream fileOutputStream = new FileOutputStream( downloadPath );
        file.getContent( fileOutputStream );
        fileOutputStream.close();

        String updateMD5 = TestTools.getMD5( path );
        String downloadMD5 = TestTools.getMD5( downloadPath );
        Assert.assertEquals( updateMD5, downloadMD5 );

        Assert.assertTrue( file.isNullVersion() );
    }

    private class UpdateFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            ScmFile file = bucket.getFile( keyName );
            file.updateContent( updatePath );
        }
    }

    private class GetFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                ScmFile scmFile = bucket.getFile( keyName );

                Assert.assertTrue( scmFile.isNullVersion() );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                OutputStream fileOutputStream = new FileOutputStream(
                        downloadPath );
                scmFile.getContent( fileOutputStream );
                fileOutputStream.close();
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError().getErrorType(),
                        "FILE_NOT_FOUND" );
                saveResult( e.getErrorCode(), e );
            }
        }
    }
}
