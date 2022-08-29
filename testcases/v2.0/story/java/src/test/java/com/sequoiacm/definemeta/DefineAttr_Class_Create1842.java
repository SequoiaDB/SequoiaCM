package com.sequoiacm.definemeta;

import com.sequoiacm.testcommon.listener.GroupTags;
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

/**
 * @Description: SCM-1842:创建/获取模型
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
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        // create
        expClass = ScmFactory.Class.createInstance( ws, classname, desc );
        // get
        ScmClass actClass = ScmFactory.Class.getInstance( ws,
                expClass.getId() );
        checkClass( actClass, expClass );

        actClass = ScmFactory.Class.getInstanceByName( ws, classname );
        checkClass( actClass, expClass );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkClass( ScmClass actClass, ScmClass expClass ) {
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
    }
}
