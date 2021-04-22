package com.sequoiacm.s3.object;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description: SCM-3324：指定ifUnModifiedSince和ifModifiedSince条件查询对象
 * @author wangkexin
 * @Date 2018.12.11
 * @version 1.00
 */

public class TestGetObjectMetadata3324 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3324";
    private String keyName = "key3324";
    private String content = "content3324";
    private Calendar cal = Calendar.getInstance();
    private String eTag = null;
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );

        s3Client.putObject( bucketName, keyName, content + "v1" );
        s3Client.putObject( bucketName, keyName, content + "v2" );
        PutObjectResult result = s3Client.putObject( bucketName, keyName,
                content + "v3" );
        eTag = result.getETag();
    }

    @Test
    private void testGetObjectMetadata() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US );
        sdf.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

        cal.set( Calendar.MONTH, cal.get( Calendar.MONTH ) - 1 );
        String modifiedDate = sdf.format( cal.getTime() );

        cal.set( Calendar.MONTH, cal.get( Calendar.MONTH ) + 2 );
        String unModifiedDate = sdf.format( cal.getTime() );

        // 指定ifModifiedSince时间小于actDate, ifUnModifiedSince时间大于actDate
        GetObjectMetadataRequest request1 = new GetObjectMetadataRequest(
                bucketName, keyName );
        request1.putCustomRequestHeader( "If-Unmodified-Since",
                unModifiedDate );
        request1.putCustomRequestHeader( "If-Modified-Since", modifiedDate );
        ObjectMetadata objectMeta1 = s3Client.getObjectMetadata( request1 );

        Assert.assertEquals( objectMeta1.getETag(), eTag );
        Assert.assertEquals( objectMeta1.getUserMetadata(), new HashMap<>() );
        Assert.assertEquals( objectMeta1.getContentLength(),
                ( content + "v3" ).length() );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                s3Client.deleteObject( bucketName, keyName );
                s3Client.deleteBucket( bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
