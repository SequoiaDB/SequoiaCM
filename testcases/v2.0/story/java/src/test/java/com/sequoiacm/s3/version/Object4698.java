package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;

/**
 * @descreption SCM-4698 :: 带maxkeys查询对象版本列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4698 extends TestScmBase {
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private boolean runSuccess3 = false;
    private boolean runSuccess4 = false;
    private String bucketName = "bucket4698";
    private String[] objectNames = { "dir4698%dir4698A%dir4698AB",
            "dir4698%subdir4698A", "dir4698A", "dir4698B" };
    private AmazonS3 s3Client = null;
    private int fileSize = 3;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
        for ( String objectName : objectNames ) {
            s3Client.putObject( new PutObjectRequest( bucketName, objectName,
                    new File( filePath ) ) );
        }
    }

    @Test
    private void testMaxKeysLtRecords() {
        int maxKeys = 1;
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withMaxResults( maxKeys ) );
        // expected results
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        expMap.add( objectNames[ 0 ], "1.0" );
        // check
        Assert.assertEquals( vsList.isTruncated(), true,
                "vsList.isTruncated() must be true" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(), expMap );
        runSuccess1 = true;
    }

    @Test
    private void testMaxKeysGtRecords() {
        int maxKeys = 5;
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withMaxResults( maxKeys ) );
        // expected results
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        for ( int i = 0; i < objectNames.length; i++ ) {
            expMap.add( objectNames[ i ], "1.0" );
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), false,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(), expMap );
        runSuccess2 = true;
    }

    @Test
    private void testMaxKeysEqRecords() {
        int maxKeys = 4;
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withMaxResults( maxKeys ) );
        // expected results
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        for ( int i = 0; i < objectNames.length; i++ ) {
            expMap.add( objectNames[ i ], "1.0" );
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), false,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(), expMap );
        runSuccess3 = true;
    }

    @Test
    private void testMaxKeysEqMiddle() {
        int maxKeys = 3;
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withMaxResults( maxKeys ) );
        // expected results
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        for ( int i = 0; i < maxKeys; i++ ) {
            expMap.add( objectNames[ i ], "1.0" );
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), true,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(), expMap );
        runSuccess4 = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess1 && runSuccess2 && runSuccess3 && runSuccess4 ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
