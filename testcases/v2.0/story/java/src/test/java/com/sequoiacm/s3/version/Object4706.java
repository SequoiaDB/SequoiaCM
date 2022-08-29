package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4706 ::
 *              带prefix、keyMarker、versionIdMarker和delimiter查询对象版本列表，不匹配delimiter
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4706 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4706";
    private String[] objectNames = { "air14706", "dir2/4706A.png",
            "test4706.doc" };
    private AmazonS3 s3Client = null;
    private int fileSize = 3;
    private int versionNum = 5;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < versionNum; i++ ) {
            String filePath = localPath + File.separator + "localFile_"
                    + ( fileSize + i ) + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize + i );
            filePathList.add( filePath );
        }
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
        for ( String objectName : objectNames ) {
            for ( int i = 0; i < versionNum; i++ ) {
                s3Client.putObject( new PutObjectRequest( bucketName,
                        objectName, new File( filePathList.get( i ) ) ) );
            }
        }
    }

    // 指定keyMarker+versionIdMarker的key匹配prefix
    @Test
    private void test1() throws Exception {
        String prefix = "dir";
        String delimiter = "%";
        String keyMarker = objectNames[ 0 ];
        String versionIdMarker = "5.0";
        // list versions by prefix/delimiter/currentversionId/key
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withDelimiter( delimiter )
                .withPrefix( prefix ).withKeyMarker( keyMarker )
                .withVersionIdMarker( versionIdMarker ) );
        // expected results
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        for ( int i = versionNum; i > 0; i-- ) {
            expMap.add( objectNames[ 1 ], i + ".0" );
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), false,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList,
                S3Utils.getCommPrefixes( objectNames, prefix, delimiter ),
                expMap );
        runSuccess = true;
    }

    // 指定keyMarker+versionIdMarker的key不匹配prefix
    @Test
    private void test2() throws Exception {
        String prefix = "abc";
        String delimiter = "%";
        String keyMarker = objectNames[ 0 ];
        String versionIdMarker = "4.0";
        // list versions by prefix/delimiter/currentversionId/key
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withDelimiter( delimiter )
                .withPrefix( prefix ).withKeyMarker( keyMarker )
                .withVersionIdMarker( versionIdMarker ) );
        // check
        Assert.assertEquals( vsList.isTruncated(), false,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList,
                S3Utils.getCommPrefixes( objectNames, prefix, delimiter ),
                new LinkedMultiValueMap< String, String >() );
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
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
