package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.client.core.*;
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
 * @Descreption SCM-4232:SCM API删除桶和S3接口写对象并发
 * @Author YiPan
 * @CreateDate 2022/5/16
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4232 extends TestScmBase {
    private final String bucketName = "bucket4232";
    private final String objectKey = "object4232";
    private AmazonS3 s3Client;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
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

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        // 创建桶
        s3Client.createBucket( bucketName );

        // 上传文件和删除桶并发
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new ScmDeleteBucket() );
        te.addWorker( new S3PutObject() );
        te.run();

        // 如果桶不存在重新创建桶，上传原文件
        if ( !( s3Client.doesBucketExistV2( bucketName ) ) ) {
            s3Client.createBucket( bucketName );
            s3Client.putObject( bucketName, objectKey, new File( filePath ) );
        }
        // 下载文件校验数据
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        S3Object object = s3Client.getObject( bucketName, objectKey );
        S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private class ScmDeleteBucket {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getRootSite() );
            try {
                ScmFactory.Bucket.deleteBucket( session, bucketName );
            } catch ( ScmException e ) {
                if ( !e.getError().equals( ScmError.BUCKET_NOT_EMPTY ) ) {
                    throw e;
                }
            } finally {
                session.close();
            }
        }
    }

    private class S3PutObject {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.putObject( bucketName, objectKey,
                        new File( filePath ) );
            } catch ( AmazonS3Exception e ) {
                if ( !( e.getErrorCode().equals( "NoSuchBucket" ) ) ) {
                    throw e;
                }
            } finally {
                s3Client.shutdown();
            }
        }
    }
}