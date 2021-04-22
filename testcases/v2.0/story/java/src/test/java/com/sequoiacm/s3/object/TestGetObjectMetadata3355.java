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
 * @Descreption SCM-3355:指定prefix为空串查询对象元数据列表
 * @Author YiPan
 * @Date 2021/2/26
 */
public class TestGetObjectMetadata3355 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3355";
    private String objectName = "object3355";
    private int objectnum = 6;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 100;
    private String prefix = "test";
    private String delimiter = "/";
    private String userName = "admin";

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
            expkey.add( prefix + delimiter + objectName + i );
        }
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName( bucketName ).withPrefix( "" )
                .withFetchOwner( true );
        ListObjectsV2Result result = s3Client.listObjectsV2( request );
        for ( S3ObjectSummary s : result.getObjectSummaries() ) {
            actkey.add( s.getKey() );
        }
        Assert.assertEquals( result.getCommonPrefixes().size(), 0 );
        Collections.sort( actkey );
        Collections.sort( expkey );
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
