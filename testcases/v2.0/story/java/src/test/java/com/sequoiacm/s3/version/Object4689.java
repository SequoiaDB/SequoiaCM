package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @descreption SCM-4689 :: 对象存在多个版本，查询对象版本列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4689 extends TestScmBase {
    private String bucketName = "bucket4689";
    private String keyName = "object4689";
    private String repeatedKeyName = keyName + "_1";
    private String fileContent = "content4689";
    private List< String > expResultList = new ArrayList<>();
    private int objectTotalNum = 5;
    private String latestVersionId = null;
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        // create bucket and set bucket version status
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );

        // put multiple objects
        for ( int i = 0; i < objectTotalNum; i++ ) {
            String currentKeyName = keyName + "_" + i;
            s3Client.putObject( bucketName, currentKeyName, fileContent );
            expResultList.add( currentKeyName );
        }

        // put object key = "object4689_1" twice again
        s3Client.putObject( bucketName, repeatedKeyName, fileContent );
        expResultList.add( repeatedKeyName );
        PutObjectResult result = s3Client.putObject( bucketName,
                repeatedKeyName, fileContent );
        latestVersionId = result.getVersionId();
        expResultList.add( repeatedKeyName );
    }

    @Test
    public void test() throws Exception {
        VersionListing versionList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        List< S3VersionSummary > verList = versionList.getVersionSummaries();
        checkListObjectsResult( verList );
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

    private void checkListObjectsResult( List< S3VersionSummary > versions ) {
        Collections.sort( expResultList );
        Assert.assertEquals( versions.size(), expResultList.size(),
                "The number of results returned does not match the expected value" );
        for ( int i = 0; i < versions.size(); i++ ) {
            Assert.assertEquals( versions.get( i ).getKey(),
                    expResultList.get( i ), "key order is wrong" );
            if ( versions.get( i ).getKey().equals( repeatedKeyName ) ) {
                if ( versions.get( i ).getVersionId()
                        .equals( latestVersionId ) ) {
                    Assert.assertEquals( versions.get( i ).isLatest(), true,
                            "isLatest is wrong" );
                } else {
                    Assert.assertEquals( versions.get( i ).isLatest(), false,
                            "isLatest is wrong" );
                }
            } else {
                Assert.assertEquals( versions.get( i ).isLatest(), true,
                        "isLatest is wrong" );
            }
        }
    }
}
