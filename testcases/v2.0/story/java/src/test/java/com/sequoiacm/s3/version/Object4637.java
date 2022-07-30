package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @descreption SCM-4637 :: 指定versionId为null，获取带null-marker标记的文件
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4637 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4637";
    private String objectName = "object4637";
    private AmazonS3 s3Client = null;
    private int versionNum = 3;

    private int fileSize = 1024 * 200;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();
    private List< PutObjectResult > objectVSList = new ArrayList< PutObjectResult >();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < versionNum; i++ ) {
            String filePath = localPath + File.separator + "localFile_"
                    + ( fileSize + i ) + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize + i );
            filePathList.add( filePath );
        }
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
        objectVSList.add( s3Client.putObject( bucketName, objectName,
                new File( filePathList.get( 0 ) ) ) );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.SUSPENDED );
        objectVSList.add( s3Client.putObject( bucketName, objectName,
                new File( filePathList.get( 1 ) ) ) );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
        objectVSList.add( s3Client.putObject( bucketName, objectName,
                new File( filePathList.get( 2 ) ) ) );
    }

    @Test
    private void test() throws Exception {
        int index = 1;
        String versionId = objectVSList.get( index ).getVersionId();
        S3Object obj = s3Client.getObject(
                new GetObjectRequest( bucketName, objectName, versionId ) );

        // check the Etag and the md5 of object content
        String path = filePathList.get( index );
        checkResult( obj, path );
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

    private void checkResult( S3Object object, String filePath )
            throws Exception {
        Assert.assertEquals( object.getObjectMetadata().getETag(),
                TestTools.getMD5( filePath ) );
        S3ObjectInputStream s3ObjectInputStream = null;
        try {
            s3ObjectInputStream = object.getObjectContent();
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            S3Utils.inputStream2File( s3ObjectInputStream, downloadPath );
            Assert.assertEquals( TestTools.getMD5( downloadPath ),
                    TestTools.getMD5( filePath ) );
        } finally {
            if ( s3ObjectInputStream != null ) {
                s3ObjectInputStream.close();
            }
        }
    }
}
