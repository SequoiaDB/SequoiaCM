package com.sequoiacm.s3.object;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3357:带前缀prefix查询对象元数据列表
 * @author wuyan
 * @Date 2018.11.19
 * @version 1.00
 */
public class ListObjects3357 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3357";
    private String key = "aa/bb/object3357";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 100;
    private int matchObjectNums = 20;
    private File localPath = null;
    private String filePath = null;
    private String prefix = "cc";

    @BeforeClass
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void testCreateObject() throws Exception {
        List< String > keyList = putObjects();
        listObjectV1AndCheckResult( keyList );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void listObjectV1AndCheckResult( List< String > keyList )
            throws IOException {
        List< String > queryKeyList = new ArrayList<>();
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName( bucketName );
        request.withPrefix( prefix );
        ObjectListing result = s3Client.listObjects( request );
        Assert.assertEquals( prefix, result.getPrefix() );

        List< S3ObjectSummary > objects = result.getObjectSummaries();
        Assert.assertEquals( objects.size(), matchObjectNums );
        for ( S3ObjectSummary os : objects ) {
            String key = os.getKey();
            String etag = os.getETag();
            long size = os.getSize();
            queryKeyList.add( key );

            // check the etag and size
            Assert.assertEquals( etag, TestTools.getMD5( filePath ) );
            Assert.assertEquals( size, fileSize );
        }

        // check the keyName
        Assert.assertEquals( queryKeyList, keyList );
    }

    private List< String > putObjects() {
        List< String > matchKeyList = new ArrayList<>();
        int objectNums = 30;
        String keyName;
        for ( int i = 0; i < objectNums; i++ ) {
            if ( i < matchObjectNums ) {
                keyName = prefix + "_" + i;
                matchKeyList.add( keyName );
            } else {
                keyName = key + "_" + i;
            }
            s3Client.putObject( bucketName, keyName, new File( filePath ) );
        }
        Collections.sort( matchKeyList );
        return matchKeyList;
    }
}
