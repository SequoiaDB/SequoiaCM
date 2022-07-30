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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @descreption SCM-4701 ::
 *              带prefix、keyMarker和versionIdMarker查询对象版本列表，不匹配versionIdMarker
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4701 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4701";
    private String[] objectNames = { "dir4701%4701A",
            "dir4701%dir4701A%dir4701AB", "dir4701A", "dir4701B" };
    private List< String > sortObjectNames = new ArrayList< String >();
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
            sortObjectNames.add( objectName );
        }
        Collections.sort( sortObjectNames );
    }

    @Test
    private void test() {
        String prefix = "dir";
        int index = 0;
        String keyMarker = sortObjectNames.get( index );
        String versionIdMarker = ( versionNum + 1 ) + ".0";
        // list by prefix/keyMarker/versionIdMarker
        VersionListing vsList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName )
                        .withPrefix( prefix ).withKeyMarker( keyMarker )
                        .withVersionIdMarker( versionIdMarker ) );
        // expected results
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap<>();
        for ( String objectName : sortObjectNames ) {
            for ( int i = versionNum; i > 0; i-- ) {
                expMap.add( objectName, i + ".0" );
            }
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), false,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(), expMap );
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
