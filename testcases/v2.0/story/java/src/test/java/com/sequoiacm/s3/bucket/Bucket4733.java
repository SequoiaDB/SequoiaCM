package com.sequoiacm.s3.bucket;

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
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Descreption SCM-4733:桶禁用版本控制
 * @author wuyan
 * @Date 2022.07.07
 * @version 1.00
 */
public class Bucket4733 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String bucketName1 = "bucket4733";
    private String bucketName2 = "bucket4733enable";

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
        bucketinfo1.suspendVersionControl();
        Assert.assertEquals( bucketinfo1.getVersionStatus().name(),
                "Suspended" );

        ScmBucket bucketinfo2 = ScmFactory.Bucket.createBucket( ws,
                bucketName2 );
        bucketinfo2.enableVersionControl();
        bucketinfo2.suspendVersionControl();
        Assert.assertEquals( bucketinfo2.getVersionStatus().name(),
                "Suspended" );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Bucket.deleteBucket( session, bucketName1 );
                ScmFactory.Bucket.deleteBucket( session, bucketName2 );
            }
        } finally {
            session.close();
        }
    }
}
