package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description: SCM-4818 :: 并发更新和获取相同对象
 * @author wuyan
 * @Date 2022.07.19
 * @version 1.00
 */
public class Object4818 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4818";
    private String keyName = "对象%key4818";
    private int fileSize = 1024 * 1024 * 2;
    private int updateSize = 1024;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private AmazonS3 s3Client = null;

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
        s3Client.createBucket( bucketName );
        BucketVersioningConfiguration configuration = new BucketVersioningConfiguration()
                .withStatus( "Enabled" );
        SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest = new SetBucketVersioningConfigurationRequest(
                bucketName, configuration );
        s3Client.setBucketVersioningConfiguration(
                setBucketVersioningConfigurationRequest );
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        UpdateObject updateObject = new UpdateObject( keyName );
        GetObject getObject = new GetObject( keyName );
        es.addWorker( updateObject );
        es.addWorker( getObject );
        es.run();

        checkUpdateObjectResult( bucketName, keyName );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private class UpdateObject {
        private String keyName;

        private UpdateObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
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

    private class GetObject {
        String keyName;

        private GetObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                S3Object object = s3Client.getObject( bucketName, keyName );
                checkGetObjectResult( object, bucketName, keyName );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private void checkGetObjectResult( S3Object object, String bucketName,
            String key ) throws Exception {
        ObjectMetadata metadata = object.getObjectMetadata();
        String versionId = metadata.getVersionId();
        String curVersionId = "2.0";
        if ( versionId.equals( curVersionId ) ) {
            String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                    bucketName, key, versionId );
            Assert.assertEquals( downfileMd5, TestTools.getMD5( updatePath ) );
        } else {
            String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                    bucketName, key, versionId );
            Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
        }
    }

    private void checkUpdateObjectResult( String bucketName, String key )
            throws Exception {
        String newVersionId = "2.0";
        S3Object object = s3Client.getObject( bucketName, keyName );
        Assert.assertEquals( object.getObjectMetadata().getVersionId(),
                newVersionId );
        Assert.assertEquals( object.getBucketName(), bucketName );
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, key );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( updatePath ) );
    }
}
