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
 * @descreption SCM-4688 :: 查询桶中对象版本列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4688 extends TestScmBase {
    private String bucketName = "bucket4688";
    private String keyName = "object4688";
    private String fileContent = "content4688";
    private List< String > exKeyNameList = new ArrayList<>();
    private int objectTotalNum = 1500;
    private int objectOnceQueryNum = 1000;
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        // create bucket
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );

        // put multiple objects
        for ( int i = 0; i < objectTotalNum; i++ ) {
            String currentKeyName = keyName + "_" + i;
            s3Client.putObject( bucketName, currentKeyName, fileContent );
            exKeyNameList.add( currentKeyName );
        }
    }

    @Test
    public void test() {
        VersionListing versionList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName )
                        .withMaxResults( objectOnceQueryNum ) );
        int queryTime = 0;
        while ( true ) {
            queryTime++;
            List< S3VersionSummary > verList = versionList
                    .getVersionSummaries();
            checkListObjectsResult( verList, queryTime );
            if ( versionList.isTruncated() ) {
                versionList = s3Client.listNextBatchOfVersions( versionList );
            } else {
                break;
            }
        }

        Assert.assertEquals( queryTime, objectTotalNum / objectOnceQueryNum + 1,
                "the query time is wrong!" );

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

    private void checkListObjectsResult( List< S3VersionSummary > versions,
            int queryTime ) {
        Collections.sort( exKeyNameList );
        int startKeyNum = ( queryTime - 1 ) * objectOnceQueryNum;
        if ( queryTime == 2 ) {
            Assert.assertEquals( versions.size(), 500,
                    "The result of the last round of return is not equal to the expected result" );
        } else {
            Assert.assertEquals( versions.size(), objectOnceQueryNum,
                    "The number of results returned does not match the expected value" );
        }
        for ( int i = 0; i < versions.size(); i++ ) {
            Assert.assertEquals( versions.get( i ).getKey(),
                    exKeyNameList.get( startKeyNum ),
                    "commonPrefixes is wrong" );
            Assert.assertEquals( versions.get( i ).getVersionId(), "null",
                    "version id is not null" );
            startKeyNum++;
        }
    }
}
