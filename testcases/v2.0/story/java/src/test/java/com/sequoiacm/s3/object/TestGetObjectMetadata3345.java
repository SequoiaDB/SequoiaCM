package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Descreption SCM-3345:带prefix、start-after和maxkeys匹配查询对象元数据列表
 * @Author YiPan
 * @Date 2021/2/26
 */
public class TestGetObjectMetadata3345 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3345";
    private String objectName = "object3345";
    private int objectnum = 15;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 100;
    private String prefix = "test";

    @BeforeClass
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localfile" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws IOException {
        List< String > expkey = new ArrayList<>();
        List< String > actkey = new ArrayList<>();
        // 带delimiter
        for ( int i = 0; i < objectnum / 3; i++ ) {
            expkey.add( prefix + "/" + objectName + "a" + i );
            expkey.add( prefix + "/" + objectName + "b" + i );
            s3Client.putObject( bucketName, prefix + "/" + objectName + "a" + i,
                    new File( filePath ) );
            s3Client.putObject( bucketName, prefix + "/" + objectName + "b" + i,
                    new File( filePath ) );
            s3Client.putObject( bucketName, prefix + "/" + objectName,
                    new File( filePath ) );
        }
        ListObjectsV2Request request;
        ListObjectsV2Result result;
        // 满足StarAfter和prefix,maxkeys<objectnum
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withPrefix( prefix ).withStartAfter( expkey.get( 0 ) )
                .withMaxKeys( 4 );
        do {
            result = s3Client.listObjectsV2( request );
            List< S3ObjectSummary > objects = result.getObjectSummaries();
            for ( S3ObjectSummary s : objects ) {
                actkey.add( s.getKey() );
            }
            request.setContinuationToken( result.getNextContinuationToken() );
        } while ( result.isTruncated() );
        expkey.remove( 0 );
        Collections.sort( actkey );
        Collections.sort( expkey );
        Assert.assertEquals( actkey, expkey );
        actkey.clear();
        // 满足StarAfter和prefix,maxkeys>objectnum
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withPrefix( prefix ).withStartAfter( expkey.get( 0 ) )
                .withMaxKeys( 11 );
        result = s3Client.listObjectsV2( request );
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        for ( S3ObjectSummary s : objects ) {
            actkey.add( s.getKey() );
        }
        expkey.remove( 0 );
        Collections.sort( actkey );
        Collections.sort( expkey );
        Assert.assertEquals( actkey, expkey );
    }

    @AfterClass
    private void tearDown() {
        try {
            S3Utils.clearBucket( s3Client, bucketName );
            TestTools.LocalFile.removeFile( localPath );
        } finally {
            s3Client.shutdown();
        }
    }
}
