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
 * @Descreption SCM-3338:带分隔符delimiter查询对象元数据列表
 * @Author YiPan
 * @Date 2021/3/10
 */
public class TestGetObjectMetadata3338 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3338";
    private String objectName = "object3338";
    private int objectnum = 10;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 100;
    private String delimiter = "/";
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
        for ( int i = 0; i < objectnum; i++ ) {
            s3Client.putObject( bucketName, prefix + delimiter + objectName + i,
                    new File( filePath ) );
            expkey.add( objectName + i );
            s3Client.putObject( bucketName, objectName + i,
                    new File( filePath ) );
        }
        // 正确delimiter获取结果
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName( bucketName );
        request.withDelimiter( delimiter );
        ListObjectsV2Result result = s3Client.listObjectsV2( request );
        // 获取Contents校验
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        Assert.assertEquals( objects.size(), objectnum );
        for ( S3ObjectSummary object : objects ) {
            actkey.add( object.getKey() );
            Assert.assertEquals( object.getETag(),
                    TestTools.getMD5( filePath ) );
            Assert.assertEquals( object.getSize(), fileSize );
        }
        Assert.assertEquals( actkey, expkey );

        // 获取CommonPrefixes校验
        List< String > commonPrefixes = result.getCommonPrefixes();
        Assert.assertEquals( commonPrefixes.size(), 1 );
        Assert.assertEquals( commonPrefixes.get( 0 ), prefix + delimiter );
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
