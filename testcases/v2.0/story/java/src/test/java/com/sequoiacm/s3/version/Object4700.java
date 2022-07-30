package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.UUID;

/**
 * @descreption SCM-4700 ::
 *              带prefix、keyMarker和versionIdMarker查询对象版本列表，不匹配keyMarker
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4700 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4700";
    private String[] objectNames = { "dir4700&dir4700A&dir4700AB",
            "dir4700&subdir4700A", "dir4700A", "dir4700B" };
    private AmazonS3 s3Client = null;
    private int versionNum = 3;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
        for ( String objectName : objectNames ) {
            for ( int i = 0; i < versionNum; i++ ) {
                s3Client.putObject( bucketName, objectName,
                        "" + UUID.randomUUID() );
            }
        }
    }

    @Test
    private void test() throws Exception {
        String prefix = "dir";
        // keyMarker does not exist
        String keyMarker = "air4700C";
        String versionIdMarker = String.valueOf( versionNum ) + ".0";
        // list by prefix/keyMarker/versionIdMarker
        VersionListing vsList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName )
                        .withPrefix( prefix ).withKeyMarker( keyMarker )
                        .withVersionIdMarker( versionIdMarker ) );

        // expected results
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        for ( String objectName : objectNames ) {
            for ( int i = versionNum; i > 0; i-- ) {
                expMap.add( objectName, i + ".0" );
            }
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), false,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(), expMap );

        String prefix1 = "air";
        // keyMarker does not exist
        String keyMarker1 = "dir4700/dir4700A/dir4700AB";
        String versionIdMarker1 = String.valueOf( versionNum ) + ".0";
        // list by prefix/keyMarker/versionIdMarker
        VersionListing vsList1 = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName )
                        .withPrefix( prefix1 ).withKeyMarker( keyMarker1 )
                        .withVersionIdMarker( versionIdMarker1 ) );
        // check
        S3Utils.checkListVSResults( vsList1, new ArrayList< String >(),
                new LinkedMultiValueMap< String, String >() );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
