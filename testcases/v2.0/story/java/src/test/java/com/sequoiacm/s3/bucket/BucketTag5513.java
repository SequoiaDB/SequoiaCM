package com.sequoiacm.s3.bucket;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.TagSet;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-5513:设置桶标签超出上限
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BucketTag5513 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private TagSet tagSet = new TagSet();
    private Map< String, String > map = new HashMap<>();
    private String bucketName = "bucket5513";
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        initTag();
    }

    @Test
    public void test() throws ScmException {
        testS3API();

        testSCMAPI();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void initTag() {
        String baseKey = "test";
        String baseValue = "value";
        for ( int i = 0; i < 49; i++ ) {
            tagSet.setTag( baseKey + i, baseValue + i );
            map.put( baseKey + i, baseValue + i );
        }
    }

    private void testSCMAPI() throws ScmException {
        String baseKey = "test";
        String baseValue = "value";
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );

        // 设置标签<50个
        bucket.setCustomTag( map );
        Assert.assertEquals( bucket.getCustomTag(), map );

        // 设置标签=50个
        map.put( baseKey + 49, baseValue + 49 );
        bucket.setCustomTag( map );
        Assert.assertEquals( bucket.getCustomTag(), map );

        // 设置标签>50个
        try {
            map.put( baseKey + 50, baseValue + 50 );
            bucket.setCustomTag( map );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(),
                    ScmError.BUCKET_CUSTOMTAG_TOO_LARGE );
        }
    }

    private void testS3API() {
        String baseKey = "test";
        String baseValue = "value";

        // 设置标签<50个
        S3Utils.setBucketTag( s3Client, bucketName, tagSet );
        TagSet actTagSet = s3Client.getBucketTaggingConfiguration( bucketName )
                .getTagSet();
        Assert.assertEquals(  actTagSet.getAllTags() ,tagSet.getAllTags());

        // 设置标签=50个
        tagSet.setTag( baseKey + 49, baseValue + 49 );
        S3Utils.setBucketTag( s3Client, bucketName, tagSet );
        actTagSet = s3Client.getBucketTaggingConfiguration( bucketName )
                .getTagSet();
        Assert.assertEquals(  actTagSet.getAllTags(),tagSet.getAllTags() );

        // 设置标签>50个
        try {
            tagSet.setTag( baseKey + 50, baseValue + 50 );
            S3Utils.setBucketTag( s3Client, bucketName, tagSet );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "BadRequest" ) ) {
                throw e;
            }
        }
    }
}
