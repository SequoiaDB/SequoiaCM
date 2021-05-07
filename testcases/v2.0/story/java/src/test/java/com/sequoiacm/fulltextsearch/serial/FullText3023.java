package com.sequoiacm.fulltextsearch.serial;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextModifiler;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description: SCM-3023 :: 工作区不存在，工作区更新索引； SCM-3027 :: 工作区不存在，工作区删除索引;
 *               SCM-3048 :: 工作区不存在，指定文件重新建索引
 * @author fanyu
 * @Date:2020/11/12
 * @version:1.0
 */
public class FullText3023 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws3023";
    private ScmWorkspace ws = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
    }

    @Test
    private void test() throws Exception {
        // 删除工作区
        ScmFactory.Workspace.deleteWorkspace( session, wsName );

        // 工作区更新索引
        try {
            ScmFactory.Fulltext.alterIndex( ws,
                    new ScmFulltextModifiler().newMode( ScmFulltextMode.async )
                            .newFileCondition( new BasicBSONObject() ) );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST
                    && e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                throw e;
            }
        }

        // 工作区删除索引
        try {
            ScmFactory.Fulltext.dropIndex( ws );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST
                    && e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                throw e;
            }
        }

        // 工作区不存在，指定文件重新建索引
        try {
            ScmFactory.Fulltext.rebuildFileIndex( ws,
                    new ScmId( "5fabc9e4400001007fe7e8de" ) );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST
                    && e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( session != null ) {
            session.close();
        }
    }
}