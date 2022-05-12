package com.sequoiacm.s3.object;

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
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * @Descreption SCM-3295:指定ifUnModifiedSince条件，不带versionId获取对象
 * @Author YiPan
 * @Date 2020/3/11
 */
public class GetObject3295 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3295";
    private String objectName = "object3295";
    private AmazonS3 s3Client = null;
    private int fileSize = 1;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();
    private List< PutObjectResult > objectVSList = new ArrayList< PutObjectResult >();
    private int fileNum = 5;
    private Calendar cal = Calendar.getInstance();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        String filePath = null;
        for ( int i = 0; i < fileNum; i++ ) {
            filePath = localPath + File.separator + "localFile_"
                    + ( fileSize + i ) + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize + i );
            filePathList.add( filePath );
        }
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws Exception {
        // create multiple versions object in the bucket
        for ( int i = 0; i < fileNum; i++ ) {
            objectVSList.add( s3Client.putObject(
                    new PutObjectRequest( bucketName, objectName + i,
                            new File( filePathList.get( i ) ) ) ) );
        }

        // random version
        Random random = new Random();
        int randomIndex = random.nextInt( fileNum - 1 );

        // the object has been modified since now-one_month
        cal.set( Calendar.MONTH, cal.get( Calendar.MONTH ) - 1 );
        S3Object object = s3Client.getObject(
                new GetObjectRequest( bucketName, objectName + randomIndex )
                        .withUnmodifiedSinceConstraint( cal.getTime() ) );
        // AmazonS3 Java driver handles error,so it returns null
        Assert.assertNull( object );

        // the object has not been modified since now+one_year
        cal.set( Calendar.YEAR, cal.get( Calendar.YEAR ) + 1 );
        S3Object object1 = s3Client.getObject(
                new GetObjectRequest( bucketName, objectName + randomIndex )
                        .withUnmodifiedSinceConstraint( cal.getTime() ) );
        // check the content
        String filePath = filePathList.get( randomIndex );
        System.out.println( filePath );
        checkResult( object1, filePath );
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
