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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-402: countInstance无效参数校验
 * @Author linsuqiang
 * @Date 2017-05-24
 * @Version 1.00
 */

/*
 * 1、countInstance接口无效参数校验： a.ws不存在； b.condition为null； 2、检查执行结果；
 */

public class ScmFile_param_countInstance402 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testWsIsNull() {
        try {
            ScmFactory.File.countInstance( null,
                    ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject() );
            Assert.fail( "expect fail, but success." );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testConditionIsNull() {
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            ScmFactory.File.countInstance( ws, ScmType.ScopeType.SCOPE_CURRENT,
                    null );
            Assert.fail( "count shouldn't succeed when condition is null!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT,
                    "wrong error code when condition is null" );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

}
