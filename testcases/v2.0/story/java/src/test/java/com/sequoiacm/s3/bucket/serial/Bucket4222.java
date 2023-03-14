package com.sequoiacm.s3.bucket.serial;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-4222:S3接口创建桶，SCM API操作桶
 * @Author YiPan
 * @CreateDate 2022/5/12
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4222 extends TestScmBase {
    private final String bucketName = "bucket4222no";
    private List< String > expBucektNames = new ArrayList<>();
    private List< String > actBucektNames = new ArrayList<>();
    private List< String > envBuckets;
    private ScmSession session;
    private AmazonS3 s3Client;
    private int bucketNum = 10;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        envBuckets = S3Utils.getEnvBuckets( TestScmBase.scmUserName );
        SiteWrapper rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        s3Client = S3Utils.buildS3Client();
        for ( int i = 0; i < bucketNum; i++ ) {
            S3Utils.clearBucket( s3Client, bucketName + i );
        }
    }

    @Test
    public void test() throws ScmException {
        // 创建多个桶
        for ( int i = 0; i < bucketNum; i++ ) {
            s3Client.createBucket( bucketName + i );
            expBucektNames.add( bucketName + i );
        }

        // 列取桶
        ScmCursor< ScmBucket > scmBucketScmCursor = ScmFactory.Bucket
                .listBucket( session, s3WorkSpaces, TestScmBase.scmUserName );
        while ( scmBucketScmCursor.hasNext() ) {
            actBucektNames.add( scmBucketScmCursor.getNext().getName() );
        }
        scmBucketScmCursor.close();
        actBucektNames.removeAll( envBuckets );
        Assert.assertEqualsNoOrder( actBucektNames.toArray(),
                expBucektNames.toArray() );

        // 查询桶
        for ( int i = 0; i < bucketNum; i++ ) {
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName + i );
            Assert.assertEquals( bucket.getName(), bucketName + i );
            Assert.assertEquals( bucket.getWorkspace(), s3WorkSpaces );
            Assert.assertEquals( bucket.getCreateUser(),
                    TestScmBase.scmUserName );
        }

        // 删除一半桶
        for ( int i = 0; i < bucketNum / 2; i++ ) {
            ScmFactory.Bucket.deleteBucket( session, bucketName + i );
        }

        // 统计桶
        long num = ScmFactory.Bucket.countBucket( session, s3WorkSpaces,
                TestScmBase.scmUserName );
        Assert.assertEquals( num - envBuckets.size(), bucketNum / 2 );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() {
        try {
            if ( runSuccess ) {
                for ( int i = bucketNum / 2; i < bucketNum; i++ ) {
                    S3Utils.clearBucket( s3Client, bucketName + i );
                }
            }
        } finally {
            session.close();
            s3Client.shutdown();
        }
    }

}