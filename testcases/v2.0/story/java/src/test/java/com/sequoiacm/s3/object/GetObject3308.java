package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @Descreption SCM-3308:指定ifNoneMatch/ifMatch/ifModifiedSince/ifNoneModifiedSince条件获取对象（标准模式）
 * @Author YiPan
 * @Date 2020/3/11
 */
public class GetObject3308 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3308";
    private String objectName = "object3308";
    private AmazonS3 s3Client = null;
    private int fileSize = 3;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();
    private List< PutObjectResult > objectVSList = new ArrayList< PutObjectResult >();
    private int fileNum = 2;

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

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        List< Calendar > time = new ArrayList< Calendar >();
        // create multiple versions object in the bucket
        for ( int i = 0; i < fileNum; i++ ) {
            objectVSList
                    .add( s3Client.putObject( new PutObjectRequest( bucketName,
                            objectName, new File( filePathList.get( i ) ) ) ) );
            time.add( Calendar.getInstance() );
        }

        // get eTag
        String eTag = objectVSList.get( fileNum - 1 ).getETag();

        // time B
        Date modified = new Date();
        Calendar calMod = time.get( 0 );
        calMod.set( Calendar.MONTH, calMod.get( Calendar.MONTH ) - 1 );
        calMod.setTime( modified );

        // time A
        Date unModified = new Date();
        Calendar calUnMod = time.get( fileNum - 1 );
        calUnMod.set( Calendar.MONTH, calMod.get( Calendar.MONTH ) + 1 );
        calUnMod.setTime( unModified );

        // get object by
        // matchingETag/nonMatchingETag/modifiedSince/unModifiedSince
        S3Object currObject = s3Client
                .getObject( new GetObjectRequest( bucketName, objectName )
                        .withMatchingETagConstraint( eTag )
                        .withNonmatchingETagConstraint( "wrong" )
                        .withModifiedSinceConstraint( modified )
                        .withUnmodifiedSinceConstraint( unModified ) );

        // check the eTag and the content of object
        String currPath = filePathList.get( fileNum - 1 );
        chectResult( currObject, currPath );
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

    private void chectResult( S3Object object, String filePath )
            throws Exception {
        Assert.assertEquals( object.getObjectMetadata().getETag(),
                TestTools.getMD5( filePath ) );
        S3ObjectInputStream s3InputStream = null;
        try {
            s3InputStream = object.getObjectContent();
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            S3Utils.inputStream2File( s3InputStream, downloadPath );
            Assert.assertEquals( TestTools.getMD5( downloadPath ),
                    TestTools.getMD5( filePath ) );
        } finally {
            if ( s3InputStream != null ) {
                s3InputStream.close();
            }
        }
    }

}
