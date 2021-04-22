package com.sequoiacm.fulltextsearch.serial;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description SCM-3016:工作区不存在，工作区创建索引
 * @author wuyan
 * @Date 2020.09.17
 * @version 1.00
 */

public class WsIndex3016 extends TestScmBase {
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String wsName = "ws3016";

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        int siteNum = ScmInfo.getSiteNum();
        ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    private void test() throws Exception {
        ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
        try {
            BSONObject matcher = new BasicBSONObject();
            matcher.put( "author", "test" );
            ScmFactory.Fulltext.createIndex( ws,
                    new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );
            Assert.fail( "createindex must bu fail!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                Assert.fail( e.getMessage() + ";e=" + e.getErrorCode() );
            }
        }
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
