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
 * @Description: SCM-4815 :: 开启版本控制，并发创建和删除相同对象
 * @author wuyan
 * @Date 2022.07.18
 * @version 1.00
 */
public class Object4815 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4815";
    private String keyName = "key4815";
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;
    private AmazonS3 s3Client = null;

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
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning(s3Client, bucketName, "Enabled");
    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        CreateObject createObject = new CreateObject( keyName );
        DeleteObject deleteObject = new DeleteObject( keyName );
        es.addWorker( createObject );
        es.addWorker( deleteObject );
        es.run();

        boolean isExistObject = s3Client.doesObjectExist( bucketName, keyName );
        if ( isExistObject ) {
            // 先执行删除对象操作，再执行创建相同对象操作
            String expVersionId = "2.0";
            checkObjectResult( expVersionId );
            // 获取历史版本中删除标记对象失败
            String historyVersion = "1.0";
            GetObjectRequest request = new GetObjectRequest( bucketName,
                    keyName, historyVersion );
            try {
                s3Client.getObject( request );
                Assert.fail( "get object with deleteMarker should be fail!" );
            } catch ( AmazonS3Exception e ) {
                Assert.assertEquals( e.getErrorCode(), "MethodNotAllowed" );
            }
        } else {
            // 创建对象成功后执行删除操作，当前版本新增删除标记对象
            String currentVersion = "2.0";
            String historyVersion = "1.0";
            try {
                GetObjectRequest request = new GetObjectRequest( bucketName,
                        keyName, currentVersion );
                s3Client.getObject( request );
                Assert.fail( "get object with deleteMarker should be fail!" );
            } catch ( AmazonS3Exception e ) {
                Assert.assertEquals( e.getErrorCode(), "MethodNotAllowed" );
            }
            checkObjectResult( historyVersion );

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
                s3Client.putObject( bucketName, keyName, new File( filePath ) );
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

    private void checkObjectResult( String expVersionId ) throws IOException {
        GetObjectRequest request = new GetObjectRequest( bucketName, keyName,
                expVersionId );
        S3Object object = s3Client.getObject( request );
        Assert.assertEquals( object.getObjectMetadata().getVersionId(),
                expVersionId );
        Assert.assertEquals( object.getObjectMetadata().getETag(),
                TestTools.getMD5( filePath ) );
    }
}
