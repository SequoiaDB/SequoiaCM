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
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description: SCM-3318：指定ifModifiedSince条件查询对象
 * @author fanyu
 * @Date 2018.12.17
 * @version 1.00
 */

public class TestGetObjectMetadata3318 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3318";
    private String keyName = "key3318";
    private String content = "content3318";
    private AmazonS3 s3Client = null;
    private Calendar cal = Calendar.getInstance();
    private String expEtag = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, keyName, content + "v1" );
        PutObjectResult result = s3Client.putObject( bucketName, keyName,
                content + "v2" );
        expEtag = result.getETag();
    }

    @Test
    private void testGetObjectMetadata() throws Exception {
        cal.set( Calendar.YEAR, 2037 );
        Date date1 = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US );
        sdf.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
        String modifiedDate1 = sdf.format( date1 );

        // 指定ifModifiedSince条件查询对象 ，指定时间后该对象未修改
        GetObjectMetadataRequest request1 = new GetObjectMetadataRequest(
                bucketName, keyName );
        System.out.println( "modifiedDate1 = " + modifiedDate1 );
        request1.putCustomRequestHeader( "If-Modified-Since", modifiedDate1 );
        try {
            s3Client.getObjectMetadata( request1 );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( AmazonS3Exception e ) {
            if ( e.getStatusCode() != 304 ) {
                throw e;
            }
        }

        // 指定ifModifiedSince条件查询对象 ，指定时间后该对象已更新
        cal.set( Calendar.YEAR, 2020 );
        Date date2 = cal.getTime();
        String modifiedDate2 = sdf.format( date2 );
        GetObjectMetadataRequest request2 = new GetObjectMetadataRequest(
                bucketName, keyName );
        request2.putCustomRequestHeader( "If-Modified-Since", modifiedDate2 );
        ObjectMetadata objectMeta2 = s3Client.getObjectMetadata( request2 );
        Assert.assertEquals( objectMeta2.getETag(), expEtag );
        Assert.assertEquals( objectMeta2.getUserMetadata(), new HashMap<>() );
        Assert.assertEquals( objectMeta2.getContentLength(),
                ( content + "v2" ).length() );
        Assert.assertNotNull( objectMeta2.getLastModified() );
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
