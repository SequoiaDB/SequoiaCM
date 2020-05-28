package com.sequoiacm.definemeta;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
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
 * @Description: SCM-1922 :: 创建属性正常功能测试
 * @author fanyu
 * @Date:2018年7月5日
 * @version:1.0
 */
public class DefineAttr_Attr_List1933 extends TestScmBase {
    private boolean runSuccess = false;
    private String attrname = "List1933";
    private String desc = "List1933";
    private List< ScmAttribute > attrList = new ArrayList< ScmAttribute >();
    private int attrNum = 10;
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
            for ( int i = 0; i < attrNum; i++ ) {
                ScmAttribute attr = craeteAttr( attrname + "_" + i );
                attrList.add( attr );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // suport
        BSONObject cond = null;
        try {
            cond = createCond();
            System.out.println( "cond = " + cond.toString() );
            ScmCursor< ScmAttribute > cursor = ScmFactory.Attribute
                    .listInstance( ws, cond );
            int i = 0;
            while ( cursor.hasNext() ) {
                System.out.println( cursor.getNext().toString() );
                i++;
            }
            Assert.assertEquals( i, attrNum );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // does not support
        BSONObject matcher = new BasicBSONObject();
        BSONObject submatcher = new BasicBSONObject();
        submatcher.put( "$regex1", "*List1850" );
        matcher.put( ScmAttributeName.Attribute.NAME + "1",
                submatcher.toString() );
        try {
            ScmCursor< ScmAttribute > cursor = ScmFactory.Attribute
                    .listInstance( ws, matcher );
            while ( cursor.hasNext() ) {
                Assert.fail( "cursor = " + cursor.getNext().toString() );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
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
                for ( ScmAttribute attr : attrList ) {
                    System.out.println( "attr = " + attr.toString() );
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

    private ScmAttribute craeteAttr( String attrname ) {
        ScmAttributeConf conf = new ScmAttributeConf();
        ScmAttribute attr = null;
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
        return attr;
    }

    private BSONObject createCond() throws ScmException {
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.Attribute.DESCRIPTION ).exists( 1 )
                .and( ScmAttributeName.Attribute.CREATE_USER )
                .greaterThanEquals( TestScmBase.scmUserName )
                .and( ScmAttributeName.Attribute.UPDATE_USER )
                .lessThanEquals( TestScmBase.scmUserName )
                .and( ScmAttributeName.Attribute.CREATE_TIME ).greaterThan( 0 )
                .and( ScmAttributeName.Attribute.CREATE_TIME )
                .lessThan( Long.MAX_VALUE )
                .and( ScmAttributeName.Attribute.NAME )
                .notEquals( Integer.MAX_VALUE )
                .not( ScmQueryBuilder
                        .start( ScmAttributeName.Attribute.DESCRIPTION )
                        .is( desc + "_1" ).get() )
                .notIn( ScmQueryBuilder
                        .start( ScmAttributeName.Attribute.DESCRIPTION )
                        .is( desc + "_1" ).get() )
                .or( ScmQueryBuilder
                        .start( ScmAttributeName.Attribute.DESCRIPTION )
                        .in( desc ).get() )
                .get();
        return cond;
    }
}
