package com.sequoiacm.fulltextsearch.serial;

import com.sequoiacm.exception.ScmError;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description SCM-3039:工作区不存在，inspect工作区
 * @author wuyan
 * @Date 2020.09.17
 * @version 1.00
 */

public class WsIndex3039 extends TestScmBase {
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String wsName = "ws3039";

    @BeforeClass
    private void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        int siteNum = ScmInfo.getSiteNum();
        ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    private void test() throws Exception {
        ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
        try {
            ScmFactory.Fulltext.inspectIndex( ws );
            Assert.fail( "inspectIndex should be failed!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.WORKSPACE_NOT_EXIST,
                    e.getMessage() );
        }

    }

    @AfterClass
    private void tearDown() {
        session.close();
    }
}
