package com.sequoiacm.s3.object;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3577:指定ifNoneMatch和ifUnModifiedSince条件匹配源对象复制
 * @Author huangxiaoni
 * @Date 2019.09.17
 */
public class CopyObject3577 extends TestScmBase {
    private boolean runSuccess = false;
    private AmazonS3 s3Client = null;
    private String srcBucketName = "bucket3577a";
    private String dstBucketName = "bucket3577b";
    private String keyName = "obj3577";
    private int fileSize = 5 * 1024 * 1024;
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;
    private String filePath3 = null;

    @BeforeClass
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath1 = localPath + File.separator + "localFile_" + fileSize
                + "_1.txt";
        filePath2 = localPath + File.separator + "localFile_" + fileSize
                + "_2.txt";
        filePath3 = localPath + File.separator + "localFile_" + fileSize
                + "_3.txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize );
        TestTools.LocalFile.createFile( filePath2, fileSize );
        TestTools.LocalFile.createFile( filePath3, fileSize );

        s3Client = S3Utils.buildS3Client();

        s3Client.createBucket( srcBucketName );
        s3Client.createBucket( dstBucketName );

        s3Client.putObject( srcBucketName, keyName, new File( filePath1 ) );
        s3Client.putObject( srcBucketName, keyName, new File( filePath2 ) );
    }

    @Test
    private void test() throws Exception {
        // get last modified date of the current version object
        GetObjectMetadataRequest metadataRequest = new GetObjectMetadataRequest(
                srcBucketName, keyName );
        ObjectMetadata objMetadata = s3Client
                .getObjectMetadata( metadataRequest );
        String srcObjHisVerETag = objMetadata.getETag();
        Date srcObjHisLastModDate = objMetadata.getLastModified();

        // put object after last modified date of current version object
        s3Client.putObject( srcBucketName, keyName, new File( filePath3 ) );

        // copy object
        CopyObjectRequest request = new CopyObjectRequest( srcBucketName,
                keyName, dstBucketName, keyName );
        request.withNonmatchingETagConstraint( srcObjHisVerETag );
        request.withUnmodifiedSinceConstraint( new Date(srcObjHisLastModDate.getTime() + 1000 * 600) );
        s3Client.copyObject( request );

        // check results
        checkObjectAttribute( filePath3 );
        checkObjectContent( filePath3 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client,srcBucketName );
                S3Utils.clearBucket( s3Client, dstBucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void checkObjectContent( String filePath ) throws Exception {
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                srcBucketName, keyName );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
    }

    private void checkObjectAttribute( String filePath )
            throws IOException {
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(
                srcBucketName, keyName );
        ObjectMetadata objMetadata = s3Client.getObjectMetadata( request );
        String expMd5 = TestTools.getMD5( filePath );
        Assert.assertEquals( objMetadata.getETag(), expMd5 );
        Assert.assertEquals( objMetadata.getContentLength(), fileSize );
    }
}
