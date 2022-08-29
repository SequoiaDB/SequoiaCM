package com.sequoiacm.definemeta;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.exception.BaseException;

/**
 * @Description: SCM-1928 :: 更新属性正常功能测试
 * @author fanyu
 * @Date:2018年7月5日
 * @version:1.0
 */
public class DefineAttr_Attr_Update1928 extends TestScmBase {
    private boolean runSuccess = false;
    private String attrname = "Update1928";
    private String desc = "Update1928";
    private ScmAttribute attr;
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

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        craeteAttr();
        ScmAttribute updateAttr = ScmFactory.Attribute.getInstance( ws,
                attr.getId() );
        updateAttr.setDescription( desc + "_1" );
        updateAttr.setDisplayName( attrname + "_display_2" );
        updateAttr.setRequired( false );

        ScmIntegerRule rule = new ScmIntegerRule();
        rule.setMinimum( 10 );
        rule.setMaximum( 100 );

        updateAttr.setCheckRule( rule );

        ScmAttribute actattr = ScmFactory.Attribute.getInstance( ws,
                attr.getId() );
        check( actattr, attr, rule );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
            }
            if ( !runSuccess && attr != null ) {
                System.out.println( "class = " + attr.toString() );
                ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void craeteAttr() {
        ScmAttributeConf conf = new ScmAttributeConf();
        try {
            conf.setName( attrname );
            conf.setDescription( desc );
            conf.setDisplayName( attrname + "_display" );
            conf.setRequired( true );
            conf.setType( AttributeType.INTEGER );

            ScmIntegerRule rule = new ScmIntegerRule();
            rule.setMinimum( 0 );
            rule.setMaximum( 10 );
            conf.setCheckRule( rule );

            attr = ScmFactory.Attribute.createInstance( ws, conf );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void check( ScmAttribute actAttr, ScmAttribute expAttr,
            ScmIntegerRule rule ) {
        Assert.assertEquals( actAttr.getCreateUser(), expAttr.getCreateUser() );
        Assert.assertEquals( actAttr.getDescription(), desc + "_1" );
        Assert.assertEquals( actAttr.getDisplayName(),
                attrname + "_display_2" );
        Assert.assertEquals( actAttr.getName(), expAttr.getName() );
        Assert.assertEquals( actAttr.getUpdateUser(), expAttr.getUpdateUser() );
        Assert.assertEquals( actAttr.getCheckRule().toStringFormat(),
                rule.toStringFormat() );
        Assert.assertEquals( actAttr.getCreateTime(), expAttr.getCreateTime() );
        Assert.assertEquals( actAttr.getId(), expAttr.getId() );
        Assert.assertEquals( actAttr.getType(), expAttr.getType() );
        Assert.assertEquals(
                actAttr.getUpdateTime().compareTo( expAttr.getUpdateTime() ),
                1 );
        Assert.assertEquals( actAttr.getWorkspace().getName(),
                expAttr.getWorkspace().getName() );
        Assert.assertEquals( actAttr.isRequired(), false );
        Assert.assertEquals( actAttr.isExist(), true );
    }
}
