package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
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
 * @Descreption SCM-3340:带start-after查询对象元数据列表
 * @Author YiPan
 * @Date 2021/2/26
 */
public class TestGetObjectMetadata3340 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3340";
    private String objectName = "object3340";
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
        List< String > non = new ArrayList<>();
        // 带delimiter
        for ( int i = 0; i < objectnum; i++ ) {
            s3Client.putObject( bucketName, objectName + i,
                    new File( filePath ) );
            expkey.add( objectName + i );
        }
        ListObjectsV2Request request = null;
        // test a:指定为中间记录
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withStartAfter( expkey.get( objectnum / 2 ) );
        checkresult( request, expkey.subList( objectnum / 2 + 1, objectnum ) );
        // test b:指定为第一条记录
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withStartAfter( expkey.get( 0 ) );
        checkresult( request, expkey.subList( 1, objectnum ) );
        // test c:指定为最后一条记录
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withStartAfter( expkey.get( objectnum - 1 ) );
        checkresult( request, non );
        // test d:指定匹配最后一条记录
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withStartAfter( expkey.get( objectnum - 2 ) );
        checkresult( request, expkey.subList( 9, 10 ) );
        // test e:指定匹配不到记录
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withStartAfter( "test3340" );
        checkresult( request, non );
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

    private void checkresult( ListObjectsV2Request request,
            List< String > expkey ) {
        List< String > actkey = new ArrayList<>();
        List< S3ObjectSummary > objects = s3Client.listObjectsV2( request )
                .getObjectSummaries();
        for ( S3ObjectSummary object : objects ) {
            actkey.add( object.getKey() );
        }
        Assert.assertEquals( actkey, expkey );
    }
}
