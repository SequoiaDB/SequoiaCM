package com.sequoiacm.fulltextsearch;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextHighlightOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description: SCM-3089 ::getIndexInfo参数校验; SCM-3090 :: simpleSearcher参数校验
 *               SCM-3091 :: customSearcher参数校验 SCM-3092 :: rebuildFileIndex参数校验
 *               SCM-3093 :: getFileIndexInfo参数校验 SCM-3096 ::
 *               countWithFulltextMatcher参数校验
 * @author fanyu
 * @Date:2020/11/12
 * @version:1.0
 */
public class Param_FullText3089 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName1 = "ws3089A";
    private String wsName2 = "ws3089B";
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.createWS( session, wsName1, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName1 );
        ScmWorkspaceUtil.createWS( session, wsName2, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName2 );
        ws1 = ScmFactory.Workspace.getWorkspace( wsName1, session );
        ws2 = ScmFactory.Workspace.getWorkspace( wsName2, session );
        checkIndexInfo();
        // 删除工作区
        ScmFactory.Workspace.deleteWorkspace( session, wsName1 );
        ScmFactory.Fulltext.createIndex( ws2, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.async ) );
    }

    // SCM-3089 ::getIndexInfo参数校验;
    @Test
    private void test1() throws Exception {
        try {
            ScmFactory.Fulltext.getIndexInfo( null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.getIndexInfo( ws1 );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST
                    && e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                throw e;
            }
        }
    }

    // SCM-3090 :: simpleSearcher参数校验
    @Test
    private void test2() throws Exception {
        try {
            ScmFactory.Fulltext.simpleSeracher( null )
                    .fileCondition( new BasicBSONObject() ).match( "test" )
                    .search();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.simpleSeracher( ws1 )
                    .fileCondition( new BasicBSONObject() ).match( "test" )
                    .search();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST
                    && e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.simpleSeracher( ws2 )
                    .fileCondition( new BasicBSONObject() ).search();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    // SCM-3091 :: customSearcher参数校验
    @Test
    private void test3() throws Exception {
        try {
            ScmFactory.Fulltext.customSeracher( null )
                    .fileCondition( new BasicBSONObject() )
                    .fulltextCondition( new BasicBSONObject() ).search();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.customSeracher( ws1 )
                    .fileCondition( new BasicBSONObject() )
                    .fulltextCondition( new BasicBSONObject() ).search();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST
                    && e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.customSeracher( ws2 )
                    .fileCondition( new BasicBSONObject() ).search();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.customSeracher( ws2 )
                    .fileCondition( new BasicBSONObject() )
                    .scope( ScmType.ScopeType.SCOPE_ALL ).fulltextCondition(
                            new BasicBSONObject( "min_score", null ) )
                    .search();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SYSTEM_ERROR ) {
                throw e;
            }
        }
    }

    // SCM-3092 :: rebuildFileIndex参数校验
    @Test
    private void test4() throws Exception {
        try {
            ScmFactory.Fulltext.rebuildFileIndex( null,
                    new ScmId( "123", false ) );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    // SCM-3093 :: getFileIndexInfo参数校验
    @Test
    private void test5() throws Exception {
        try {
            ScmFactory.Fulltext.getFileIndexInfo( null,
                    new ScmId( "5fb20ea040000200eef6579b" ) );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.getFileIndexInfo( ws1,
                    new ScmId( "5fb20ea040000200eef6579b" ) );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST
                    && e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.getFileIndexInfo( ws2, null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.getFileIndexInfo( ws2,
                    new ScmId( "5fb20ea040000200eef6579b" ) );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                throw e;
            }
        }
    }

    // SCM-3096 :: countWithFulltextMatcher参数校验
    @Test
    private void test6() throws Exception {
        long count = ScmFactory.Fulltext.countWithFulltextMatcher( ws2,
                ScmFileFulltextStatus.NONE );
        Assert.assertEquals( count, 0 );

        try {
            ScmFactory.Fulltext.countWithFulltextMatcher( null,
                    ScmFileFulltextStatus.NONE );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.countWithFulltextMatcher( ws1,
                    ScmFileFulltextStatus.NONE );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST
                    && e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.countWithFulltextMatcher( ws2, null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    // SCM-3097:ScmFulltextHighlightOption参数校验
    @Test
    private void test7() throws Exception {
        ScmFulltextHighlightOption option = new ScmFulltextHighlightOption();
        try {
            option.setTag( null, "text" );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            option.setTag( "text", null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName2, session );
        if ( session != null ) {
            session.close();
        }
    }

    private void checkIndexInfo() throws ScmException {
        ScmFulltexInfo info = ScmFactory.Fulltext.getIndexInfo( ws2 );
        Assert.assertNull( info.getFileMatcher() );
        Assert.assertNull( info.getFulltextLocation() );
        Assert.assertNull( info.getJobInfo() );
        Assert.assertNull( info.getMode() );
        Assert.assertEquals( info.getStatus(), ScmFulltextStatus.NONE );
    }
}