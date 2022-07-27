package com.sequoiacm.testresource;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
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
    private static final int fileNum = 500;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testRemainSession() throws Exception {
        List< String > sessionList = this.isRemainScmSession();
        int session_num = 0;
        if ( sessionList.size() > session_num ) {
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
                        String accessKey = ( String ) signatureInfo
                                .get( "accessKey" );
                        if ( !accessKey.equals( TestScmBase.s3AccessKeyID ) ) {
                            sessionList.add( "[_id:" + ssId + ", createTime:"
                                    + ssCreateTime + ", s3]" );
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
            if ( null != db ) {
                db.close();
            }
        }
        return sessionList;
    }

    private List< String > isRemainScmFile() throws Exception {
        List< String > fileList = new ArrayList<>();
        Sequoiadb db = null;
        DBCursor wsCursor = null;
        try {
            db = TestSdbTools.getSdb( TestScmBase.mainSdbUrl );
            wsCursor = db.getCollectionSpace( "SCMSYSTEM" )
                    .getCollection( "WORKSPACE" ).query();
            while ( wsCursor.hasNext() ) {
                BSONObject info = wsCursor.getNext();
                String wsName = ( String ) info.get( "name" );
                getFileInfo( db, wsName, fileList );
            }
        } finally {
            if ( wsCursor != null ) {
                wsCursor.close();
            }
            if ( db != null ) {
                db.close();
            }
        }
        return fileList;
    }

    private void getFileInfo( Sequoiadb db, String wsName,
            List< String > fileList ) {
        DBCollection cl = db.getCollectionSpace( wsName + "_META" )
                .getCollection( "FILE" );
        long cnt = cl.getCount();
        DBCursor infoCursor = null;
        try {
            if ( 0 != cnt ) {
                infoCursor = cl.query();
                if ( cnt < fileNum ) {
                    while ( infoCursor.hasNext() ) {
                        BSONObject info = infoCursor.getNext();
                        String name = ( String ) info.get( "name" );
                        fileList.add( name );
                        logger.error( "remain scmfile \nwsName = " + wsName
                                + ",fileInfo = " + info.toString() );
                    }
                } else {
                    while ( infoCursor.hasNext() ) {
                        BSONObject info = infoCursor.getNext();
                        String name = ( String ) info.get( "name" );
                        fileList.add( name );
                    }
                    logger.error( "remain scmfile \nwsName = " + wsName
                            + ", remainNum = " + cnt + ", " + "scmfile name = "
                            + fileList.subList(
                                    ( int ) ( fileList.size() - cnt ),
                                    fileList.size() ) );
                }
            }
        } finally {
            if ( infoCursor != null ) {
                infoCursor.close();
            }
        }
    }
}
