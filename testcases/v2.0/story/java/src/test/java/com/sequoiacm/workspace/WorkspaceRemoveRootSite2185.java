package com.sequoiacm.workspace;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description WorkspaceRemoveRootSite2185.java ws删除主站点
 * @author luweikang
 * @date 2018年8月28日
 */
public class WorkspaceRemoveRootSite2185 extends TestScmBase {

    private static SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws2185";

    @BeforeClass
    public void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test
    public void test() throws ScmException, InterruptedException {
        int siteNum = ScmInfo.getSiteNum();
        ScmWorkspace ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
        try {
            ScmWorkspaceUtil.wsRemoveSite( ws, site.getSiteName() );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT,
                    e.getMessage() );
        }
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            ScmWorkspaceUtil.deleteWs( wsName, session );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
