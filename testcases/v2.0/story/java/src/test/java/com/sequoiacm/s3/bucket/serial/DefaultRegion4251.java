package com.sequoiacm.s3.bucket.serial;

import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @descreption SCM-4251 :: 修改默认region，在默认region下创建桶
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class DefaultRegion4251 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws_4251";
    private ScmWorkspace ws_test = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4251";

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        int siteNum = ScmInfo.getSiteNum();

        ws_test = ScmWorkspaceUtil.createS3WS( session, wsName );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    public void test() throws ScmException, IOException {
        ScmFactory.S3.setDefaultRegion( session, wsName );

        Assert.assertEquals( ScmFactory.S3.getDefaultRegion( session ),
                wsName );

        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws_test,
                bucketName );

        Assert.assertEquals( bucket.getWorkspace(), wsName );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                ScmFactory.Bucket.deleteBucket( session, bucketName );
                ScmFactory.S3.setDefaultRegion( session, s3WorkSpaces );
                ScmWorkspaceUtil.deleteWs( wsName, session );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
