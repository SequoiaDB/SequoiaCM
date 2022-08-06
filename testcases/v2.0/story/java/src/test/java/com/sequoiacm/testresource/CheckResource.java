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
    private Sequoiadb db = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        db = TestSdbTools.getSdb( TestScmBase.mainSdbUrl );
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

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testRemainBuckets() throws Exception {
        List< String > buckets = this.isRemainBuckets();
        if ( buckets.size() > 0 ) {
            throw new Exception( "remain s3 buckets \n" + buckets );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( db != null ) {
            db.close();
        }
    }

    private List< String > isRemainScmSession() {
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
        }
        return sessionList;
    }

    private List< String > isRemainBuckets() {
        List< String > bucketNames = new ArrayList<>();
        DBCursor wsCursor = null;
        try {
            wsCursor = db.getCollectionSpace( "SCMSYSTEM" )
                    .getCollection( "BUCKET" ).query();
            while ( wsCursor.hasNext() ) {
                BSONObject info = wsCursor.getNext();
                String bucket = ( String ) info.get( "name" );
                // 排除公共桶的干扰
                if ( !( bucket.contains( "comm" ) ) ) {
                    bucketNames.add( bucket );
                }
            }
        } finally {
            if ( wsCursor != null ) {
                wsCursor.close();
            }
        }
        return bucketNames;
    }

    private List< String > isRemainScmFile() {
        List< String > fileList = new ArrayList<>();
        DBCursor wsCursor = null;
        try {
            wsCursor = db.getCollectionSpace( "SCMSYSTEM" )
                    .getCollection( "WORKSPACE" ).query();
            while ( wsCursor.hasNext() ) {
                BSONObject info = wsCursor.getNext();
                String wsName = ( String ) info.get( "name" );
                fileList.addAll( getFileInfo( db, wsName ) );
                fileList.addAll( getHistoryFileInfo( db, wsName ) );
            }
        } finally {
            if ( wsCursor != null ) {
                wsCursor.close();
            }
        }
        return fileList;
    }

    /**
     * @descreption 遍历工作区下历史版本集合
     * @param db
     * @param wsName
     * @return
     */
    private List< String > getHistoryFileInfo( Sequoiadb db, String wsName ) {
        String csName = wsName + "_META";
        String clName = "FILE_HISTORY";
        return queryCl( db, csName, clName );
    }

    /**
     * @descreption 遍历工作区下当前版本集合
     * @param db
     * @param wsName
     * @return
     */
    private List< String > getFileInfo( Sequoiadb db, String wsName ) {
        String csName = wsName + "_META";
        String clName = "FILE";
        return queryCl( db, csName, clName );
    }

    /**
     * @descreption 根据集合名遍历记录
     * @param db
     * @param csName
     * @param clName
     * @return
     */
    private List< String > queryCl( Sequoiadb db, String csName,
            String clName ) {
        List< String > fileList = new ArrayList<>();
        DBCollection cl = db.getCollectionSpace( csName )
                .getCollection( clName );
        long cnt = cl.getCount();
        DBCursor infoCursor = null;
        try {
            if ( 0 != cnt ) {
                infoCursor = cl.query();
                if ( cnt < fileNum ) {
                    while ( infoCursor.hasNext() ) {
                        BSONObject info = infoCursor.getNext();
                        String name = ( String ) info.get( "name" );
                        int version = ( int ) info.get( "major_version" );
                        fileList.add( name + "-" + version );
                        logger.error( "remain scmfile \nfileInfo = "
                                + info.toString() );
                    }
                }
            }
        } finally {
            if ( infoCursor != null ) {
                infoCursor.close();
            }
        }
        return fileList;
    }
}
