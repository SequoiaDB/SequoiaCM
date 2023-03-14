package com.sequoiacm.fulltextsearch.serial;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description SCM-3025:空工作区且工作区索引状态为created,删除索引 SCM-3030:空工作区，全文检索
 * @author wuyan
 * @Date 2020.09.17
 * @version 1.00
 */

public class WsIndex3025_3030 extends TestScmBase {
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String wsName = "ws3025";

    @BeforeClass
    private void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        int siteNum = ScmInfo.getSiteNum();
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    private void test() throws Exception {
        // 创建全文索引
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "author", "test3025" );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 全文检索
        ScmCursor< ScmFulltextSearchResult > result = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .notMatch( "condition" ).search();
        Assert.assertFalse( result.hasNext() );

        // 删除索引
        ScmFactory.Fulltext.dropIndex( ws );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.NONE );
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
