package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @Descreption SCM-3342:带prefix和start-after查询对象元数据列表，不匹配prefix
 *              SCM-3343:带prefix和start-after查询对象元数据列表，不匹配start-after
 * @Author YiPan
 * @Date 2021/2/26
 */
public class TestGetObjectMetadata3342_3343 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3342";
    private String objectName = "object3342";
    private int objectnum = 10;
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

    @Test(groups = { GroupTags.base })
    public void test() throws IOException {
        for ( int i = 0; i < objectnum; i++ ) {
            s3Client.putObject( bucketName, prefix + "/" + objectName + i,
                    new File( filePath ) );
        }
        ListObjectsV2Request request;
        ListObjectsV2Result result;
        // SCM-3342:带prefix和start-after查询对象元数据列表，不匹配prefix
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withEncodingType( "url" ).withPrefix( prefix + "wrong" )
                .withStartAfter( prefix );
        result = s3Client.listObjectsV2( request );
        Assert.assertEquals( result.getObjectSummaries().size(), 0 );
        Assert.assertEquals( result.getCommonPrefixes().size(), 0 );
        // SCM-3343:带prefix和start-after查询对象元数据列表，不匹配start-after
        request = new ListObjectsV2Request().withBucketName( bucketName )
                .withEncodingType( "url" ).withPrefix( prefix )
                .withStartAfter( prefix + "Z" );
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
