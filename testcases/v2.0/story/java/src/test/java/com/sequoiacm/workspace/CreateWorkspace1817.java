/**
 *
 */
package com.sequoiacm.workspace;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description CreateWorkspace1817.java 创建ws指定不同数据源站点
 * @author luweikang
 * @date 2018年6月22日
 */
public class CreateWorkspace1817 extends TestScmBase {

    private String wsName = "ws1817";
    private ScmSession session = null;
    private SiteWrapper site = null;

    @BeforeClass
    private void setUp() throws Exception {

        site = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {
        int siteNum = ScmInfo.getSiteNum();
        ScmWorkspace ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );

        Assert.assertEquals( ws.getDataLocations().size(), siteNum );
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
