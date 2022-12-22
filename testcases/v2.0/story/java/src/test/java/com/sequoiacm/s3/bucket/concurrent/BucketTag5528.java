package com.sequoiacm.s3.bucket.concurrent;

import java.util.HashMap;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.TagSet;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5528:并发获取和删除桶标签
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BucketTag5528 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private TagSet tagSet = new TagSet();
    private String bucketName = "bucket5528";
    private String tagKey = "key5528";
    private String tagValue = "value5528";
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        initTag();
    }

    @Test()
    public void test() throws Exception {
        S3Utils.setBucketTag( s3Client, bucketName, tagSet );

        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new S3Get() );
        t.addWorker( new SCMDelete() );
        // 结果在线程中校验
        t.run();
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

    private class S3Get {
        @ExecuteOrder(step = 1)
        private void run() {
            BucketTaggingConfiguration configuration = s3Client
                    .getBucketTaggingConfiguration( bucketName );
            TagSet tag = configuration.getTagSet();
            if ( tag.getTag( tagKey ) != null ) {
                Assert.assertEquals( tag.getTag( tagKey ), tagValue );
            } else {
                Assert.assertEquals( tag.getAllTags(),
                        new HashMap< String, String >() );
            }
        }
    }

    private class SCMDelete {
        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            bucket.deleteCustomTag();
        }
    }

    private void initTag() {
        tagSet.setTag( tagKey, tagValue );
    }
}
