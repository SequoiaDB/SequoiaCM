package com.sequoiacm.fulltextsearch;

import org.bson.BasicBSONObject;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.fulltext.ScmFulltextModifiler;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-3086:: createIndex参数校验; SCM-3087 :: dropIndex参数校验;
 *               SCM-3088:: alterIndex参数校验;SCM-3094 :: inspect参数校验;
 *               SCM-3095:listWithFulltextMatcher参数校验
 * @author fanyu
 * @Date:2020/11/18
 * @version:1.0
 */

public class Param_FullText3086 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    // SCM-3086:: createIndex参数校验;
    @Test
    private void test1() throws ScmException {
        try {
            ScmFactory.Fulltext.createIndex( null, new ScmFulltextOption(
                    new BasicBSONObject(), ScmFulltextMode.async ) );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.createIndex( ws, null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    // SCM-3087 :: dropIndex参数校验
    @Test
    private void test2() throws ScmException {
        try {
            ScmFactory.Fulltext.dropIndex( null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    // SCM-3088 :: alterIndex参数校验
    @Test
    private void test3() throws ScmException {
        try {
            ScmFactory.Fulltext.alterIndex( null,
                    new ScmFulltextModifiler().newMode( ScmFulltextMode.async )
                            .newFileCondition( new BasicBSONObject() ) );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.alterIndex( ws, null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    // SCM-3094 :: inspect参数校验
    @Test
    private void test4() throws ScmException {
        try {
            ScmFactory.Fulltext.inspectIndex( null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    // SCM-3095:listWithFulltextMatcher参数校验
    @Test
    private void test5() throws ScmException {
        try {
            ScmFactory.Fulltext.listWithFulltextMatcher( null,
                    ScmFileFulltextStatus.CREATED );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.Fulltext.listWithFulltextMatcher( ws, null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        session.close();
    }
}
