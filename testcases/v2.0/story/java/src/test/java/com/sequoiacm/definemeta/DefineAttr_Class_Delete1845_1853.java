package com.sequoiacm.definemeta;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.exception.BaseException;

/**
 * @Description: SCM-1845 :: 删除模型 SCM-1853 :: 获取不存在的模型
 * @author fanyu
 * @Date:2018年7月4日
 * @version:1.0
 */
public class DefineAttr_Class_Delete1845_1853 extends TestScmBase {
    private boolean runSuccess = false;
    private String classname = "Delete1845";
    private String desc = "Delete1845";
    private ScmClass expClass = null;
    private ScmAttribute expAttr = null;
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

            // create Class
            expClass = ScmFactory.Class.createInstance( ws, classname, desc );

            // create Attr
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( classname );
            conf.setDisplayName( classname );
            conf.setDescription( desc );
            conf.setRequired( false );
            conf.setType( AttributeType.BOOLEAN );
            conf.setCheckRule( null );
            expAttr = ScmFactory.Attribute.createInstance( ws, conf );

            // Attach
            expClass.attachAttr( expAttr.getId() );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        try {
            // delete class that has attached attr
            ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            // delete again
            try {
                ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.METADATA_CLASS_NOT_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // check delete
        checkDel( expClass );

        try {
            // create again
            expClass = ScmFactory.Class.createInstance( ws, classname, desc );

            // delete
            ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            // delete again
            try {
                ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.METADATA_CLASS_NOT_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // check delete
        checkDel( expClass );

        try {
            // create again
            expClass = ScmFactory.Class.createInstance( ws, classname, desc );

            ScmClass actClass = ScmFactory.Class.getInstance( ws,
                    expClass.getId() );
            check( actClass, expClass );
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
                ScmFactory.Class.deleteInstance( ws, expClass.getId() );
                ScmFactory.Attribute.deleteInstance( ws, expAttr.getId() );
            }
            if ( !runSuccess && expClass != null ) {
                System.out.println( "class = " + expClass.toString() );
                System.out.println( "expAttr = " + expAttr.toString() );
                ScmFactory.Class.deleteInstance( ws, expClass.getId() );
                ScmFactory.Class.deleteInstance( ws, expAttr.getId() );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkDel( ScmClass expClass ) {
        try {
            ScmFactory.Class.getInstance( ws, expClass.getId() );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(),
                    ScmError.METADATA_CLASS_NOT_EXIST, e.getMessage() );
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
