/**
 *
 */
package com.sequoiacm.testresource;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.TestScmListener;
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

    @Test
    public static void checkRemain() throws Exception {
        Sequoiadb db = TestSdbTools.getSdb( TestScmBase.mainSdbUrl );
        try {
            List< String > sessionList = isRemainScmSession( db );
            List< String > fileList = isRemainScmFile( db );
            List< String > skipTests = TestScmListener.skipTests;
            if ( sessionList.size() + fileList.size() + skipTests.size() > 0 ) {
                throw new Exception( "remain session: \n" + sessionList + "\n"
                        + "remain scmFile: \n" + fileList + "\n"
                        + "abnormal skip test: \n" + skipTests );
            }
        } finally {
            db.close();
        }
    }

    private static List< String > isRemainScmSession( Sequoiadb db ) {
        DBCursor cursor = null;
        List< String > sessionList = new ArrayList<>();
        try {
            CollectionSpace cs = db.getCollectionSpace( TestSdbTools.SCM_CS );
            DBCollection cl = cs.getCollection( TestSdbTools.SCM_CL_SESSION );

            long cnt = cl.getCount();
            if ( 0 != cnt ) {
                cursor = cl.query();
                while ( cursor.hasNext() ) {
                    BSONObject ssInfo = cursor.getNext();
                    logger.error( "remain session \nremainNum = " + cnt
                            + ", session = " + sessionList );
                    String ssId = ssInfo.get( "_id" ).toString();
                    String ssCreateTime = ssInfo.get( "creationTime" )
                            .toString();

                    BSONObject attributes = ( BSONObject ) ssInfo
                            .get( "attributes" );
                    BSONObject spring_security_context = ( BSONObject ) attributes
                            .get( "SPRING_SECURITY_CONTEXT" );
                    BSONObject authentication = ( BSONObject ) spring_security_context
                            .get( "authentication" );
                    BSONObject details = ( BSONObject ) authentication
                            .get( "details" );
                    BSONObject signatureInfo = ( BSONObject ) details
                            .get( "signatureInfo" );
                    if ( signatureInfo != null ) {
                        String secretKeyPrefix = ( String ) signatureInfo
                                .get( "secretKeyPrefix" );
                        if ( !secretKeyPrefix.equals( "AWS4" ) ) {
                            sessionList.add( "[_id:" + ssId + ", createTime:"
                                    + ssCreateTime + "]" );
                        }
                    } else {
                        sessionList.add( "[_id:" + ssId + ", createTime:"
                                + ssCreateTime + "]" );
                    }
                }
            }
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
        }
        return sessionList;
    }

    private static List< String > isRemainScmFile( Sequoiadb db ) {
        DBCursor cursor = null;
        List< String > fileList = new ArrayList<>();
        try {
            List< WsWrapper > wss = ScmInfo.getAllWorkspaces();
            for ( WsWrapper ws : wss ) {
                String wsName = ws.getName();
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
        }
        return fileList;
    }
}
