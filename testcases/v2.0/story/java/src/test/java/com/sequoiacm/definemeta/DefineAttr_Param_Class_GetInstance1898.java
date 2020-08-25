package com.sequoiacm.definemeta;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmClass;
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
 * @Description: SCM-1898:ScmFactory.Class.getInstance()/getInstanceByName()参数校验
 * @author fanyu
 * @Date:2018年7月7日
 * @version:1.0
 */
public class DefineAttr_Param_Class_GetInstance1898 extends TestScmBase {
    private String classname = "Param1898 中文.!@#$*()_+::<>\"test";
    private String desc = "Param1898 It is a test";
    private ScmClass expClass = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        expClass = ScmFactory.Class.createInstance( ws, classname, desc );
    }

    @Test(enabled = false)// TODO: SEQUOIACM-548
    private void test() throws ScmException {
       ScmClass actClass = ScmFactory.Class.getInstanceByName( ws, classname );
       Assert.assertEquals( actClass.getName(),classname );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testWsIsNull() throws ScmException {
        // get
        try {
            ScmFactory.Class.getInstance( null, expClass.getId() );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        // get
        try {
            ScmFactory.Class.getInstanceByName( null, expClass.getName() );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testIdOrNameIsNull() throws ScmException {
        // get
        try {
            ScmFactory.Class.getInstance( ws, null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        // get
        try {
            ScmFactory.Class.getInstanceByName( ws, null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ScmFactory.Class.deleteInstance( ws, expClass.getId() );
        if ( session != null ) {
            session.close();
        }
    }
}
