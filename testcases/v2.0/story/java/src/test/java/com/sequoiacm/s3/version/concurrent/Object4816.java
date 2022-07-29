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
 * @Description: SCM-4816 :: 开启版本控制，并发更新和删除相同对象
 * @author wuyan
 * @Date 2022.07.19
 * @version 1.00
 */
public class Object4816 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4816";
    private String keyName = "对象并发key4816";
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
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning(s3Client, bucketName, "Enabled");
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        CreateObject createObject = new CreateObject( keyName );
        DeleteObject deleteObject = new DeleteObject( keyName );
        es.addWorker( createObject );
        es.addWorker( deleteObject );
        es.run();
        checkUpdateAndDeleteObjectResult( bucketName, keyName );
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

        private DeleteObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
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

    private void checkUpdateAndDeleteObjectResult( String bucketName,
            String keyName ) throws Exception {
        boolean isExistObject = s3Client.doesObjectExist( bucketName, keyName );
        String currentVersion = "3.0";
        String historyVersion1 = "2.0";
        String historyVersion2 = "1.0";
        if ( isExistObject ) {
            // 最新版本为更新对象版本
            S3Object object = s3Client.getObject( bucketName, keyName );
            Assert.assertEquals( object.getObjectMetadata().getVersionId(),
                    currentVersion );
            checkObjectResult( currentVersion, updatePath );
            // 历史版本v2为删除标记对象
            GetObjectRequest request = new GetObjectRequest( bucketName,
                    keyName, historyVersion1 );
            try {
                s3Client.getObject( request );
                Assert.fail( "get object with deleteMarker should be fail!" );
            } catch ( AmazonS3Exception e ) {
                Assert.assertEquals( e.getErrorCode(), "MethodNotAllowed" );
            }
            // 历史版本v1为新增对象
            checkObjectResult( historyVersion2, filePath );
        } else {
            // 最新版本v3为删除标记对象
            GetObjectRequest request = new GetObjectRequest( bucketName,
                    keyName, currentVersion );
            try {
                s3Client.getObject( request );
                Assert.fail( "get object with deleteMarker should be fail!" );
            } catch ( AmazonS3Exception e ) {
                Assert.assertEquals( e.getErrorCode(), "MethodNotAllowed" );
            }
            // 历史版本v2为更新对象
            checkObjectResult( historyVersion1, updatePath );
            // 历史版本v1为新增对象
            checkObjectResult( historyVersion2, filePath );
        }
    }
}
