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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-5536:桶标签规则校验
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BucketTag5536 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private TagSet tagSet = new TagSet();
    private Map< String, String > map = new HashMap<>();
    private String bucketName = "bucket5536";
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
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

    private void testSCMAPI() throws ScmException {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        // key为null
        try {
            map.put( null, "test" );
            bucket.setCustomTag( map );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(),
                    ScmError.BUCKET_INVALID_CUSTOMTAG );
        }

        // key为空字符串
        try {
            map.clear();
            map.put( "", "test" );
            bucket.setCustomTag( map );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(),
                    ScmError.BUCKET_INVALID_CUSTOMTAG );
        }

        // value为null value为空字符串
        map.clear();
        map.put( "k1", null );
        map.put( "k2", "" );
        bucket.setCustomTag( map );
        Assert.assertEquals( bucket.getCustomTag(), map );
    }

    private void testS3API() {
        // key为null
        try {
            tagSet.setTag( null, "test" );
            S3Utils.setBucketTag( s3Client, bucketName, tagSet );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "InvalidTag" ) ) {
                throw e;
            }
        }

        // key为空字符串
        try {
            tagSet = new TagSet();
            tagSet.setTag( "", "test" );
            S3Utils.setBucketTag( s3Client, bucketName, tagSet );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "InvalidTag" ) ) {
                throw e;
            }
        }
        // value为null value为空字符串
        tagSet = new TagSet();
        tagSet.setTag( "k1", null );
        tagSet.setTag( "k2", "" );
        S3Utils.setBucketTag( s3Client, bucketName, tagSet );
        TagSet actTagSet = s3Client.getBucketTaggingConfiguration( bucketName )
                .getTagSet();
        Assert.assertEquals( actTagSet.getTag( "k1" ), "" );
        Assert.assertEquals( actTagSet.getTag( "k2" ), "" );
    }
}
