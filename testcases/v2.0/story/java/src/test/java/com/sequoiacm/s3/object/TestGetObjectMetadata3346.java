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
 * @Descreption SCM-3346:带prefix、start-after、delimiter查询对象元数据列表，不匹配delimiter
 * @Author YiPan
 * @Date 2021/2/26
 */
public class TestGetObjectMetadata3346 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3346";
    private String objectName = "object3346";
    private int objectnum = 4;
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
        List< String > expperfix = new ArrayList<>();
        List< String > actperfix = new ArrayList<>();
        // 带delimiter
        for ( int i = 0; i < objectnum; i++ ) {
            s3Client.putObject( bucketName, prefix + objectName + i,
                    new File( filePath ) );
            expkey.add( prefix + objectName + i );
        }
        // 满足StarAfter和prefix 不满足delimiter
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName( bucketName ).withPrefix( prefix )
                .withStartAfter( prefix ).withDelimiter( delimiter );
        ListObjectsV2Result result = s3Client.listObjectsV2( request );
        List< String > commonPrefixes = result.getCommonPrefixes();
        for ( String s : commonPrefixes ) {
            actperfix.add( s );
        }
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        for ( S3ObjectSummary s : objects ) {
            actkey.add( s.getKey() );
        }
        Assert.assertEquals( actkey, expkey );
        Assert.assertEquals( actperfix, expperfix );

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
