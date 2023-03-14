package com.sequoiacm.workspace;

import java.io.IOException;
import java.util.List;

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
 * @Description WorkspaceRemoveSite2192.java ws删除所有分站点
 * @author luweikang
 * @date 2018年8月28日
 */
public class WorkspaceRemoveSite2192 extends TestScmBase {

    private static SiteWrapper siteM = null;
    private static List< SiteWrapper > branchSiteList = null;
    private ScmSession sessionM = null;
    private ScmSession sessionA = null;
    private String wsName = "ws2192";

    @BeforeClass
    public void setUp() throws Exception {
        siteM = ScmInfo.getRootSite();
        branchSiteList = ScmInfo.getBranchSites( ScmInfo.getSiteNum() - 1 );
        sessionM = ScmSessionUtils.createSession( siteM );
        ScmWorkspaceUtil.deleteWs( wsName, sessionM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws ScmException, InterruptedException, IOException {
        int siteNum = ScmInfo.getSiteNum();
        ScmWorkspace ws = ScmWorkspaceUtil.createWS( sessionM, wsName,
                siteNum );
        for ( int i = 0; i < branchSiteList.size(); i++ ) {
            System.err.println(branchSiteList.get(i).getSiteName());
            ScmWorkspaceUtil.wsRemoveSite( ws,
                    branchSiteList.get( i ).getSiteName() );
        }
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            ScmWorkspaceUtil.deleteWs( wsName, sessionM );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

}
