package com.sequoiacm.workspace.serial;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmContentLocation;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSftpDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
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
        session = TestScmTools.createSession( site );
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
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmWorkspaceUtil.deleteWs( wsName, session );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}