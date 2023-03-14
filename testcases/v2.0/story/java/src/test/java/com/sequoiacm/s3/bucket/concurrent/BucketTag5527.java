package com.sequoiacm.s3.bucket.concurrent;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5527:并发设置标签和删除相同桶标签
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BucketTag5527 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private Map< String, String > map = new HashMap<>();
    private String bucketName = "bucket5527";
    private String tagKey = "key5527";
    private String tagValue = "value5527";
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

    @Test()
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new S3DeleteTag() );
        t.addWorker( new SCMSetTag() );
        t.run();

        // 获取结果
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        Map< String, String > customTag = bucket.getCustomTag();

        // 校验结果
        if ( customTag != null ) {
            Assert.assertEquals( customTag.get( tagKey ), tagValue );
        }
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

    private class S3DeleteTag {
        @ExecuteOrder(step = 1)
        private void run() {
            s3Client.deleteBucketTaggingConfiguration( bucketName );
        }
    }

    private class SCMSetTag {
        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            bucket.setCustomTag( map );
        }
    }

    private void initTag() {
        map.put( tagKey, tagValue );
    }
}
