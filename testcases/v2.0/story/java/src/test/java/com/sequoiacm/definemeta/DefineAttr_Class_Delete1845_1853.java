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
    private void setUp() throws ScmException {
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
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        // delete class that has attached attr
        ScmFactory.Class.deleteInstance( ws, expClass.getId() );
        // delete again
        try {
            ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.METADATA_CLASS_NOT_EXIST ) {
                throw e;
            }
        }
        // check delete
        checkDel( expClass );

        // create again
        expClass = ScmFactory.Class.createInstance( ws, classname, desc );
        // delete
        ScmFactory.Class.deleteInstance( ws, expClass.getId() );
        // delete again
        try {
            ScmFactory.Class.deleteInstance( ws, expClass.getId() );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.METADATA_CLASS_NOT_EXIST ) {
                throw e;
            }
        }
        // check delete
        checkDel( expClass );

        // create again
        expClass = ScmFactory.Class.createInstance( ws, classname, desc );
        ScmClass actClass = ScmFactory.Class.getInstance( ws,
                expClass.getId() );
        check( actClass, expClass );

        // delete by name
        ScmFactory.Class.deleteInstanceByName( ws, expClass.getName() );
        try {
            ScmFactory.Class.getInstanceByName( ws, expClass.getName() );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(),
                    ScmError.METADATA_CLASS_NOT_EXIST, e.getMessage() );
        }

        // delete by name again
        try {
            ScmFactory.Class.deleteInstanceByName( ws, expClass.getName() );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.METADATA_CLASS_NOT_EXIST ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Attribute.deleteInstance( ws, expAttr.getId() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkDel( ScmClass expClass ) {
        try {
            ScmFactory.Class.getInstance( ws, expClass.getId() );
            Assert.fail( "exp fail but act success!!!" );
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
