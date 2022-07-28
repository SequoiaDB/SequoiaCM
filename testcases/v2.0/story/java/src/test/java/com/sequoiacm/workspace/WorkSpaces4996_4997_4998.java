package com.sequoiacm.workspace;

import com.sequoiacm.client.core.ScmWorkspace;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-4996:使用驱动创建、更新工作区优先站点 SCM-4997:使用驱动创建、更新工作区为不存在工作区
 *              SCM-4998:使用驱动创建开启目录的工作区，设置/更新优先站点
 * @author YiPan
 * @date 2022/7/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces4996_4997_4998 extends TestScmBase {
    private String wsNameNull = "ws_4996_null";
    private String wsName = "ws_4996_test";
    private String wsNameError = "ws_4997_error";
    private String wsNameDir = "ws_4998_Dir";
    private String rootSiteName;
    private String branchSiteName;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        rootSiteName = ScmInfo.getRootSite().getSiteName();
        branchSiteName = ScmInfo.getBranchSite().getSiteName();
        ScmWorkspaceUtil.deleteWs( wsNameError, session );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.deleteWs( wsNameNull, session );
        ScmWorkspaceUtil.deleteWs( wsNameDir, session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test4996() throws Exception {
        ScmWorkspace wsNull = createWS( session, wsNameNull, null, false );
        Assert.assertNull( wsNull.getPreferred() );
        wsNull.updatePreferred( rootSiteName );
        Assert.assertEquals( wsNull.getPreferred(), rootSiteName );

        ScmWorkspace ws = createWS( session, wsName, rootSiteName, false );
        Assert.assertEquals( ws.getPreferred(), rootSiteName );
        ws.updatePreferred( branchSiteName );
        Assert.assertEquals( ws.getPreferred(), branchSiteName );
    }

    @Test
    private void test4997() throws Exception {
        try {
            createWS( session, wsNameError, "errorSite", false );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        ScmWorkspace wsError = createWS( session, wsNameError, rootSiteName,
                false );
        try {
            wsError.updatePreferred( "errorSite" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test4998() throws Exception {
        ScmWorkspace wsDir = createWS( session, wsNameError, rootSiteName,
                true );
        Assert.assertEquals( wsDir.getPreferred(), rootSiteName );
        wsDir.updatePreferred( branchSiteName );
        Assert.assertEquals( wsDir.getPreferred(), branchSiteName );

    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            ScmWorkspaceUtil.deleteWs( wsNameError, session );
            ScmWorkspaceUtil.deleteWs( wsName, session );
            ScmWorkspaceUtil.deleteWs( wsNameNull, session );
            ScmWorkspaceUtil.deleteWs( wsNameDir, session );
        } finally {
            session.close();
        }
    }

    private static ScmWorkspace createWS( ScmSession session, String wsName,
            String preferred, boolean enable_directory ) throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations(
                ScmWorkspaceUtil.getDataLocationList( ScmInfo.getSiteNum() ) );
        conf.setMetaLocation(
                ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ) );
        if ( preferred != null ) {
            conf.setPreferred( preferred );
        }
        if ( enable_directory ) {
            conf.setEnableDirectory( enable_directory );
        }
        conf.setName( wsName );
        return ScmWorkspaceUtil.createWS( session, conf );
    }
}
