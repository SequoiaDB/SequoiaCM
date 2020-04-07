package com.sequoiacm.definemeta;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.exception.BaseException;

/**
 * @Description: SCM-1850 :: 列取模型
 * @author fanyu
 * @Date:2018年7月4日
 * @version:1.0
 */
public class DefineAttr_Class_List1850 extends TestScmBase {
    private boolean runSuccess = false;
    private String classname = "List1850";
    private String desc = "List1850";
    private List< ScmClass > classList = new ArrayList< ScmClass >();
    private int classNum = 10;
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
            for ( int i = 0; i < classNum; i++ ) {
                ScmClass class1 = ScmFactory.Class
                        .createInstance( ws, classname + "_" + i, desc );
                classList.add( class1 );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        //suport
        BSONObject cond = null;
        try {
            cond = createCond();
            System.out.println( "cond = " + cond.toString() );
            ScmCursor< ScmClassBasicInfo > cursor = ScmFactory.Class
                    .listInstance( ws, cond );
            int i = 0;
            while ( cursor.hasNext() ) {
                System.out.println( cursor.getNext().toString() );
                i++;
            }
            Assert.assertEquals( i, classNum );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        //does not support
        BSONObject matcher = new BasicBSONObject();
        BSONObject submatcher = new BasicBSONObject();
        submatcher.put( "$regex1", "*List1850" );
        matcher.put( ScmAttributeName.Class.NAME + "1", submatcher.toString() );
        try {
            ScmCursor< ScmClassBasicInfo > cursor = ScmFactory.Class
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
                for ( ScmClass class1 : classList ) {
                    ScmFactory.Class.deleteInstance( ws, class1.getId() );
                }
            }
            if ( !runSuccess && classList.size() != 0 ) {
                System.out.println( "class = " + classList.toString() );
                for ( ScmClass class1 : classList ) {
                    ScmFactory.Class.deleteInstance( ws, class1.getId() );
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

    private BSONObject createCond() throws ScmException {
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.Class.DESCRIPTION ).exists( 1 )
                .and( ScmAttributeName.Class.CREATE_USER )
                .greaterThanEquals( TestScmBase.scmUserName )
                .and( ScmAttributeName.Class.UPDATE_USER )
                .lessThanEquals( TestScmBase.scmUserName )
                .and( ScmAttributeName.Class.CREATE_TIME ).greaterThan( 0 )
                .and( ScmAttributeName.Class.CREATE_TIME )
                .lessThan( Long.MAX_VALUE ).and( ScmAttributeName.Class.NAME )
                .notEquals( Integer.MAX_VALUE )
                .not( ScmQueryBuilder
                        .start( ScmAttributeName.Class.DESCRIPTION )
                        .is( desc + "_1" ).get() )
                .notIn( ScmQueryBuilder
                        .start( ScmAttributeName.Class.DESCRIPTION )
                        .is( desc + "_1" ).get() )
                .or( ScmQueryBuilder.start( ScmAttributeName.Class.DESCRIPTION )
                        .in( desc ).get() ).get();
        return cond;
    }
}
