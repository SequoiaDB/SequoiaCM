package com.sequoiacm.s3.bucket.concurrent;

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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5525:并发给相同桶设置标签
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BucketTag5525 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private TagSet tagSet = new TagSet();
    private Map< String, String > map = new HashMap<>();
    private String bucketName = "bucket5525";
    private String s3Tag = "s3Tag";
    private String s3Value = "s3Value";
    private String scmTag = "scmTag";
    private String scmValue = "scmValue";
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
        t.addWorker( new S3SetTag() );
        t.addWorker( new SCMSetTag() );
        t.run();

        // 获取结果
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        Map< String, String > customTag = bucket.getCustomTag();

        // 校验结果
        if ( customTag.get( s3Tag ) != null ) {
            Assert.assertEquals( customTag.get( s3Tag ), s3Value );
        } else {
            Assert.assertEquals( customTag.get( scmTag ), scmValue );
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

    private class S3SetTag {
        @ExecuteOrder(step = 1)
        private void run() {
            S3Utils.setBucketTag( s3Client, bucketName, tagSet );
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
        tagSet.setTag( s3Tag, s3Value );
        map.put( scmTag, scmValue );
    }
}
