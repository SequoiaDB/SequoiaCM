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
 * @descreption SCM-4641 :: 指定ifMatch和ifNoneMatch条件获取对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4641 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = null;
    private String objectName = "object4641";
    private AmazonS3 s3Client = null;
    private int fileSize = 3;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();
    private List< PutObjectResult > objectVSList = new ArrayList< PutObjectResult >();
    private int fileNum = 3;

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
        bucketName = enableVerBucketName;
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    private void test() throws Exception {
        // create multiple versions object in the bucket
        for ( int i = 0; i < fileNum; i++ ) {
            objectVSList
                    .add( s3Client.putObject( new PutObjectRequest( bucketName,
                            objectName, new File( filePathList.get( i ) ) ) ) );
        }

        // get history version eTag
        Random random = new Random();
        int histIndex = random.nextInt( fileNum - 1 );
        String histETag = objectVSList.get( histIndex ).getETag();

        // get current version eTag
        String currVersionId = objectVSList.get( fileNum - 1 ).getVersionId();
        String currETag = objectVSList.get( fileNum - 1 ).getETag();

        // get object by eTag
        S3Object currObject = s3Client.getObject(
                new GetObjectRequest( bucketName, objectName, currVersionId )
                        .withMatchingETagConstraint( currETag )
                        .withNonmatchingETagConstraint( histETag ) );

        // check the eTag and the content of object
        String currPath = filePathList.get( fileNum - 1 );
        checkResult( currObject, currPath );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        objectName );
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
