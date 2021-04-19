package com.sequoiacm.s3.object;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description: SCM-3329：指定ifNoneMatch和ifModifiedSince条件查询对象
 * @author wangkexin
 * @Date 2018.12.11
 * @version 1.00
 */

public class TestGetObjectMetadata3329 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3329";
    private String keyName = "key3329";
    private String content = "content3329";
    private Calendar cal = Calendar.getInstance();
    private String historyETag = null;
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client( );
        s3Client.createBucket( bucketName );
        PutObjectResult result1 =  s3Client.putObject( bucketName, keyName, content + "v1" );
        historyETag = result1.getETag();
         s3Client.putObject( bucketName, keyName, content + "v2" );
    }

    @Test
    private void testGetObjectMetadata() throws Exception {
        cal.set( Calendar.YEAR, 2010 );
        Date date1 =  cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US );
        sdf.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
        String modifiedDate1 = sdf.format( date1 );

        // 匹配If-None-Match 不匹配If-Unmodified-Since
        GetObjectMetadataRequest request1 = new GetObjectMetadataRequest(
                bucketName, keyName );
        request1.putCustomRequestHeader( "If-Unmodified-Since", modifiedDate1 );
        request1.putCustomRequestHeader( "If-None-Match", historyETag );
        try {
            s3Client.getObjectMetadata( request1 );
            Assert.fail("exp failed but act success!!!");
        }catch ( AmazonS3Exception e ){
            if(e.getStatusCode() != 412){
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                s3Client.deleteObject( bucketName, keyName );
                s3Client.deleteBucket( bucketName );
            }
        }finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
