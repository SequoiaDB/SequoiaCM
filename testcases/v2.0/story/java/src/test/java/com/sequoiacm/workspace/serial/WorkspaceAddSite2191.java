package com.sequoiacm.workspace.serial;

import java.io.IOException;

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
 * @Description WorkspaceAddSite2189.java ws重复添加相同站点
 * @author luweikang
 * @date 2018年8月28日
 */
public class WorkspaceAddSite2191 extends TestScmBase {

    private ScmSession session = null;
    private SiteWrapper site = null;
    private String wsName = "ws2191";
    private ScmWorkspace ws = null;

    @BeforeClass
    public void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test(groups = { "one", "twoSite", "fourSite" })
    public void test() throws ScmException, InterruptedException, IOException {
        int siteNum = ScmInfo.getSiteNum();
        ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
        try {
            ScmWorkspaceUtil.wsAddSite( ws, site );
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

