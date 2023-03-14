package com.sequoiacm.s3.bucket.concurrent;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-4867 :: 并发创建相同桶，设置相同版本控制状态
 * @author wuyan
 * @Date 2022.07.20
 * @version 1.00
 */
public class Bucket4867 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private final String bucketName = "bucket4867";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        CreateS3Bucket createS3Bucket = new CreateS3Bucket();
        CreateSCMBucket createSCMBucket = new CreateSCMBucket();
        es.addWorker( createS3Bucket );
        es.addWorker( createSCMBucket );
        es.run();

        if ( createS3Bucket.getRetCode() == 0 ) {
            // BUCKET_EXISTS:-850
            Assert.assertEquals( createSCMBucket.getRetCode(), -850,
                    createSCMBucket.getThrowable().getMessage() );
        } else {
            Assert.assertEquals( createSCMBucket.getRetCode(), 0 );
            // Status Code: 409; Error Code: BucketAlreadyOwnedByYou
            Assert.assertEquals( createS3Bucket.getRetCode(), 409,
                    createS3Bucket.getThrowable().getMessage() );
        }

        Assert.assertEquals( s3Client
                .getBucketVersioningConfiguration( bucketName ).getStatus(),
                "Enabled" );

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

    private class CreateS3Bucket extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                s3Client.createBucket( bucketName );
                BucketVersioningConfiguration configuration = new BucketVersioningConfiguration()
                        .withStatus( "Enabled" );
                SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest = new SetBucketVersioningConfigurationRequest(
                        bucketName, configuration );
                s3Client.setBucketVersioningConfiguration(
                        setBucketVersioningConfigurationRequest );
            } catch ( AmazonS3Exception e ) {
                saveResult( e.getStatusCode(), e );
            }
        }
    }

    private class CreateSCMBucket extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                ScmBucket bucket = ScmFactory.Bucket.createBucket( ws,
                        bucketName );
                bucket.enableVersionControl();
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            }
        }
    }
}
