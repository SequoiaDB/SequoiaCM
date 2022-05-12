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
import java.util.*;

/**
 * @Descreption SCM-3298:指定ifNoneMatch和ifUnModifiedSince条件，不带versionId获取对象（标准模式）
 * @Author YiPan
 * @Date 2020/3/11
 */
public class GetObject3298 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3298";
    private String objectName = "object3298";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 20;
    private int updateSize = 1024 * 15;
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
    }

    @Test
    public void test() throws Exception {
        s3Client.putObject( bucketName, objectName, new File( filePath ) );
        s3Client.putObject( bucketName, objectName, new File( updatePath ) );

        // set date one day later than current time
        long currentTimestamp = new Date().getTime();
        long timestamp = currentTimestamp + 96784000l;
        Date date = new Date( timestamp );
        String eTag = TestTools.getMD5( filePath );
        GetObjectRequest request = new GetObjectRequest( bucketName,
                objectName );
        request.withNonmatchingETagConstraint( eTag )
                .withUnmodifiedSinceConstraint( date );
        S3Object object = s3Client.getObject( request );

        checkGetObjectResult( object, updatePath );
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

    private void checkGetObjectResult( S3Object object, String filePath )
            throws Exception {
        S3ObjectInputStream s3is = object.getObjectContent();
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        S3Utils.inputStream2File( s3is, downloadPath );
        String getMd5 = TestTools.getMD5( downloadPath );
        Assert.assertEquals( getMd5, TestTools.getMD5( filePath ) );
    }
}
