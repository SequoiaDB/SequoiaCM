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
import java.util.List;

/**
 * @Descreption SCM-3339:带分隔符delimiter和maxkeys查询
 * @Author YiPan
 * @Date 2021/3/10
 */
public class TestGetObjectMetadata3339 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3339";
    private String objectName = "object3339";
    private int objectnum = 10;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 100;
    private String delimiter = "/";
    private String prefix = "test";

    @BeforeClass
    private void setUp() throws Exception {
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
        for ( int i = 0; i < objectnum; i++ ) {
            s3Client.putObject( bucketName, prefix + delimiter + objectName + i,
                    new File( filePath ) );
            expkey.add( objectName + i );
            s3Client.putObject( bucketName, objectName + i,
                    new File( filePath ) );
        }
        ListObjectsV2Request request = null;
        ListObjectsV2Result result = null;
        // maxkeys=3匹配结果
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withDelimiter( delimiter ).withMaxKeys( 3 );
        do {
            result = s3Client.listObjectsV2( request );
            List< String > commonPrefixes = result.getCommonPrefixes();
            for ( String s : commonPrefixes ) {
                Assert.assertEquals( s, prefix + delimiter );
            }
            List< S3ObjectSummary > objects = result.getObjectSummaries();
            for ( S3ObjectSummary object : objects ) {
                actkey.add( object.getKey() );
            }
            if ( objects.size() > 3 ) {
                Assert.fail( "exceed the maximum number,  :" + objects.size() );
            }
            request.setContinuationToken( result.getNextContinuationToken() );
        } while ( result.isTruncated() );
        Assert.assertEquals( actkey, expkey );
        actkey.clear();

        // maxkeys=11匹配结果
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withDelimiter( delimiter ).withMaxKeys( 11 );
        result = s3Client.listObjectsV2( request );
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        List< String > commonPrefixes = result.getCommonPrefixes();
        Assert.assertEquals( commonPrefixes.size(), 1 );
        for ( String s : commonPrefixes ) {
            Assert.assertEquals( s, prefix + delimiter );
        }
        Assert.assertEquals( objects.size(), objectnum );
        for ( S3ObjectSummary object : objects ) {
            actkey.add( object.getKey() );
        }
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
