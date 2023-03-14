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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-1900:ScmFactory.Class.deleteInstance()/deleteInstanceByName()参数校验
 * @author fanyu
 * @Date:2018年7月7日
 * @version:1.0
 */
public class DefineAttr_Param_Class_DeleteInstance1900 extends TestScmBase {
    private String classname = "Param1900";
    private String desc = "Param1900 It is a test";
    private ScmClass expClass = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        expClass = ScmFactory.Class.createInstance( ws, classname, desc );
    }

    @Test
    private void test() throws ScmException {
        String classname = "Param1900 中文.!@#$*()/%\\_+::<>\"test";
        ScmClass scmClass = ScmFactory.Class.createInstance( ws,classname, desc);
        ScmFactory.Class.deleteInstanceByName( ws, classname );
        try {
            ScmFactory.Class.getInstance( ws,scmClass.getId() );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.METADATA_CLASS_NOT_EXIST ) {
                throw e;
            }
        }
    }

    @Test
    private void testWsIsNull() throws ScmException {
        // delete
        try {
            ScmFactory.Class.deleteInstance( null, expClass.getId() );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.Class.deleteInstanceByName( null, expClass.getName() );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test
    private void testIdOrNameIsNull() throws ScmException {
        // delete
        try {
            ScmFactory.Class.deleteInstance( ws, null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.Class.deleteInstanceByName( ws, null );
            Assert.fail( "exp fail but act success" );
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
