package com.sequoiacm.s3.object;

import java.io.File;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3313:headObject请求查询对象
 * @author fanyu
 * @Date 2018.12.17
 * @version 1.00
 */
public class HeadObject3313 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3313";
    private String key = "test/object3313";
    private int fileSize1 = 1024 * 300;
    private int fileSize2 = 1024 * 100;
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath1 = localPath + File.separator + "localFile_" + fileSize1
                + ".txt";
        filePath2 = localPath + File.separator + "localFile_" + fileSize2
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize1 );
        TestTools.LocalFile.createFile( filePath2, fileSize2 );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );

    }

    @Test
    public void testCreateBucket() throws IOException {
        s3Client.putObject( bucketName, key, new File( filePath1 ) );
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(
                bucketName, key );
        ObjectMetadata result = s3Client.getObjectMetadata( request );
        checkObjectMetaInfo( result, filePath1, fileSize1 );

        s3Client.deleteObject( bucketName, key );
        try {
            s3Client.getObjectMetadata( request );
            Assert.fail( "head object must be fail!" );
        } catch ( AmazonS3Exception e ) {
            // 404 Not Found
            Assert.assertEquals( e.getStatusCode(), 404 );
        }

        s3Client.putObject( bucketName, key, new File( filePath2 ) );
        GetObjectMetadataRequest request2 = new GetObjectMetadataRequest(
                bucketName, key );
        ObjectMetadata result2 = s3Client.getObjectMetadata( request2 );
        checkObjectMetaInfo( result2, filePath2, fileSize2 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                s3Client.deleteObject( bucketName, key );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void checkObjectMetaInfo( ObjectMetadata result, String filePath,
            int fileSize ) throws IOException {
        long size = result.getContentLength();
        String md5 = result.getETag();

        String expMd5 = TestTools.getMD5( filePath );
        Assert.assertEquals( size, fileSize );
        Assert.assertEquals( md5, expMd5 );
    }
}
