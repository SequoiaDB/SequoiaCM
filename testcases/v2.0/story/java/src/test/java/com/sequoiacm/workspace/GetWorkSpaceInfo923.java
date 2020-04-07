/**
 *
 */
package com.sequoiacm.workspace;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;

/**
 * @Description:1、listWorkspace获取ws列表； 2、遍历游标，获取每个workspace的所有属性，并校验属性正确性；
 * @author fanyu
 * @Date:2017年9月20日
 * @version:1.0
 */
public class GetWorkSpaceInfo923 extends TestScmBase {
    private static SiteWrapper site = null;
    private ScmSession session = null;
    private Sequoiadb sdb = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        ScmCursor< ScmWorkspaceInfo > cursor = null;
        DBCursor dbcursor = null;
        try {
            cursor = ScmFactory.Workspace.listWorkspace( session );
            List< ScmWorkspaceInfo > infoList = new ArrayList<
                    ScmWorkspaceInfo >();
            while ( cursor.hasNext() ) {
                ScmWorkspaceInfo info = cursor.getNext();
                infoList.add( info );
            }

            // check results
            dbcursor = listWorkSpaceFromDB();
            while ( dbcursor.hasNext() ) {
                BSONObject obj = dbcursor.getNext();
                int id = ( int ) obj.get( "id" );
                for ( ScmWorkspaceInfo info : infoList ) {
                    //System.out.println("info = " + info.toString());
                    if ( id == info.getId() ) {
                        Assert.assertEquals( info.getName(),
                                ( String ) obj.get( "name" ) );
                        Assert.assertEquals( info.getMetaLocation(),
                                obj.get( "meta_location" ) );
                        Assert.assertEquals( info.getDataLocation(),
                                obj.get( "data_location" ) );
                        /*deprecated interface
						Assert.assertEquals(info.getDataOption(), obj.get
						("data_options"));
						Assert.assertEquals(info.getDataShardingType(), obj
						.get("data_sharding_type"));
						Assert.assertEquals(info.getMetaShardingType(),
						(String) obj.get("meta_sharding_type"));
						*/
                    }
                }
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
            if ( dbcursor != null ) {
                dbcursor.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
        if ( sdb != null ) {
            sdb.close();
        }
    }

    private DBCursor listWorkSpaceFromDB() {
        DBCursor dbcursor = null;
        sdb = new Sequoiadb( TestScmBase.mainSdbUrl, TestScmBase.sdbUserName,
                TestScmBase.sdbPassword );
        DBCollection wsCL = sdb.getCollectionSpace( TestSdbTools.SCM_CS )
                .getCollection( TestSdbTools.SCM_CL_WORKSPACE );
        dbcursor = wsCL.query();
        return dbcursor;
    }
}
