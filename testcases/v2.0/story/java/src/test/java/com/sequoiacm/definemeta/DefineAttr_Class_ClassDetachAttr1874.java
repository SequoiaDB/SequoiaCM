package com.sequoiacm.definemeta;

import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
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
 * @Description: SCM-1874 :: 解除没有关连的属性
 * @author fanyu
 * @Date:2018年7月4日
 * @version:1.0
 */
public class DefineAttr_Class_ClassDetachAttr1874 extends TestScmBase {
    private boolean runSuccess = false;
    private String name = "ClassDetachAttr1874";
    private String desc = "ClassDetachAttr1874";
    private ScmClass class1 = null;
    private List< ScmAttribute > attrList = new ArrayList< ScmAttribute >();
    private int num = 2;

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
            class1 = ScmFactory.Class.createInstance( ws, name, desc );
            for ( int i = 0; i < num; i++ ) {
                ScmAttribute attr = craeteAttr( name + "_" + i );
                class1.attachAttr( attr.getId() );
                attrList.add( attr );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        class1.detachAttr( attrList.get( 1 ).getId() );
        try {
            class1.detachAttr( attrList.get( 1 ).getId() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.METADATA_ATTR_NOT_IN_CLASS ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        ScmClass class2 = ScmFactory.Class.getInstance( ws, class1.getId() );
        Assert.assertEquals( class2.listAttrs().size(), 1,
                class2.listAttrs().toString() );
        Assert.assertEquals( class2.listAttrs().get( 0 ).getId(),
                attrList.get( 0 ).getId(),
                class2.listAttrs().get( 0 ).toString() );
        Assert.assertEquals( class2.listAttrs().get( 0 ).getName(),
                attrList.get( 0 ).getName(),
                class2.listAttrs().get( 0 ).toString() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Class.deleteInstance( ws, class1.getId() );
                for ( int i = 0; i < num; i++ ) {
                    ScmFactory.Attribute.deleteInstance( ws,
                            attrList.get( i ).getId() );
                }
            }
            if ( !runSuccess && attrList.size() != 0 ) {
                System.out.println( "class = " + class1.toString() );
                ScmFactory.Class.deleteInstance( ws, class1.getId() );
                for ( int i = 0; i < num; i++ ) {
                    ScmFactory.Attribute.deleteInstance( ws,
                            attrList.get( i ).getId() );
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

    private ScmAttribute craeteAttr( String name ) throws ScmException {
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( name );
        conf.setDescription( desc );
        conf.setDisplayName( name + "_display" );
        conf.setRequired( true );
        conf.setType( AttributeType.INTEGER );

        ScmIntegerRule rule = new ScmIntegerRule();
        rule.setMinimum( 0 );
        rule.setMaximum( 10 );
        conf.setCheckRule( rule );
        ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
        return attr;
    }
}
