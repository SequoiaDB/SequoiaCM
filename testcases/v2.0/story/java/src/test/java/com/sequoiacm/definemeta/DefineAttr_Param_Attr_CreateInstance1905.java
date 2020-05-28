package com.sequoiacm.definemeta;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
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
 * @Description: SCM-1905 :: Attribute.createInstance参数校验
 * @author fanyu
 * @Date:2018年7月7日
 * @version:1.0
 */
public class DefineAttr_Param_Attr_CreateInstance1905 extends TestScmBase {
    private String desc = "Param1905  It is a test";
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

        String name = "模型1 1905";
        String desc = "模型描述  it is a test!";

        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( name );
        conf.setDisplayName( desc );
        conf.setDescription( desc );
        conf.setType( AttributeType.BOOLEAN );
        // create
        ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
        // get
        ScmAttribute actAttr = ScmFactory.Attribute.getInstance( ws,
                attr.getId() );

        Assert.assertEquals( actAttr.getCreateUser(), attr.getCreateUser() );
        Assert.assertEquals( actAttr.getDescription(), desc );
        Assert.assertEquals( actAttr.getDisplayName(), desc );
        Assert.assertEquals( actAttr.getName(), name );
        Assert.assertEquals( actAttr.getUpdateUser(), attr.getUpdateUser() );
        Assert.assertEquals( actAttr.getCheckRule(), attr.getCheckRule() );
        Assert.assertEquals( actAttr.getCreateTime(), attr.getCreateTime() );
        Assert.assertEquals( actAttr.getId(), attr.getId() );
        Assert.assertEquals( actAttr.getType(), attr.getType() );
        Assert.assertEquals( actAttr.getUpdateTime(), attr.getUpdateTime() );
        Assert.assertEquals( actAttr.getWorkspace().getName(),
                attr.getWorkspace().getName() );
        Assert.assertEquals( actAttr.isRequired(), false );
        Assert.assertEquals( actAttr.isExist(), true );
        ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNameWithDollar() throws ScmException {
        // create
        String name = "19$05$$";
        ScmAttribute attr = null;
        try {
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( name );
            conf.setDisplayName( "" );
            conf.setDescription( "" );
            conf.setType( AttributeType.BOOLEAN );
            attr = ScmFactory.Attribute.createInstance( ws, conf );
            ScmAttribute actAttr = ScmFactory.Attribute.getInstance( ws,
                    attr.getId() );
            Assert.assertEquals( actAttr.getName(), name );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( attr != null ) {
                ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNamewithDot() throws ScmException {
        String name = "1905_1..90";
        ScmAttribute attr = null;
        // create
        try {
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( name );
            conf.setDisplayName( name );
            conf.setDescription( name );
            conf.setType( AttributeType.BOOLEAN );

            attr = ScmFactory.Attribute.createInstance( ws, conf );
            ScmAttribute actAttr = ScmFactory.Attribute.getInstance( ws,
                    attr.getId() );
            Assert.assertEquals( actAttr.getName(), name );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.ATTRIBUTE_FORMAT_ERROR ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( attr != null ) {
                ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNameWithDot() {
        // create
        try {
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( "." );
            conf.setDisplayName( "" );
            conf.setDescription( "" );
            conf.setType( AttributeType.BOOLEAN );
            ScmFactory.Attribute.createInstance( ws, conf );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.ATTRIBUTE_FORMAT_ERROR ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNameIsDollar() {
        // create
        try {
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( "$" );
            conf.setDisplayName( "" );
            conf.setDescription( "" );
            conf.setType( AttributeType.BOOLEAN );
            ScmFactory.Attribute.createInstance( ws, conf );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.ATTRIBUTE_FORMAT_ERROR ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNameWithBlack() throws ScmException {
        String name = "1905_2 90";
        ScmAttribute attr = null;
        // create
        try {
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( name );
            conf.setDisplayName( name );
            conf.setDescription( name );
            conf.setType( AttributeType.BOOLEAN );
            attr = ScmFactory.Attribute.createInstance( ws, conf );
            ScmAttribute actAttr = ScmFactory.Attribute.getInstance( ws,
                    attr.getId() );
            Assert.assertEquals( actAttr.getName(), name );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( attr != null ) {
                ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNameWithDiagonal() throws ScmException {
        String name = "1905_3/90";
        ScmAttribute attr = null;
        // create
        try {
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( name );
            conf.setDisplayName( name );
            conf.setDescription( name );
            conf.setType( AttributeType.BOOLEAN );
            attr = ScmFactory.Attribute.createInstance( ws, conf );
            ScmAttribute actAttr = ScmFactory.Attribute.getInstance( ws,
                    attr.getId() );
            Assert.assertEquals( actAttr.getName(), name );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( attr != null ) {
                ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNameIsNull() {
        // create
        try {
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( null );
            conf.setDisplayName( desc );
            conf.setDescription( desc );
            conf.setType( AttributeType.BOOLEAN );
            ScmFactory.Attribute.createInstance( ws, conf );
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
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( "" );
            conf.setDisplayName( "" );
            conf.setDescription( "" );
            conf.setType( AttributeType.BOOLEAN );
            ScmFactory.Attribute.createInstance( ws, conf );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.ATTRIBUTE_FORMAT_ERROR ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testDescIsNull() {
        String name = "1905_3/90";
        // create
        try {
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( name );
            conf.setDisplayName( "" );
            conf.setDescription( null );
            conf.setType( AttributeType.BOOLEAN );
            ScmFactory.Attribute.createInstance( ws, conf );
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
        String name = "1905_5";
        // create
        try {
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( name );
            conf.setDisplayName( desc );
            conf.setDescription( desc );
            conf.setType( AttributeType.BOOLEAN );
            // create
            ScmFactory.Attribute.createInstance( null, conf );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testConfIsNull() {
        // create
        try {
            ScmFactory.Attribute.createInstance( ws, null );
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
