package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
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
import java.io.IOException;

/**
 * @Description: SCM-4817 :: 开启版本控制，并发指定不同版本更新和删除相同对象
 * @author wuyan
 * @Date 2022.07.19
 * @version 1.00
 */
public class Object4817 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4817";
    private String keyName = "对象并发key4817";
    private int fileSize = 1024 * 200;
    private int updateSize = 1024 * 2;
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
        S3Utils.clearBucket( s3Client, bucketName );
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
        String historyVersion = "1.0";
        String currentVersion = "2.0";
        ThreadExecutor es = new ThreadExecutor();
        CreateObject createObject = new CreateObject( keyName );
        DeleteObject deleteObject = new DeleteObject( keyName, historyVersion );
        es.addWorker( createObject );
        es.addWorker( deleteObject );
        es.run();

        S3Object object = s3Client.getObject( bucketName, keyName );
        String newVersion = object.getObjectMetadata().getVersionId();
        if ( newVersion.equals( currentVersion ) ) {
            // 查询历史版本已不存在
            GetObjectRequest request = new GetObjectRequest( bucketName,
                    keyName, historyVersion );
            try {
                s3Client.getObject( request );
                Assert.fail( "get object should be fail!" );
            } catch ( AmazonS3Exception e ) {
                Assert.assertEquals( e.getErrorCode(), "NoSuchVersion" );
            }
            // 检查更新版本信息正确
            checkObjectResult( currentVersion, updatePath );
        } else {
            // 如果先删除对象后再更新，则只有一个版本文件
            Assert.assertEquals( newVersion, historyVersion );
            checkObjectResult( historyVersion, updatePath );
            // 查询V2版本不存在
            GetObjectRequest request = new GetObjectRequest( bucketName,
                    keyName, currentVersion );
            try {
                s3Client.getObject( request );
                Assert.fail( "get object should be fail!" );
            } catch ( AmazonS3Exception e ) {
                Assert.assertEquals( e.getErrorCode(), "NoSuchVersion" );
            }
        }

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

    private class CreateObject {
        private String keyName;

        private CreateObject( String keyName ) {
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

    private class DeleteObject {
        String keyName;
        String versionId;

        private DeleteObject( String keyName, String versionId ) {
            this.keyName = keyName;
            this.versionId = versionId;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.deleteVersion( bucketName, keyName, versionId );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private void checkObjectResult( String expVersionId, String filePath )
            throws IOException {
        GetObjectRequest request = new GetObjectRequest( bucketName, keyName,
                expVersionId );
        S3Object object = s3Client.getObject( request );
        Assert.assertEquals( object.getObjectMetadata().getVersionId(),
                expVersionId );
        Assert.assertEquals( object.getObjectMetadata().getETag(),
                TestTools.getMD5( filePath ) );
    }
}
