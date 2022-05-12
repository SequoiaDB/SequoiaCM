package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
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
 * @Descreption SCM-3349:带prefix、start-after、delimiter在设置continueation-token前后匹配条件不一致
 * @Author YiPan
 * @Date 2021/3/3
 */
public class TestGetObjectMetadata3349 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3349";
    private String objectName = "object/3349";
    private int objectnum = 1500;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 100;
    private String prefixA = "testA";
    private String prefixB = "testB";
    private String delimiter = "/";

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

    // SEQUOIACM-770暂时屏蔽
    @Test(enabled = false)
    public void test() throws IOException {
        List< String > expCommonPrefixesA = new ArrayList<>();
        List< String > expCommonPrefixesB = new ArrayList<>();
        List< String > actCommonPrefixes = new ArrayList<>();
        ListObjectsV2Result result;
        ListObjectsV2Request request;
        for ( int i = 0; i < objectnum; i++ ) {
            s3Client.putObject( bucketName,
                    prefixA + i + delimiter + objectName + i,
                    new File( filePath ) );
            expCommonPrefixesA.add( prefixA + i + delimiter );
        }
        for ( int i = 0; i < objectnum; i++ ) {
            s3Client.putObject( bucketName,
                    prefixB + i + delimiter + objectName + i,
                    new File( filePath ) );
            expCommonPrefixesB.add( prefixB + i + delimiter );
        }
        // 第一次查询
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withPrefix( prefixA ).withStartAfter( prefixA )
                .withDelimiter( delimiter ).withEncodingType( "url" );
        result = s3Client.listObjectsV2( request );
        List< String > commonPrefixes = result.getCommonPrefixes();
        for ( String s : commonPrefixes ) {
            actCommonPrefixes.add( s );
        }
        String nextContinuationToken = result.getNextContinuationToken();
        Collections.sort( actCommonPrefixes );
        Collections.sort( expCommonPrefixesA );
        Assert.assertEquals( actCommonPrefixes,
                expCommonPrefixesA.subList( 0, 1000 ) );
        actCommonPrefixes.clear();

        // 第二次查询
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withPrefix( prefixB ).withStartAfter( prefixB )
                .withDelimiter( delimiter )
                .withContinuationToken( nextContinuationToken );
        result = s3Client.listObjectsV2( request );
        commonPrefixes = result.getCommonPrefixes();
        for ( String s : commonPrefixes ) {
            actCommonPrefixes.add( s );
        }
        Collections.sort( actCommonPrefixes );
        Collections.sort( expCommonPrefixesB );
        Assert.assertEquals( actCommonPrefixes,
                expCommonPrefixesB.subList( 0, 1000 ) );
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
