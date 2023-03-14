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
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-5514:桶不存在，操作桶标签
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BucketTag5514 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private TagSet tagSet = new TagSet();
    private Map< String, String > map = new HashMap<>();
    private String bucketName = "bucket5514";
    private ScmWorkspace ws;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( TestScmBase.s3WorkSpaces,
                session );
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
        for ( int i = 0; i < 10; i++ ) {
            tagSet.setTag( baseKey + i, baseValue + i );
            map.put( baseKey + i, baseValue + i );
        }
    }

    private void testSCMAPI() throws ScmException {
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        ScmFactory.Bucket.deleteBucket( session, bucketName );
        try {
            bucket.setCustomTag( map );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.BUCKET_NOT_EXISTS ) ) {
                throw e;
            }
        }
        
        Assert.assertNull( bucket.getCustomTag() );

        try {
            bucket.deleteCustomTag();
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.BUCKET_NOT_EXISTS ) ) {
                throw e;
            }
        }
    }

    private void testS3API() {
        try {
            S3Utils.setBucketTag( s3Client, bucketName, tagSet );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "NoSuchBucket" ) ) {
                throw e;
            }
        }

        Assert.assertNull(
                s3Client.getBucketTaggingConfiguration( bucketName ) );

        try {
            s3Client.deleteBucketTaggingConfiguration( bucketName );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "NoSuchBucket" ) ) {
                throw e;
            }
        }
    }
}
