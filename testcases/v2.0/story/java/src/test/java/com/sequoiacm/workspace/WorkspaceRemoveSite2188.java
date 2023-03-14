package com.sequoiacm.workspace;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description WorkspaceRemoveSite2188.java ws删除不同ws的相同站点
 * @author luweikang
 * @date 2018年8月28日
 */
public class WorkspaceRemoveSite2188 extends TestScmBase {

    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private String wsNameA = "ws2188_A";
    private String wsNameB = "ws2188_B";

    @BeforeClass
    public void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        session = ScmSessionUtils.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( wsNameA, session );
        ScmWorkspaceUtil.deleteWs( wsNameB, session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws ScmException, InterruptedException {
        int siteNum = ScmInfo.getSiteNum();
        ScmWorkspace wsA = ScmWorkspaceUtil.createWS( session, wsNameA,
                siteNum );
        ScmWorkspace wsB = ScmWorkspaceUtil.createWS( session, wsNameB,
                siteNum );

        ScmWorkspaceUtil.wsRemoveSite( wsA, branchSite.getSiteName() );
        ScmWorkspaceUtil.wsRemoveSite( wsB, branchSite.getSiteName() );
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            ScmWorkspaceUtil.deleteWs( wsNameA, session );
            ScmWorkspaceUtil.deleteWs( wsNameB, session );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

}
