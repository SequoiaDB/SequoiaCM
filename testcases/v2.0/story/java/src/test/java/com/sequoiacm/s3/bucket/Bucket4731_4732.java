package com.sequoiacm.s3.bucket;

import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-4731 :: 创建桶，设置桶版本控制策略 SCM-4732 :: 桶启用版本控制
 * @author wuyan
 * @Date 2022.07.07
 * @version 1.00
 */
public class Bucket4731_4732 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String bucketName1 = "bucket4731";
    private String bucketName2 = "bucket4731enable";
    private String bucketName3 = "bucket4731suspend";

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws ScmException {
        ScmBucket bucketinfo1 = ScmFactory.Bucket.createBucket( ws,
                bucketName1 );
        Assert.assertEquals( bucketinfo1.getVersionStatus().name(),
                "Disabled" );

        ScmBucket bucketinfo2 = ScmFactory.Bucket.createBucket( ws,
                bucketName2 );
        bucketinfo2.enableVersionControl();
        Assert.assertEquals( bucketinfo2.getVersionStatus().name(), "Enabled" );

        ScmBucket bucketinfo3 = ScmFactory.Bucket.createBucket( ws,
                bucketName3 );
        bucketinfo3.suspendVersionControl();
        Assert.assertEquals( bucketinfo3.getVersionStatus().name(),
                "Suspended" );

        // testcase4732:开启版本控制
        bucketinfo1.enableVersionControl();
        Assert.assertEquals( bucketinfo1.getVersionStatus().name(), "Enabled" );
        bucketinfo3.enableVersionControl();
        Assert.assertEquals( bucketinfo3.getVersionStatus().name(), "Enabled" );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Bucket.deleteBucket( session, bucketName1 );
                ScmFactory.Bucket.deleteBucket( session, bucketName2 );
                ScmFactory.Bucket.deleteBucket( session, bucketName3 );
            }
        } finally {
            session.close();
        }
    }
}
