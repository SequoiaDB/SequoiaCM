/**
 *
 */
package com.sequoiacm.testresource;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;

/**
 * public testCase, execute when all of the testCases succeed
 */
public class CheckResource extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( CheckResource.class );

    @BeforeClass(alwaysRun = true)
    private void setUp() {
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testRemainSession() throws Exception {
        List< String > sessionList = this.isRemainScmSession();
        if ( sessionList.size() > 0 ) {
            throw new Exception( "remain session \n" + sessionList );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testRemainScmFile() throws Exception {
        List< String > fileList = this.isRemainScmFile();
        if ( fileList.size() > 0 ) {
            throw new Exception( "remain scmfile \n" + fileList );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }

    private List< String > isRemainScmSession() {
        Sequoiadb db = null;
        DBCursor cursor = null;
        List< String > sessionList = new ArrayList<>();
        try {
            db = TestSdbTools.getSdb( TestScmBase.mainSdbUrl );
            CollectionSpace cs = db.getCollectionSpace( TestSdbTools.SCM_CS );
            DBCollection cl = cs.getCollection( TestSdbTools.SCM_CL_SESSION );

            long cnt = cl.getCount();
            if ( 0 != cnt ) {
                cursor = cl.query();
                while ( cursor.hasNext() ) {
                    BSONObject ssInfo = cursor.getNext();
                    String ssId = ssInfo.get( "_id" ).toString();
                    String ssCreateTime = ssInfo.get( "creationTime" )
                            .toString();
                    sessionList.add( "[_id:" + ssId + ", createTime:"
                            + ssCreateTime + "]" );
                }
                logger.error( "remain session \nremainNum = " + cnt
                        + ", session = " + sessionList );
            }
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
            if ( null != db ) {
                db.close();
            }
        }
        return sessionList;
    }

    private List< String > isRemainScmFile() throws Exception {
        Sequoiadb db = null;
        DBCursor cursor = null;
        List< String > fileList = new ArrayList<>();
        try {
            List< WsWrapper > wss = ScmInfo.getAllWorkspaces();
            for ( WsWrapper ws : wss ) {
                String wsName = ws.getName();
                db = TestSdbTools.getSdb( TestScmBase.mainSdbUrl );
                DBCollection cl = db.getCollectionSpace( wsName + "_META" )
                        .getCollection( "FILE" );

                long cnt = cl.getCount();
                if ( 0 != cnt ) {
                    cursor = cl.query();
                    while ( cursor.hasNext() ) {
                        BSONObject info = cursor.getNext();
                        String name = ( String ) info.get( "name" );
                        fileList.add( name );
                    }
                    logger.error( "remain scmfile \nwsName = " + wsName
                            + ", remainNum = " + cnt + ", " + "scmfile name = "
                            + fileList );
                }
            }
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
            if ( null != db ) {
                db.close();
            }
        }
        return fileList;
    }
}
