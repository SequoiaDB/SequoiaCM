package com.sequoiacm.s3.object;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3284:create object
 * @author wuyan
 * @Date 2018.11.6
 * @version 1.00
 */
public class CreateObject3284 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3284";
    private String[] keyNames = {"aa/maa/bb/object3284","aa/maa/cc/object3284","bb/object3284","cc/object3284" };
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client,bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void testCreateObject() throws Exception {
        for ( String keyName : keyNames ) {
            Date beforeDate = new Date();
            PutObjectResult result = s3Client
                    .putObject( bucketName, keyName, new File( filePath ) );

            checkObjectAttributeInfo( result, beforeDate, keyName );
            checkPutObjectResult( bucketName, keyName );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                for(String keyName : keyNames) {
                    s3Client.deleteObject( bucketName, keyName );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void checkPutObjectResult( String bucketName, String keyName) throws Exception {
        // down file
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
    }

    private void checkObjectAttributeInfo( PutObjectResult objAttrInfo,
            Date beforeDate,String keyName ) throws IOException {
        String expMd5 = TestTools.getMD5( filePath );
        Assert.assertEquals( objAttrInfo.getETag(), expMd5 );

        // check the attributeInfo of get object
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(
                bucketName, keyName );
        ObjectMetadata result = s3Client.getObjectMetadata( request );
        Date modifiedDate = result.getLastModified();
        Assert.assertEquals( result.getETag(), expMd5 );
        Assert.assertEquals( result.getContentLength(), fileSize );
        Assert.assertTrue(
                Math.abs( modifiedDate.getTime() - beforeDate.getTime() ) < 1000
                        * 60 * 60 );
    }
}
