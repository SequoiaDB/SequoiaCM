package com.sequoiacm.s3.object;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description: SCM-3286: 开启版本控制，增加对象，指定headers请求参数
 *
 * @author wangkexin
 * @Date 2018.11.12
 * @version 1.00
 */
public class CreateObject3286 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3286";
    private String keyName = "object3286";
    private AmazonS3 s3Client = null;
    private File localPath = null;

    @BeforeClass
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        s3Client = S3Utils.buildS3Client();

        // create bucket
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test
    public void testCreateBucket() throws Exception {
        String content_encoding = "gzip3286";
        String content_type = "plain/text3286";
        String content = "testContent3286";
        Date date = new Date();
        Date x_amz_date = new Date();
        String x_amz_meta_x = "myparameter3286";
        String cache_control = "no-cache";
        String content_disposition = "testDisposition3286";
        String expires = getGMTDate( date );

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType( content_type );
        objectMetadata.setContentEncoding( content_encoding );
        Map<String,String> hashMap = new HashMap<>(  );
        hashMap.put( "myparameter",x_amz_meta_x );
        objectMetadata.setUserMetadata( hashMap );
        objectMetadata.setExpirationTime( date );
        objectMetadata.setHttpExpiresDate( x_amz_date );
        objectMetadata.setCacheControl( cache_control );
        objectMetadata.setContentDisposition( content_disposition );
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, keyName,
                new ByteArrayInputStream( content.getBytes() ),objectMetadata);

        s3Client.putObject( putObjectRequest );

        S3Object object = s3Client.getObject( bucketName, keyName );
        Assert.assertEquals( object.getObjectMetadata().getContentLength(),
                content.length(), "ContentLength is wrong" );
        Assert.assertEquals( object.getObjectMetadata().getContentEncoding(),
                content_encoding, "ContentEncoding is wrong" );
        Assert.assertEquals( object.getObjectMetadata().getContentType(),
                content_type, "ContentType is wrong" );
        Assert.assertEquals( object.getObjectMetadata().getCacheControl(),
                cache_control, "CacheControl is wrong" );
        Assert.assertEquals( object.getObjectMetadata().getContentDisposition(),
                content_disposition, "ContentDisposition is wrong" );
        Assert.assertEquals(
                getGMTDate( object.getObjectMetadata().getHttpExpiresDate() ),
                expires, "Expires is wrong" );
        Assert.assertEquals(
                object.getObjectMetadata().getUserMetadata()
                        .get( "myparameter" ),
                x_amz_meta_x, "x-amz-meta-* is wrong" );
        String actMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( actMd5, TestTools.getMD5( content.getBytes() ) );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.deleteAllObjectVersions( s3Client, bucketName );
            s3Client.deleteBucket( bucketName );
            TestTools.LocalFile.removeFile( localPath );
        }
    }

    private String getGMTDate( Date date ) {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US );
        sdf.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
        String rfc1123 = sdf.format( date );
        return rfc1123;
    }
}