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
import com.sequoiadb.exception.BaseException;

/**
 * @Description: SCM-1843 : 重复创建模型
 * @author fanyu
 * @Date:2018年7月4日
 * @version:1.0
 */
public class DefineAttr_Class_RepeatCreate1843 extends TestScmBase {
    private boolean runSuccess = false;
    private String classname = "RepeatCreate1843";
    private String desc = "RepeatCreate1843";
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
    private void test() {
        // create
        try {
            expClass = ScmFactory.Class.createInstance( ws, classname, desc );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // repeat create
        try {
            ScmFactory.Class.createInstance( ws, classname, desc );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.METADATA_CLASS_EXIST,
                    e.getMessage() );
        }

        // get
        ScmClass actClass = null;
        try {
            actClass = ScmFactory.Class.getInstance( ws, expClass.getId() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // check
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
