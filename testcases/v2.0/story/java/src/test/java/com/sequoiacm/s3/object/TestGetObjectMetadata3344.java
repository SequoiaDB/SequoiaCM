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
 * @Descreption SCM-3344:带prefix和start-after匹配查询对象元数据列表
 * @Author YiPan
 * @Date 2021/2/26
 */
public class TestGetObjectMetadata3344 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3344";
    private String objectName = "object3344";
    private int objectnum = 1100;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 100;
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
        ListObjectsV2Request request;
        ListObjectsV2Result result;
        List< String > expkey = new ArrayList<>();
        List< String > actkey = new ArrayList<>();
        // SCM-3344:带prefix和start-after匹配查询对象元数据列表 多次返回
        for ( int i = 0; i < objectnum; i++ ) {
            expkey.add( prefix + "/" + objectName + i );
            s3Client.putObject( bucketName, prefix + "/" + objectName + i,
                    new File( filePath ) );
        }
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withPrefix( prefix ).withStartAfter( prefix )
                .withEncodingType( "url" );
        do {
            result = s3Client.listObjectsV2( request );
            List< S3ObjectSummary > objects = result.getObjectSummaries();
            for ( S3ObjectSummary s : objects ) {
                actkey.add( s.getKey() );
            }
            request.setContinuationToken( result.getNextContinuationToken() );
        } while ( result.isTruncated() );
        Collections.sort( actkey );
        Collections.sort( expkey );
        Assert.assertEquals( actkey, expkey );
        // 清理环境
        actkey.clear();
        expkey.clear();
        S3Utils.deleteAllObjects( s3Client, bucketName );

        // SCM-3344:带prefix和start-after匹配查询对象元数据列表 一次返回
        for ( int i = 0; i < objectnum / 10; i++ ) {
            expkey.add( prefix + "/" + objectName + i );
            s3Client.putObject( bucketName, prefix + "/" + objectName + i,
                    new File( filePath ) );
        }
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withPrefix( prefix ).withStartAfter( prefix );
        result = s3Client.listObjectsV2( request );
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        for ( S3ObjectSummary s : objects ) {
            actkey.add( s.getKey() );
        }
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
