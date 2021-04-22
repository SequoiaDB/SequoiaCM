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
 * @Descreption SCM-3354:指定fetch-owner查询对象元数据列表，显示所有者信息
 * @Author YiPan
 * @Date 2021/2/26
 */
public class TestGetObjectMetadata3354 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3354";
    private String objectName = "object3354";
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
    public void test() {
        List< String > expkey = new ArrayList<>();
        List< String > actkey = new ArrayList<>();
        for ( int i = 0; i < objectnum; i++ ) {
            s3Client.putObject( bucketName, prefix + delimiter + objectName + i,
                    new File( filePath ) );
            s3Client.putObject( bucketName, prefix + objectName + i,
                    new File( filePath ) );
            expkey.add( prefix + objectName + i );
        }
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName( bucketName ).withPrefix( prefix )
                .withDelimiter( delimiter ).withFetchOwner( true );
        ListObjectsV2Result result = s3Client.listObjectsV2( request );
        for ( S3ObjectSummary s : result.getObjectSummaries() ) {
            actkey.add( s.getKey() );
            Assert.assertEquals( s.getOwner().getDisplayName(),
                    TestScmBase.scmUserName );
        }
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
