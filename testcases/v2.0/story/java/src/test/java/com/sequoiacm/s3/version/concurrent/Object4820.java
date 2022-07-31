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

/**
 * @Description: SCM-4820 :: 开启版本控制，并发删除和获取相同对象（指定相同版本）
 * @author wuyan
 * @Date 2022.07.20
 * @version 1.00
 */
public class Object4820 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4820";
    private String keyName = "对象%key4820";
    private int fileSize = 1024 * 1024 * 2;
    private int updateSize = 1024 * 3;
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
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
        s3Client.putObject( bucketName, keyName, new File( updatePath ) );
    }

    @Test
    public void testCreateObject() throws Exception {
        String deleteVersion = "2.0";
        ThreadExecutor es = new ThreadExecutor();
        DeleteObject deleteObject = new DeleteObject( keyName, deleteVersion );
        GetObject getObject = new GetObject( keyName, deleteVersion );
        es.addWorker( deleteObject );
        es.addWorker( getObject );
        es.run();

        // 检查指定删除版本对象已不存在
        try {
            s3Client.getObject( new GetObjectRequest( bucketName, keyName,
                    deleteVersion ) );
            Assert.fail( "exp fail but found success!" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchVersion" );
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

    private class DeleteObject {
        private String keyName;
        private String deleteVersion;

        private DeleteObject( String keyName, String deleteVersion ) {
            this.keyName = keyName;
            this.deleteVersion = deleteVersion;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.deleteVersion( bucketName, keyName, deleteVersion );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class GetObject {
        String keyName;
        String version;

        private GetObject( String keyName, String version ) {
            this.keyName = keyName;
            this.version = version;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                GetObjectRequest request = new GetObjectRequest( bucketName,
                        keyName, version );
                S3Object object = s3Client.getObject( request );
                checkGetObjectResult( object, bucketName, keyName, version );
            } catch ( AmazonS3Exception e ) {
                if ( !e.getErrorCode().equals( "NoSuchKey" )
                        && !e.getErrorCode().equals( "NoSuchVersion" ) ) {
                    throw e;
                }
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private void checkGetObjectResult( S3Object object, String bucketName,
            String key, String version ) throws Exception {
        ObjectMetadata metadata = object.getObjectMetadata();
        String actVersionId = metadata.getVersionId();
        Assert.assertEquals( actVersionId, version );
        S3ObjectInputStream s3is = object.getObjectContent();
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        S3Utils.inputStream2File( s3is, downloadPath );
        s3is.close();
        String getObjectMd5 = TestTools.getMD5( downloadPath );
        Assert.assertEquals( getObjectMd5, TestTools.getMD5( updatePath ),
                "md5 is wrong!" );
    }
}
