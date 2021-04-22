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
 * @Descreption SCM-3336:带前缀prefix查询对象元数据列表 SCM-3337:带前缀prefix查询对象元数据列表，匹配不到对象数据
 * @Author YiPan
 * @Date 2021/3/10
 */
public class TestGetObjectMetadata3336_3337 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3336";
    private String objectName = "object3336";
    private int objectnum = 10;
    private String prefix = "test";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 100;

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
        // 带prefix
        for ( int i = 0; i < objectnum; i++ ) {
            s3Client.putObject( bucketName, prefix + "/" + objectName + i,
                    new File( filePath ) );
            expkey.add( prefix + "/" + objectName + i );
            s3Client.putObject( bucketName, objectName + i,
                    new File( filePath ) );
        }
        // 正确prefix获取结果
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName( bucketName );
        request.withPrefix( prefix );
        ListObjectsV2Result result = s3Client.listObjectsV2( request );
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        // 结果校验
        Assert.assertEquals( objects.size(), objectnum );
        for ( S3ObjectSummary object : objects ) {
            actkey.add( object.getKey() );
            Assert.assertEquals( object.getETag(),
                    TestTools.getMD5( filePath ) );
            Assert.assertEquals( object.getSize(), fileSize );
        }
        Assert.assertEquals( actkey, expkey );
        // 错误五prefix获取结果
        ListObjectsV2Request wrongRequest = new ListObjectsV2Request()
                .withBucketName( bucketName );
        wrongRequest.withPrefix( "wrong" );
        ListObjectsV2Result result2 = s3Client.listObjectsV2( wrongRequest );
        // 结果校验
        Assert.assertEquals( result2.getObjectSummaries().size(), 0 );
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
