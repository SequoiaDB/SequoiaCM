package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.sequoiacm.testcommon.TestScmBase;
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

/**
 * @descreption SCM-4827 :: 禁用版本控制，并发更新和获取对象列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4827 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4827";
    private String keyName = "/dir-1/bb/object4827";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024 * 2;
    private int updateSize = 1024 * 3;
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

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.SUSPENDED );
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        GetObjectThread getObjectThread = new GetObjectThread();
        UpdateObjectThread updateObjectThread = new UpdateObjectThread();
        te.addWorker(getObjectThread);
        te.addWorker(updateObjectThread);
        te.run();

        checkUpdateObjectResult( bucketName, keyName );

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
        }
    }

    private void checkUpdateObjectResult( String bucketName, String key )
            throws Exception {
        String downFileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, key );
        Assert.assertEquals( downFileMd5, TestTools.getMD5( updatePath ) );
    }

    private class UpdateObjectThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.putObject( bucketName, keyName,
                        new File( updatePath ) );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class GetObjectThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            S3ObjectInputStream s3is = null;
            try {
                S3Object object = s3Client.getObject( bucketName, keyName );
                long objectLength = object.getObjectMetadata().getContentLength();
                s3is = object.getObjectContent();
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                S3Utils.inputStream2File( s3is, downloadPath );
                s3is.close();
                String getObjectMd5 = TestTools.getMD5( downloadPath );
                if ( objectLength == fileSize ) {
                    Assert.assertEquals( getObjectMd5, TestTools.getMD5( filePath ) );
                } else {
                    Assert.assertEquals( getObjectMd5, TestTools.getMD5( updatePath ) );
                }
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
                if (s3is != null) {
                    s3is.close();
                }
            }
        }
    }
}
