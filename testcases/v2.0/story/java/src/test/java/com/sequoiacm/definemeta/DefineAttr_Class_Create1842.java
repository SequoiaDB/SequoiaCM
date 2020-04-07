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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.exception.BaseException;

/**
 * @Description: SCM-1842 :: 创建模型
 * @author fanyu
 * @Date:2018年7月4日
 * @version:1.0
 */
public class DefineAttr_Class_Create1842 extends TestScmBase {
    private boolean runSuccess = false;
    private String classname = "Create1842";
    private String desc = "Create1842";
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
    private void test() throws Exception {
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
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            }
            if ( !runSuccess && expClass != null ) {
                System.out.println( "class = " + expClass.toString() );
                ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
