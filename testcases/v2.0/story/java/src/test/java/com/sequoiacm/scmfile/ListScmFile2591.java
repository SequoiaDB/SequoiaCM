package com.sequoiacm.scmfile;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2591:分页列取全部版本文件列表和指定无效limit、skip
 * @author fanyu
 * @Date:2019年8月28日
 * @version:1.0
 */
public class ListScmFile2591 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test
    private void testInvalidScope() throws ScmException {
        try {
            ScmFactory.File.listInstance( ws, ScmType.ScopeType.SCOPE_ALL,
                    new BasicBSONObject(), new BasicBSONObject(), 0, 1 );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNSUPPORTED ) {
                throw e;
            }
        }
    }

    @Test
    private void testInvalidLimit() throws ScmException {
        try {
            ScmFactory.File.listInstance( ws, ScmType.ScopeType.SCOPE_CURRENT,
                    new BasicBSONObject(), new BasicBSONObject(), 0, -2 );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test
    private void testInvalidSkip() throws ScmException {
        try {
            ScmFactory.File.listInstance( ws, ScmType.ScopeType.SCOPE_HISTORY,
                    new BasicBSONObject(), new BasicBSONObject(), -1, 1 );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test
    private void testWSisNull() throws ScmException {
        try {
            ScmFactory.File.listInstance( null, ScmType.ScopeType.SCOPE_HISTORY,
                    new BasicBSONObject(), new BasicBSONObject(), 0, 1 );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test
    private void testCondIsNull() throws ScmException {
        try {
            ScmFactory.File.listInstance( null, ScmType.ScopeType.SCOPE_HISTORY,
                    null, new BasicBSONObject(), 0, 1 );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( session != null ) {
            session.close();
        }
    }
}
