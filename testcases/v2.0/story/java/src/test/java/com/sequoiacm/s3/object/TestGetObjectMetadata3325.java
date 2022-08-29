package com.sequoiacm.s3.object;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description: SCM-3325：指定ifUnModifiedSince和ifModifiedSince条件查询对象，不匹配ifUnModifiedSince
 * @author fanyu
 * @Date 2018.12.11
 * @version 1.00
 */

public class TestGetObjectMetadata3325 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket16700";
    private String keyName = "key16700";
    private String content = "content16700";
    private Calendar cal = Calendar.getInstance();
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, keyName, content + "v1" );
        s3Client.putObject( bucketName, keyName, content + "v2" );
        s3Client.putObject( bucketName, keyName, content + "v3" );
    }

    @Test(groups = { GroupTags.base })
    private void testGetObjectMetadata() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US );
        sdf.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

        cal.set( Calendar.MONTH, cal.get( Calendar.MONTH ) - 1 );
        String modifiedDate = sdf.format( cal.getTime() );

        cal.set( Calendar.MONTH, cal.get( Calendar.MONTH ) - 2 );
        String unModifiedDate = sdf.format( cal.getTime() );

        // 指定ifModifiedSince时间小于actDate, ifUnModifiedSince时间小于actDate
        GetObjectMetadataRequest request1 = new GetObjectMetadataRequest(
                bucketName, keyName );
        request1.putCustomRequestHeader( "If-Unmodified-Since",
                unModifiedDate );
        request1.putCustomRequestHeader( "If-Modified-Since", modifiedDate );
        try {
            s3Client.getObjectMetadata( request1 );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( AmazonS3Exception e ) {
            if ( e.getStatusCode() != 412 ) {
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
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
