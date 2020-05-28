package com.sequoiacm.workspace.serial;

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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description WorkspaceRemoveNotExistSite2187.java 删除ws不存在的站点
 * @author luweikang
 * @date 2018年8月28日
 */
public class WorkspaceRemoveNotExistSite2187 extends TestScmBase {

    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private String siteName = "site2187";
    private String wsName = "ws2187";

    @BeforeClass
    public void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        session = TestScmTools.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws ScmException, InterruptedException {
        int siteNum = ScmInfo.getSiteNum();
        ScmWorkspace ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
        try {
            ScmWorkspaceUtil.wsRemoveSite( ws, siteName );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT,
                    e.getMessage() );
        }
        try {
            ScmWorkspaceUtil.wsRemoveSite( ws, branchSite.getSiteName() );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT );
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
