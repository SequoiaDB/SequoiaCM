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
import com.sequoiadb.exception.BaseException;

/**
 * @Description:SCM-1848 :: 更新与自身同名/已存在的模型同名
 * @author fanyu
 * @Date:2018年7月4日
 * @version:1.0
 */
public class DefineAttr_Class_Update1848 extends TestScmBase {
    private boolean runSuccess = false;
    private String classname = "Update1848";
    private String desc = "Update1848";
    private ScmClass expClass1 = null;
    private ScmClass expClass2 = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            // create class
            expClass1 = ScmFactory.Class.createInstance( ws,
                    classname + "_" + 1, desc );
            expClass2 = ScmFactory.Class.createInstance( ws,
                    classname + "_" + 2, desc );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        ScmClass class1 = null;
        try {
            class1 = ScmFactory.Class.getInstance( ws, expClass1.getId() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // update classnmae is same as self
        try {
            class1.setName( classname + "_" + 1 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // update classname is same as other
        try {
            class1.setName( classname + "_" + 2 );
        } catch ( ScmException e ) {
            e.getMessage();
            Assert.assertEquals( e.getError(), ScmError.METADATA_CLASS_EXIST,
                    e.getMessage() );
        }

        try {
            ScmClass actclass1 = ScmFactory.Class.getInstance( ws,
                    expClass1.getId() );
            check( actclass1, class1 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Class.deleteInstance( ws, expClass1.getId() );
                ScmFactory.Class.deleteInstance( ws, expClass2.getId() );
            }
            if ( !runSuccess && expClass1 != null && expClass2 != null ) {
                System.out.println( "class = " + expClass1.toString() );
                System.out.println( "class = " + expClass2.toString() );
                ScmFactory.Class.deleteInstance( ws, expClass1.getId() );
                ScmFactory.Class.deleteInstance( ws, expClass2.getId() );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void check( ScmClass actClass, ScmClass expClass ) {
        Assert.assertEquals( actClass.getId(), expClass.getId() );
        Assert.assertEquals( actClass.getName(), expClass.getName() );
        Assert.assertEquals( actClass.getDescription(),
                expClass.getDescription() );
        Assert.assertEquals( actClass.getCreateUser(),
                expClass.getCreateUser() );
        Assert.assertEquals( actClass.getUpdateUser(),
                expClass.getUpdateUser() );
        Assert.assertEquals( actClass.getWorkspace(), expClass.getWorkspace() );
        Assert.assertEquals( actClass.listAttrs(), expClass.listAttrs() );
        Assert.assertEquals( actClass.getCreateTime(),
                expClass.getCreateTime() );
        Assert.assertEquals( actClass.getUpdateTime(),
                expClass.getUpdateTime() );
    }
}
