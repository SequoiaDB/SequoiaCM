package com.sequoiacm.s3.object;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3322:指定ifNoneMatch和ifUnModifiedSince条件查询对象
 * @author fanyu
 * @Date 2018.12.18
 * @version 1.00
 */

public class TestGetObjectMetadata3322 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3322";
    private String key = "head/key3322";
    private AmazonS3 s3Client = null;
    private Calendar cal = Calendar.getInstance();
    private String eTag1 = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        PutObjectResult resultV1 = s3Client.putObject( bucketName, key,
                "testobject3322v100" );
        eTag1 = resultV1.getETag();
        s3Client.putObject( bucketName, key, "testobject3322v2" );
        s3Client.putObject( bucketName, key, "testobject3322v3" );
    }

    @Test
    private void testHeadObject() throws Exception {
        cal.set( Calendar.YEAR, 2037 );
        Date date1 = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US );
        sdf.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
        String modifiedDate1 = sdf.format( date1 );

        GetObjectMetadataRequest request1 = new GetObjectMetadataRequest(
                bucketName, key );
        request1.putCustomRequestHeader( "If-Unmodified-Since", modifiedDate1 );
        request1.putCustomRequestHeader( "If-None-Match", eTag1 );
        ObjectMetadata objectMeta1 = s3Client.getObjectMetadata( request1 );
        Assert.assertEquals( objectMeta1.getETag(),
                TestTools.getMD5( "testobject3322v3".getBytes() ) );
        Assert.assertEquals( objectMeta1.getUserMetadata(), new HashMap<>() );
        Assert.assertEquals( objectMeta1.getContentLength(),
                "testobject3322v3".length() );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.deleteObjectAllVersions( s3Client, bucketName, key );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
