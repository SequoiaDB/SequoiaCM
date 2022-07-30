package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
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
 * @descreption SCM-4691 :: 禁用版本控制存在删除标记的对象，查询对象版本列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4691 extends TestScmBase {
    private String bucketName = "bucket4691";
    private String keyName = "object4691";
    private String fileContent = "content4691";
    private int objectTotalNum = 5;
    private List< String > keyNameList = new ArrayList<>();
    private List< String > expDeleteMarKerList = new ArrayList<>();
    private List< String > expVersionsKeyNameList = new ArrayList<>();
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );

        // put objects
        for ( int i = 0; i < objectTotalNum; i++ ) {
            String curKeyName = keyName + "_" + i;
            s3Client.putObject( bucketName, curKeyName, fileContent );
            expVersionsKeyNameList.add( curKeyName );
            keyNameList.add( curKeyName );
        }
        // delete object key = "object4691_1" and "object4691_4"
        s3Client.deleteObject( bucketName, keyNameList.get( 1 ) );
        expDeleteMarKerList.add( keyNameList.get( 1 ) );

        s3Client.deleteObject( bucketName, keyNameList.get( 4 ) );
        expDeleteMarKerList.add( keyNameList.get( 4 ) );

        // set bucket version status is Suspended
        S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
        // delete object key = "object4691_0"
        s3Client.deleteObject( bucketName, keyNameList.get( 0 ) );
        expDeleteMarKerList.add( keyNameList.get( 0 ) );
    }

    @Test
    public void test() throws Exception {
        VersionListing versionList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        List< S3VersionSummary > verList = versionList.getVersionSummaries();
        checklistVersionsResult( verList );
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

    private void checklistVersionsResult( List< S3VersionSummary > versions ) {
        Collections.sort( expVersionsKeyNameList );
        Collections.sort( expDeleteMarKerList );
        Assert.assertEquals( versions.size(),
                expVersionsKeyNameList.size() + expDeleteMarKerList.size(),
                "The number of results returned does not match the expected value" );
        String lastName = "start";
        int lastVersionId = 0;
        for ( int i = 0; i < expVersionsKeyNameList.size(); i++ ) {
            Assert.assertEquals( versions.get( i ).getKey(),
                    expVersionsKeyNameList.get( i ), "version key is wrong" );
            Assert.assertEquals( versions.get( i ).isDeleteMarker(), false,
                    "isDeleteMarKer is wrong" );
            if ( versions.get( i ).getKey().equals(lastName)) {
                int curVersion = Integer.parseInt( versions.get( i ).getVersionId().split("\\.")[0] );
                Assert.assertTrue( curVersion < lastVersionId);
            } else {
                lastName = versions.get( i ).getKey();
                lastVersionId = Integer.parseInt( versions.get( i ).getVersionId().split("\\.")[0] );
            }
        }

        // versions = expVersionsKeyNameList + expDeleteMarKersList
        // deleteMarKersList should start from expVersionsKeyNameList.size()+1
        for ( int i = 0; i < expDeleteMarKerList.size(); i++ ) {
            Assert.assertEquals(
                    versions.get( expVersionsKeyNameList.size() + i ).getKey(),
                    expDeleteMarKerList.get( i ),
                    "deleteMarKerList key is wrong" );
            Assert.assertEquals(
                    versions.get( expVersionsKeyNameList.size() + i )
                            .isDeleteMarker(),
                    true, "isDeleteMarKer is wrong" );
            if ( versions.get( expVersionsKeyNameList.size() + i ).getKey()
                    .equals( keyNameList.get( 0 ) ) ) {
                Assert.assertEquals(
                        versions.get( expVersionsKeyNameList.size() + i )
                                .getVersionId(),
                        "null", "versionId is wrong" );
            } else {
                Assert.assertNotEquals(
                        versions.get( expVersionsKeyNameList.size() + i )
                                .getVersionId(),
                        "null", "versionId is wrong" );
            }
        }
    }
}
