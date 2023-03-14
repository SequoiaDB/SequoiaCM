package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Descreption SCM-4245:并发关联文件和解除文件关联
 * @Author YiPan
 * @CreateDate 2022/5/13
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4245 extends TestScmBase {
    private final String bucketName = "bucket4245";
    private final String objectKey = "object4245";
    private ScmSession session;
    private AmazonS3 s3Client;
    private ScmWorkspace ws;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private ScmId fileID = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        SiteWrapper rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        // 创建桶
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();

        // 创建SCM文件
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( objectKey );
        file.setContent( filePath );
        fileID = file.save();

        // 并发关联和解除关联
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new AttachFile() );
        DetachFile detachFileThread = new DetachFile();
        te.addWorker( detachFileThread );
        te.run();

        if ( detachFileThread.getRetCode() == 0 ) {
            // 解除关联成功
            Assert.assertFalse(
                    s3Client.doesObjectExist( bucketName, objectKey ) );
        } else {
            // 解除关联失败
            ScmException e = ( ScmException ) detachFileThread.getThrowable();
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.FILE_NOT_FOUND.getErrorCode() );
            Assert.assertTrue(
                    s3Client.doesObjectExist( bucketName, objectKey ) );
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
                try {
                    ScmFactory.File.deleteInstance( ws, fileID, true );
                } catch ( ScmException e ) {
                    if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                        throw e;
                    }
                }
            }
        } finally {
            session.close();
            s3Client.shutdown();
        }
    }

    private class AttachFile {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = ScmSessionUtils
                    .createSession( ScmInfo.getRootSite() );
            try {
                ScmFactory.Bucket.attachFile( session, bucketName, fileID );
            } finally {
                session.close();
            }
        }
    }

    private class DetachFile extends ResultStore {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = ScmSessionUtils
                    .createSession( ScmInfo.getRootSite() );
            try {
                ScmFactory.Bucket.detachFile( session, bucketName, objectKey );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            } finally {
                session.close();
            }
        }
    }
}