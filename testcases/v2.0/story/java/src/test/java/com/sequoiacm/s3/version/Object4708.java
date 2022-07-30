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
import java.util.Collections;
import java.util.List;

/**
 * @descreption SCM-4708 ::
 *              带prefix、keyMarker、versionIdMarker和delimiter匹配查询对象版本列表（多次查询）
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4708 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4708";
    private String objectNameBase = "dir";
    private List< String > objectNames = new ArrayList<>();
    private int objectNum = 1000;
    private AmazonS3 s3Client = null;
    private int fileSize = 1;
    private int versionNum = 3;
    private File localPath = null;
    private List< String > filePathList = new ArrayList<>();

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
        for ( int i = 0; i < objectNum / 2; i++ ) {
            objectNames.add( objectNameBase + "/4708-" + i );
        }
        for ( int i = objectNum / 2; i < objectNum; i++ ) {
            objectNames.add( objectNameBase + ":4708-" + i );
        }
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
        for ( int i = 0; i < objectNum; i++ ) {
            for ( int j = 0; j < versionNum; j++ ) {
                s3Client.putObject(
                        new PutObjectRequest( bucketName, objectNames.get( i ),
                                new File( filePathList.get( j ) ) ) );
            }
        }
    }

    @Test
    private void test1() {
        String prefix = "dir";
        String delimiter = "/";
        String keyMarker = objectNames.get( 0 );
        String versionIdMarker = versionNum + ".0";

        // list versions by prefix/delimiter/versionIdMarker/keyMarker
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withDelimiter( delimiter )
                .withPrefix( prefix ).withKeyMarker( keyMarker )
                .withVersionIdMarker( versionIdMarker ) );

        // expected results
        Collections.sort( objectNames );
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        for ( int i = objectNum / 2; i <= objectNum / 2 + 1000 / 3; i++ ) {
            if ( i == objectNum / 2 + 1000 / 3 ) {
                expMap.add( objectNames.get( i ), versionNum + ".0" );
            } else {
                for ( int j = versionNum; j > 0; j-- ) {
                    expMap.add( objectNames.get( i ), j + ".0" );
                }
            }
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), true,
                "vsList.isTruncated() must be true" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(), expMap );

        // list versions by prefix/delimiter/versionIdMarker/keyMarker
        String keyMarker1 = vsList.getNextKeyMarker();
        String versionIdMarker1 = vsList.getNextVersionIdMarker();
        VersionListing vsList1 = s3Client
                .listVersions( new ListVersionsRequest()
                        .withBucketName( bucketName ).withDelimiter( delimiter )
                        .withPrefix( prefix ).withKeyMarker( keyMarker1 )
                        .withVersionIdMarker( versionIdMarker1 ) );

        // expected results
        MultiValueMap< String, String > expMap1 = new LinkedMultiValueMap<>();
        for ( int i = objectNum / 2 + 1000 / 3; i < objectNum; i++ ) {
            if ( i == objectNum / 2 + 1000 / 3 ) {
                for ( int j = versionNum - 1; j > 0; j-- ) {
                    expMap1.add( objectNames.get( i ), j + ".0" );
                }
            } else {
                for ( int j = versionNum; j > 0; j-- ) {
                    expMap1.add( objectNames.get( i ), j + ".0" );
                }
            }
        }
        // check
        Assert.assertEquals( vsList1.isTruncated(), false,
                "vsList1.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList1, new ArrayList< String >(),
                expMap1 );
        runSuccess = true;
    }

    @Test
    private void test2() {
        String prefix = "dir";
        String delimiter = "/";
        String keyMarker = "air";
        String versionIdMarker = ( versionNum + 1 ) + ".0";

        // list versions by prefix/delimiter/versionIdMarker/keyMarker
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withDelimiter( delimiter )
                .withPrefix( prefix ).withKeyMarker( keyMarker )
                .withVersionIdMarker( versionIdMarker ) );

        // expected results
        Collections.sort( objectNames );
        List< String > expCommonPrefixes = new ArrayList<>();
        expCommonPrefixes.add( "dir/" );

        MultiValueMap< String, String > expMap = new LinkedMultiValueMap<>();
        for ( int i = objectNum / 2; i <= objectNum / 2 + 1000 / 3 - 1; i++ ) {
            for ( int j = versionNum; j > 0; j-- ) {
                expMap.add( objectNames.get( i ), j + ".0" );
            }
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), true,
                "vsList.isTruncated() must be true" );
        S3Utils.checkListVSResults( vsList, expCommonPrefixes, expMap );

        // list versions by prefix/delimiter/versionIdMarker/keyMarker
        String keyMarker1 = vsList.getNextKeyMarker();
        String versionIdMarker1 = vsList.getNextVersionIdMarker();
        VersionListing vsList1 = s3Client
                .listVersions( new ListVersionsRequest()
                        .withBucketName( bucketName ).withDelimiter( delimiter )
                        .withPrefix( prefix ).withKeyMarker( keyMarker1 )
                        .withVersionIdMarker( versionIdMarker1 ) );

        // expected results
        MultiValueMap< String, String > expMap1 = new LinkedMultiValueMap< String, String >();
        for ( int i = objectNum / 2 + 1000 / 3; i < objectNum; i++ ) {
            for ( int j = versionNum; j > 0; j-- ) {
                expMap1.add( objectNames.get( i ), j + ".0" );
            }
        }
        // check
        Assert.assertEquals( vsList1.isTruncated(), false,
                "vsList1.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList1, new ArrayList< String >(),
                expMap1 );
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
