package com.sequoiacm.definemeta;

import java.util.ArrayList;
import java.util.List;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.metadata.ScmAttrRule;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.exception.BaseException;

/**
 * @Description: SCM-1882 :: 字符串STRING的校验规则
 * @author fanyu
 * @Date:2018年7月5日
 * @version:1.0
 */
public class DefineAttr_Attr_CheckRule1882 extends TestScmBase {
    private boolean runSuccess = false;
    private String attrname = "CheckRule1882";
    private String desc = "CheckRule1882";
    private List< ScmAttribute > attrList = new ArrayList< ScmAttribute >();
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
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testDefault() {
        try {
            ScmAttribute attr = craeteAttr( attrname + "_1", null );
            ScmAttribute attr1 = ScmFactory.Attribute.getInstance( ws,
                    attr.getId() );
            Assert.assertEquals( attr1.getCheckRule().toStringFormat(),
                    "{ \"max_length\" : -1 }" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNormal1() {
        try {
            ScmStringRule rule = new ScmStringRule();
            rule.setMaxLength( 18 );
            ScmAttribute attr = craeteAttr( attrname + "_2", rule );
            ScmAttribute attr1 = ScmFactory.Attribute.getInstance( ws,
                    attr.getId() );
            Assert.assertEquals( attr1.getCheckRule().toStringFormat(),
                    "{ \"max_length\" : 18 }" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNormal2() {
        try {
            ScmStringRule rule = new ScmStringRule( 100 );
            ScmAttribute attr = craeteAttr( attrname + "_3", rule );
            ScmAttribute attr1 = ScmFactory.Attribute.getInstance( ws,
                    attr.getId() );
            Assert.assertEquals( attr1.getCheckRule().toStringFormat(),
                    "{ \"max_length\" : 100 }" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNormal3() {
        try {
            ScmStringRule rule = new ScmStringRule(
                    new BasicBSONObject().append( "max_length", 20 ) );
            ScmAttribute attr = craeteAttr( attrname + "_4", rule );
            ScmAttribute attr1 = ScmFactory.Attribute.getInstance( ws,
                    attr.getId() );
            Assert.assertEquals( attr1.getCheckRule().toStringFormat(),
                    "{ \"max_length\" : 20 }" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testError() {
        try {
            ScmIntegerRule rule = new ScmIntegerRule();
            rule.setMaximum( 100 );
            rule.setMinimum( 0 );
            craeteAttr( attrname + "_5", rule );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmAttribute attr : attrList ) {
                    ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
                }
            }
            if ( !runSuccess && attrList.size() != 0 ) {
                System.out.println( "attr = " + attrList.toString() );
                for ( ScmAttribute attr : attrList ) {
                    ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
                }
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmAttribute craeteAttr( String name, ScmAttrRule rule )
            throws ScmException {
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( name );
        conf.setDescription( desc );
        conf.setDisplayName( attrname + "_display" );
        conf.setRequired( true );
        conf.setType( AttributeType.STRING );
        if ( rule != null ) {
            conf.setCheckRule( rule );
        }
        ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
        attrList.add( attr );
        return attr;
    }
}
