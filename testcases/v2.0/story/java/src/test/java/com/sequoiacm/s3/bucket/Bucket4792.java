package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @Descreption SCM-4792:SCM API创建桶，S3接口更新桶状态
 * @author wuyan
 * @Date 2022.07.14
 * @version 1.00
 */
public class Bucket4792 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String bucketName = "bucket4792";
    private AmazonS3 s3Client;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws ScmException {
        ScmFactory.Bucket.createBucket( ws, bucketName );
        BucketVersioningConfiguration configuration = new BucketVersioningConfiguration()
                .withStatus( "Enabled" );
        SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest = new SetBucketVersioningConfigurationRequest(
                bucketName, configuration );
        s3Client.setBucketVersioningConfiguration(
                setBucketVersioningConfigurationRequest );
        Assert.assertEquals( s3Client
                .getBucketVersioningConfiguration( bucketName ).getStatus(),
                "Enabled" );

        ScmBucket bucketinfo = ScmFactory.Bucket.getBucket( session,
                bucketName );
        Assert.assertEquals( bucketinfo.getVersionStatus().name(), "Enabled" );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Bucket.deleteBucket( session, bucketName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }

            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
