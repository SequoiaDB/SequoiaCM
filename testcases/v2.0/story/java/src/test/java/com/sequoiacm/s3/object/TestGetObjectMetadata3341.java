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
 * @Descreption SCM-3341:带maxkeys查询对象元数据列表
 * @Author YiPan
 * @Date 2021/2/26
 */
public class TestGetObjectMetadata3341 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3341";
    private String objectName = "object3341";
    private int objectnum = 4;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 100;

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
        List< String > expkey = new ArrayList<>();
        // 带delimiter
        for ( int i = 0; i < objectnum / 2; i++ ) {
            expkey.add( "test" + i + "/" + objectName );
            expkey.add( objectName + i );
            s3Client.putObject( bucketName, "test" + i + "/" + objectName,
                    new File( filePath ) );
            s3Client.putObject( bucketName, objectName + i,
                    new File( filePath ) );
        }
        // test a: maxkeys < objectNums
        int maxKeysA = 1;
        listObjectsAndCheckResult( expkey, maxKeysA );

        // test a: maxkeys < objectNums
        int maxKeysB = 3;
        listObjectsAndCheckResult( expkey, maxKeysB );

        // test b: maxKeys > objectNums
        int maxKeysC = 5;
        listObjectsAndCheckResult( expkey, maxKeysC );

        // test c: maxKeys = objectNums
        int maxKeysD = 4;
        listObjectsAndCheckResult( expkey, maxKeysD );

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

    private void listObjectsAndCheckResult( List< String > expkey,
            int maxKeys ) {
        List< String > actkey = new ArrayList<>();
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName( bucketName ).withMaxKeys( maxKeys );
        ListObjectsV2Result result;
        do {
            result = s3Client.listObjectsV2( request );
            List< S3ObjectSummary > objects = result.getObjectSummaries();
            for ( S3ObjectSummary object : objects ) {
                actkey.add( object.getKey() );
            }
            if ( objects.size() < maxKeys && objects.size() == objectnum ) {
                Assert.assertEquals( objects.size(), objectnum );
            } else if ( objects.size() < maxKeys
                    && objects.size() < objectnum ) {
                int i = objectnum / maxKeys;
                Assert.assertEquals( objects.size(), objectnum - maxKeys * i );
            } else {
                Assert.assertEquals( objects.size(), maxKeys );
            }
            request.setContinuationToken( result.getNextContinuationToken() );
        } while ( result.isTruncated() );
        Collections.sort( actkey );
        Collections.sort( expkey );
        Assert.assertEquals( actkey, expkey );

    }
}
