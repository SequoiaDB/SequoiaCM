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
 * @Descreption SCM-3351:带start-after和maxkeys查询对象元数据列表
 * @Author YiPan
 * @Date 2021/2/26
 */
public class TestGetObjectMetadata3351 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3351";
    private String objectName = "object3351";
    private int objectnum = 5;
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
    public void test() throws IOException {
        List< String > expkey = new ArrayList<>();
        List< String > actkey = new ArrayList<>();
        // 带delimiter
        for ( int i = 0; i < objectnum; i++ ) {
            if ( i < 2 ) {
                expkey.add( prefix + i + delimiter + objectName + i );
            }
            s3Client.putObject( bucketName,
                    prefix + i + delimiter + objectName + i,
                    new File( filePath ) );
        }
        ListObjectsV2Request request;
        ListObjectsV2Result result;
        // maxkeys<objectnum,StartAfter匹配第一条
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withStartAfter( prefix ).withMaxKeys( 2 );

        result = s3Client.listObjectsV2( request );
        for ( S3ObjectSummary s : result.getObjectSummaries() ) {
            actkey.add( s.getKey() );
        }
        request.setContinuationToken( result.getNextContinuationToken() );
        Assert.assertEquals( actkey, expkey );
        Assert.assertEquals( result.getCommonPrefixes().size(), 0 );
        actkey.clear();

        // maxkeys=1 StartAfter指定最后一条
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withStartAfter( prefix + 5 ).withMaxKeys( 1 );
        result = s3Client.listObjectsV2( request );
        Assert.assertEquals( result.getObjectSummaries().size(), 0 );
        Assert.assertEquals( result.getCommonPrefixes().size(), 0 );
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
