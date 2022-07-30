package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @descreption SCM-4692 :: 带前缀prefix查询对象版本列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4692 extends TestScmBase {
    private String bucketName = "bucket4692";
    private String[] keyName = { "dir1%dir2%test1_4692", "dir1%dir2%test2_4692",
            "test3_4692", "test4_4692" };
    private String prefix = "dir1";
    private String content = "object4692";
    private List< String > expEtagList = new ArrayList<>();
    private List< Date > expLastModifiedList = new ArrayList<>();
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );

        for ( int i = 0; i < keyName.length; i++ ) {
            String currentContent = content + S3Utils.getRandomString( i );
            s3Client.putObject( bucketName, keyName[ i ], currentContent );
            expEtagList.add( TestTools.getMD5( currentContent.getBytes() ) );
            if ( keyName[ i ].startsWith( prefix ) ) {
                S3Object obj = s3Client.getObject( bucketName, keyName[ i ] );
                expLastModifiedList
                        .add( obj.getObjectMetadata().getLastModified() );
            }
        }
    }

    @Test
    public void testGetObjectList() throws Exception {
        VersionListing versionList = s3Client
                .listVersions( new ListVersionsRequest()
                        .withBucketName( bucketName ).withPrefix( prefix ) );
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

    private void checklistVersionsResult( List< S3VersionSummary > versions )
            throws ParseException {
        Assert.assertEquals( versions.size(), 2,
                "The number of results returned does not match the expected value" );
        for ( int i = 0; i < 2; i++ ) {
            Assert.assertEquals( versions.get( i ).getKey(), keyName[ i ],
                    "versions' key is wrong" );
            Assert.assertEquals( versions.get( i ).getVersionId(), "null",
                    "versions' versionId is wrong" );
            Assert.assertEquals( versions.get( i ).getSize(),
                    ( long ) ( content.length() + i ),
                    "versions' size is wrong" );
            Assert.assertEquals( versions.get( i ).getETag(),
                    expEtagList.get( i ), "versions' Etag is wrong" );
            Assert.assertEquals( versions.get( i ).getLastModified().toString(),
                    expLastModifiedList.get( i ).toString(),
                    "'lastModified' is wrong" );
        }
    }
}
