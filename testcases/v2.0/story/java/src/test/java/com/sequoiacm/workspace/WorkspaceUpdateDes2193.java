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
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description WorkspaceUpdateDes2193.java 更新ws描述信息
 * @author luweikang
 * @date 2018年8月28日
 */
public class WorkspaceUpdateDes2193 extends TestScmBase {

    private static SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws2193";

    @BeforeClass
    public void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws ScmException, InterruptedException {
        int siteNum = ScmInfo.getSiteNum();
        ScmWorkspace ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
        String description1 = "I am description 1";
        ws.updatedDescription( description1 );
        Thread.sleep( 2000 );
        checkWsDescription1( description1 );
        String description2 = "I am new description 2";
        ws.updatedDescription( description2 );
        Thread.sleep( 2000 );
        checkWsDescription1( description2 );
        String description3 = "";
        ws.updatedDescription( description3 );
        Thread.sleep( 2000 );
        checkWsDescription1( description3 );
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

    private void checkWsDescription1( String description ) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        Assert.assertEquals( ws.getDescription(), description );
    }
}
