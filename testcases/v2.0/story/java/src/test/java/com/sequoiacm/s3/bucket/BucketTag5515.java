package com.sequoiacm.s3.bucket;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.TagSet;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-5515:桶标签SCM API和S3 API互通性测试
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BucketTag5515 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private TagSet tagSet = new TagSet();
    private Map< String, String > map = new HashMap<>();
    private TagSet newTagSet = new TagSet();
    private Map< String, String > newMap = new HashMap<>();
    private String bucketName = "bucket5515";
    private ScmWorkspace ws;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( TestScmBase.s3WorkSpaces,
                session );
        initTag();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws ScmException {
        testS3API();

        S3Utils.clearBucket( s3Client, bucketName );

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
        newTagSet.setTag( baseKey, baseValue );
        newMap.put( baseKey, baseValue );
    }

    private void testSCMAPI() throws ScmException {
        // s3 api创建桶设置标签
        s3Client.createBucket( bucketName );
        S3Utils.setBucketTag( s3Client, bucketName, tagSet );

        // scm api获取标签
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        Assert.assertEquals( bucket.getCustomTag(), map );

        // scm api更新标签
        bucket.setCustomTag( newMap );
        Assert.assertEquals( bucket.getCustomTag(), newMap );

        // scm api删除标签
        bucket.deleteCustomTag();
        Assert.assertEquals( bucket.getCustomTag(),
                new HashMap< String, String >() );
    }

    private void testS3API() throws ScmException {
        // scm api创建桶设置标签
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.setCustomTag( map );

        // s3 api获取标签
        TagSet actTagSet = s3Client.getBucketTaggingConfiguration( bucketName )
                .getTagSet();
        Assert.assertEquals( actTagSet.getAllTags(), tagSet.getAllTags() );

        // s3 api更新标签
        S3Utils.setBucketTag( s3Client, bucketName, newTagSet );
        actTagSet = s3Client.getBucketTaggingConfiguration( bucketName )
                .getTagSet();
        Assert.assertEquals( actTagSet.getAllTags(), newTagSet.getAllTags() );

        // s3 api删除表
        s3Client.deleteBucketTaggingConfiguration( bucketName );
        actTagSet = s3Client.getBucketTaggingConfiguration( bucketName )
                .getTagSet();
        Assert.assertEquals( actTagSet.getAllTags(),
                new TagSet().getAllTags() );

    }
}
