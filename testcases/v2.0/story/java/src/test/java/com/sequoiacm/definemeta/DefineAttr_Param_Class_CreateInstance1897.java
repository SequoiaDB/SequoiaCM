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
 * @Description: SCM-1897 :: ScmFactory. Class.createInstance() 参数校验 
 * @author fanyu
 * @Date:2018年7月7日
 * @version:1.0
 */
public class DefineAttr_Param_Class_CreateInstance1897 extends TestScmBase {
    private String classname = "Param1897";
    private String desc = "Param1897 It is a test";
    private ScmClass expClass = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testChinese() throws Exception {
        String classname = "模型1";
        String desc = "模型描述";
        //create
        expClass = ScmFactory.Class.createInstance( ws, classname, desc );
        //get
        ScmClass actClass = ScmFactory.Class
                .getInstance( ws, expClass.getId() );

        Assert.assertEquals( actClass.getId(), expClass.getId() );
        Assert.assertEquals( actClass.getName(), classname );
        Assert.assertEquals( actClass.getDescription(), desc );
        Assert.assertEquals( actClass.getCreateUser(),
                TestScmBase.scmUserName );
        Assert.assertEquals( actClass.getUpdateUser(),
                TestScmBase.scmUserName );
        Assert.assertEquals( actClass.getWorkspace().getName(), wsp.getName() );
        Assert.assertEquals( actClass.listAttrs().size(), 0 );
        Assert.assertNotNull( actClass.getCreateTime() );
        Assert.assertNotNull( actClass.getUpdateTime() );
        ScmFactory.Class.deleteInstance( ws, actClass.getId() );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNamewithDot() throws ScmException {
        String classname = "1897_1.90";
        ScmClass expClass = null;
        // create
        try {
            expClass = ScmFactory.Class.createInstance( ws, classname, desc );
            ScmClass actClass = ScmFactory.Class
                    .getInstance( ws, expClass.getId() );
            Assert.assertEquals( actClass.getName(), classname );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expClass != null ) {
                ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNameWithBlack() throws ScmException {
        String classname = "1897_2 90";
        ScmClass expClass = null;
        // create
        try {
            expClass = ScmFactory.Class.createInstance( ws, classname, desc );
            ScmClass actClass = ScmFactory.Class
                    .getInstance( ws, expClass.getId() );
            Assert.assertEquals( actClass.getName(), classname );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expClass != null ) {
                ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNameWithDollar() throws ScmException {
        String classname = "1897_2$90";
        ScmClass expClass = null;
        // create
        try {
            expClass = ScmFactory.Class.createInstance( ws, classname, desc );
            ScmClass actClass = ScmFactory.Class
                    .getInstance( ws, expClass.getId() );
            Assert.assertEquals( actClass.getName(), classname );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expClass != null ) {
                ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNameWithDiagonal() throws ScmException {
        String classname = "1897_3/90";
        ScmClass expClass = null;
        // create
        try {
            expClass = ScmFactory.Class.createInstance( ws, classname, desc );
            ScmClass actClass = ScmFactory.Class
                    .getInstance( ws, expClass.getId() );
            Assert.assertEquals( actClass.getName(), classname );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expClass != null ) {
                ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNameIsDollar() throws ScmException {
        String classname = "$";
        ScmClass expClass = null;
        // create
        try {
            expClass = ScmFactory.Class.createInstance( ws, classname, desc );
            ScmClass actClass = ScmFactory.Class
                    .getInstance( ws, expClass.getId() );
            Assert.assertEquals( actClass.getName(), classname );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expClass != null ) {
                ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNameIsNull() {
        // create
        try {
            expClass = ScmFactory.Class.createInstance( ws, null, desc );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNameIsBlackStr() {
        // create
        try {
            expClass = ScmFactory.Class.createInstance( ws, "", desc );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testDescIsNull() {
        // create
        try {
            expClass = ScmFactory.Class.createInstance( ws, classname, null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testWsIsNull() {
        // create
        try {
            expClass = ScmFactory.Class.createInstance( null, classname, desc );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }
}
