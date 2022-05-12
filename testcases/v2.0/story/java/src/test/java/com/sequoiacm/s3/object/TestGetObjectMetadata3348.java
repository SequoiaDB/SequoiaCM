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
 * @Descreption SCM-3348:带prefix、start-after、delimiter匹配查询对象元数据列表（多次查询）
 * @Author YiPan
 * @Date 2021/3/3
 */
public class TestGetObjectMetadata3348 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3348";
    private String objectName = "object3348";
    private int objectnum = 1500;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 100;
    private String prefix = "test";
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

    @Test
    public void test() throws IOException {
        List< String > expObjectSummaries = new ArrayList<>();
        List< String > expCommonPrefixes = new ArrayList<>();
        List< String > actObjectSummaries = new ArrayList<>();
        List< String > actCommonPrefixes = new ArrayList<>();
        // 带delimiter
        for ( int i = 0; i < objectnum; i++ ) {
            expCommonPrefixes.add( prefix + i + delimiter );
            expObjectSummaries.add( prefix + i + objectName + i );
            s3Client.putObject( bucketName,
                    prefix + i + delimiter + objectName + i,
                    new File( filePath ) );
            s3Client.putObject( bucketName, prefix + i + objectName + i,
                    new File( filePath ) );
        }
        // 满足StarAfter和prefix 满足delimiter
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName( bucketName ).withPrefix( prefix )
                .withStartAfter( prefix ).withDelimiter( delimiter );
        ListObjectsV2Result result;
        do {
            result = s3Client.listObjectsV2( request );
            List< String > commonPrefixes = result.getCommonPrefixes();
            for ( String s : commonPrefixes ) {
                actCommonPrefixes.add( s );
            }
            List< S3ObjectSummary > objects = result.getObjectSummaries();
            for ( S3ObjectSummary s : objects ) {
                actObjectSummaries.add( s.getKey() );
            }
            request.setContinuationToken( result.getNextContinuationToken() );
        } while ( result.isTruncated() );
        Collections.sort( actCommonPrefixes );
        Collections.sort( expCommonPrefixes );
        Assert.assertEquals( actCommonPrefixes, expCommonPrefixes );
        Collections.sort( actObjectSummaries );
        Collections.sort( expObjectSummaries );
        Assert.assertEquals( actObjectSummaries, expObjectSummaries );
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
