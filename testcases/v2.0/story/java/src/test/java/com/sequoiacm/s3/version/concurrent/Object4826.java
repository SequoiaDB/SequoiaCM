package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
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
 * @descreption SCM-4826 :: 禁用版本控制，并发更新和删除相同对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4826 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4826";
    private String keyName = "key4826";
    private String content = "content4826";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024 * 4;
    private int updateSize = 1024 * 1024 * 3;
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
        S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        UpdateObjectThread updateObject = new UpdateObjectThread();
        DeleteObjectThread deleteObject = new DeleteObjectThread();
        te.addWorker(updateObject);
        te.addWorker(deleteObject);
        te.run();

        if ( s3Client.doesObjectExist( bucketName, keyName ) ) {
            ObjectMetadata object = s3Client.getObjectMetadata( bucketName,
                    keyName );
            Assert.assertEquals( object.getETag(),
                    TestTools.getMD5( updatePath ) );
            Assert.assertEquals( object.getVersionId(), "null" );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket(s3Client, bucketName);
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private class UpdateObjectThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.putObject( bucketName, keyName, new File( updatePath ) );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class DeleteObjectThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.deleteObject( bucketName, keyName );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
