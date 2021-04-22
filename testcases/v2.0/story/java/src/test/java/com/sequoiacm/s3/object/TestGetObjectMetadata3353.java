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
import java.util.List;

/**
 * @Descreption SCM-3353:多次查询结果在commprefix中有相同记录
 * @Author
 * @Date 2021/2/26
 */
public class TestGetObjectMetadata3353 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3553";
    private String objectName = "object3353";
    private int objectnum = 6;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 100;
    private String prefix = "test";
    private String delimiter = "/";

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
    public void test() throws IOException, InterruptedException {
        List< String > expkey = new ArrayList<>();
        List< String > actkey = new ArrayList<>();
        // 插入数据
        for ( int i = 0; i < objectnum / 2; i++ ) {
            s3Client.putObject( bucketName,
                    prefix + i + delimiter + objectName + "a",
                    new File( filePath ) );
            s3Client.putObject( bucketName,
                    prefix + i + delimiter + objectName + "b",
                    new File( filePath ) );
            expkey.add( prefix + i + delimiter );
        }
        // 第一次查询
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName( bucketName ).withStartAfter( prefix )
                .withDelimiter( delimiter ).withMaxKeys( 2 );
        ListObjectsV2Result result;
        int queryTime = 0;
        do {
            queryTime++;
            result = s3Client.listObjectsV2( request );
            List< String > commonPrefixes = result.getCommonPrefixes();
            for ( String s : commonPrefixes ) {
                actkey.add( s );
            }
            request.setContinuationToken( result.getNextContinuationToken() );
        } while ( result.isTruncated() );
        Assert.assertEquals( queryTime, 2 );
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
