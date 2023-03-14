package com.sequoiacm.workspace;

import com.sequoiacm.testcommon.ScmSessionUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5902:驱动端禁用工作区目录功能
 * @author ZhangYanan
 * @date 2023/02/02
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5902 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper site = null;
    private String wsName = "ws5902";
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    public void test() throws Exception {
        // 创建工作区，启用目录功能
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ws.disableDirectory();

        Assert.assertFalse( ws.isEnableDirectory() );

        // 创建工作区，禁用目录功能
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createDisEnableDirectoryWS( session, wsName,
                ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );

        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        try {
            ws.disableDirectory();
            Assert.fail( "预期失败，实际成功" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.CONFIG_SERVER_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        }
        Assert.assertFalse( ws.isEnableDirectory() );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }
}