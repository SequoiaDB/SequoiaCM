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
 * @Description SCM-3359:带分隔符delimiter查询对象元数据列表
 * @author fanyu
 * @Date 2018.11.19
 * @version 1.00
 */
public class ListObjects3359 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3359";
    private String key = "aa/bb/object3359";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 2;
    private int matchObjectNums = 20;
    private File localPath = null;
    private String filePath = null;
    private String delimiter = "/";

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
    public void testListObjects() throws Exception {
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
        request.withDelimiter( delimiter );
        ObjectListing result = s3Client.listObjects( request );
        Assert.assertEquals( result.getDelimiter(), delimiter );

        List< String > commonPrefixes = result.getCommonPrefixes();
        // matching delimiter displays only 1 record
        Assert.assertEquals( commonPrefixes.size(), 1 );
        Assert.assertEquals( commonPrefixes.get( 0 ), "aa/" );

        // objects do not match delimiter are displayed in contents,num is 10
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        int contentsNums = 10;
        Assert.assertEquals( objects.size(), contentsNums );
        for ( S3ObjectSummary os : objects ) {
            String key = os.getKey();
            String etag = os.getETag();
            long size = os.getSize();
            queryKeyList.add( key );
            Assert.assertEquals( etag, TestTools.getMD5( filePath ) );
            Assert.assertEquals( size, fileSize );
        }

        // check the keyName
        Assert.assertEquals( queryKeyList, keyList );
    }

    private List< String > putObjects() {
        List< String > noMatchKeyList = new ArrayList<>();
        int objectNums = 30;
        String keyName;
        for ( int i = 0; i < objectNums; i++ ) {
            if ( i < matchObjectNums ) {
                keyName = key + "_" + i;
            } else {
                keyName = "object3359_" + i;
                noMatchKeyList.add( keyName );
            }
            s3Client.putObject( bucketName, keyName, new File( filePath ) );
        }
        Collections.sort( noMatchKeyList );
        return noMatchKeyList;
    }
}
