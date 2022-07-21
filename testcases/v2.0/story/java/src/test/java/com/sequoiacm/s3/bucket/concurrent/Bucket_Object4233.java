package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
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
 * @descreption SCM-4233 :: S3接口删除桶和SCM API创建S3文件并发
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket_Object4233 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4233";
    private String key = "aa/bb/object4233";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;
    private ScmId fileId;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        DeleteS3Bucket t1 = new DeleteS3Bucket();
        CreateScmFile t2 = new CreateScmFile();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();

        // delete bucket failed, bucket is exist and the key is exist
        if ( t1.getRetCode() != 0 ) {
            Assert.assertEquals( t2.getRetCode(), 0 );
            Assert.assertTrue( s3Client.doesBucketExistV2( bucketName ) );
            Assert.assertTrue( s3Client.doesObjectExist( bucketName, key ) );
        }

        // delete bucket success, create file success, file is exist
        if ( ( 0 == t1.getRetCode() ) && ( 0 == t2.getRetCode() ) ) {
            Assert.assertFalse( s3Client.doesBucketExistV2( bucketName ) );
            Assert.assertFalse( s3Client.doesObjectExist( bucketName, key ) );
            ScmFactory.File.getInstance( ws, fileId );
        }

        // delete bucket success, re create bucket success, create file success
        if ( 0 == t1.getRetCode() ) {
            ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
            ScmFile file = bucket.createFile( key );
            file.setFileName( key );
            file.setContent( filePath );
            fileId = file.save();
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    class DeleteS3Bucket extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                // delete bucket 延迟一会，否则delete bucket总会先执行成功
                Thread.sleep( 10 );
                s3Client.deleteBucket( bucketName );
            } catch ( AmazonS3Exception e ) {
                Assert.assertEquals( e.getErrorCode(), "BucketNotEmpty" );
                saveResult( e.getStatusCode(), e );
            }
        }
    }

    class CreateScmFile extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            ScmFile file = bucket.createFile( key );
            try {
                file.setFileName( key );
                file.setAuthor( "author4233" );
                file.setContent( filePath );

                fileId = file.save();
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError().getErrorType(),
                        "BUCKET_NOT_EXISTS" );
                saveResult( e.getErrorCode(), e );
            }
        }
    }

}
