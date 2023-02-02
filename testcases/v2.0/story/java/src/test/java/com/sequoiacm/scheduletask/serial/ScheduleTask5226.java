package com.sequoiacm.scheduletask.serial;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBLob;
import com.sequoiadb.base.Sequoiadb;

/**
 * @Description SCM-5226:迁移并清理多版本文件，开启空间回收
 * @Author zhangyanan
 * @Date 2022.09.21
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2022.09.21
 * @version 1.00
 */
public class ScheduleTask5226 extends TestScmBase {
    private final static int fileSize = 1024;
    private final static String fileName = "file5226_";
    private final static String wsNameA = "ws5226";
    private String csName1 = null;
    private String clName1 = null;
    private String csName2 = null;
    private String clName2 = null;
    private String csName3 = null;
    private String clName3 = null;
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private SiteWrapper rootSite = null;
    private ScmSession branSiteSession = null;
    private ScmWorkspace branSiteWs = null;
    private SiteWrapper branchSite = null;
    private ArrayList< ScmId > fileIdList = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private BSONObject queryCond = null;
    private static SimpleDateFormat yearFm = new SimpleDateFormat( "yyyy" );
    private static SimpleDateFormat monthFm = new SimpleDateFormat( "MM" );
    private AtomicInteger runSuccessCount = new AtomicInteger( 0 );

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        tmpPath = localPath + File.separator + "tmpFile_" + fileSize + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( tmpPath, fileSize * 2 );

        rootSite = ScmInfo.getRootSite();

        List< SiteWrapper > sites = ScmInfo
                .getBranchSitesBySiteType( ScmType.DatasourceType.SEQUOIADB );
        branchSite = sites.get( 0 );

        rootSiteSession = TestScmTools.createSession( rootSite );
        branSiteSession = TestScmTools.createSession( branchSite );
    }

    @DataProvider(name = "dataProvider")
    public Object[][] generateDate() {
        // 数据源按年分区
        String createTimeOfYear1 = "2021-01-01";
        String createTimeOfYear2 = "2020-01-01";
        String createTimeOfYear3 = "2019-01-01";

        // 数据源按季度分区
        String createTimeOfQuarter1 = "2021-01-01";
        String createTimeOfQuarter2 = "2021-05-01";
        String createTimeOfQuarter3 = "2021-09-01";

        // 数据源按月分区
        String createTimeOfMonth1 = "2021-01-01";
        String createTimeOfMonth2 = "2021-02-01";
        String createTimeOfMonth3 = "2021-03-01";

        return new Object[][] {
                { ScmShardingType.YEAR, createTimeOfYear1, createTimeOfYear2,
                        createTimeOfYear3 },
                { ScmShardingType.QUARTER, createTimeOfQuarter1,
                        createTimeOfQuarter2, createTimeOfQuarter3 },
                { ScmShardingType.MONTH, createTimeOfMonth1, createTimeOfMonth2,
                        createTimeOfMonth3 } };
    }

    @Test(groups = { "twoSite", "fourSite" }, dataProvider = "dataProvider")
    public void test( ScmShardingType dataLocationShardingType,
            String fileCreateTime1, String fileCreateTime2,
            String fileCreateTime3 ) throws Exception {

        int historyVersion = 1;
        int minorVersion = 0;

        createWs( dataLocationShardingType );
        prepareDate( dataLocationShardingType, fileCreateTime1, fileCreateTime2,
                fileCreateTime3 );

        ArrayList< String > fileIds = new ArrayList<>();
        for ( ScmId fileId : fileIdList ) {
            fileIds.add( fileId.toString() );
        }
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .in( fileIds ).get();

        ScmSchedule scmSchedule = ScmScheduleUtils.createMoveSchedule(
                rootSiteSession, rootSite, branchSite, wsNameA, queryCond,
                ScmType.ScopeType.SCOPE_HISTORY, ScmDataCheckLevel.WEEK, true,
                false );

        ScmScheduleUtils.waitForTask( scmSchedule, 3 );
        SiteWrapper[] expSites1 = { branchSite };

        ScmFileUtils.checkHistoryFileMetaAndData( wsNameA, fileIdList,
                expSites1, localPath, filePath, historyVersion, minorVersion );

        scmSchedule.disable();
        ScmSystem.Schedule.delete( rootSiteSession, scmSchedule.getId() );
        ScmScheduleUtils.cleanTask( rootSiteSession, scmSchedule.getId() );

        boolean isexits1 = ScmScheduleUtils.checkLobCS( rootSite, csName1 );
        boolean isexits2 = ScmScheduleUtils.checkLobCS( rootSite, csName2 );
        boolean isexits3 = ScmScheduleUtils.checkLobCS( rootSite, csName3 );

        // 存在残留lob的表未被清理
        Assert.assertTrue( isexits1,
                "残留lob的表被清理，表名为:" + csName1 + "." + clName1 );

        // 数据源空表被清理
        Assert.assertFalse( isexits2,
                "数据源空表未清理，表名为:" + csName2 + "." + clName2 );
        Assert.assertFalse( isexits3,
                "数据源空表未清理，表名为:" + csName3 + "." + clName3 );

        ScmScheduleUtils.deleteLobCS( rootSite, csName1 );
        ScmWorkspaceUtil.deleteWs( wsNameA, rootSiteSession );
        runSuccessCount.incrementAndGet();
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccessCount.get() == generateDate().length
                    || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( rootSiteSession != null ) {
                rootSiteSession.close();
            }
            if ( branSiteSession != null ) {
                branSiteSession.close();
            }
        }
    }

    public void prepareDate( ScmShardingType dataLocationShardingType,
            String fileCreateTime1, String fileCreateTime2,
            String fileCreateTime3 ) throws Exception {
        fileIdList.clear();
        // 上传文件设置创建时间
        createFileSetCreateTime( fileCreateTime1, fileName + 1 );
        csName1 = wsNameA + "_LOB_"
                + getCsClPostfix( fileCreateTime1, dataLocationShardingType );
        clName1 = "LOB_"
                + getCsClPostfix( fileCreateTime1, dataLocationShardingType );
        // 分区插入无关lob
        putDataInSdb( rootSite, filePath, csName1, clName1 );

        createFileSetCreateTime( fileCreateTime2, fileName + 2 );
        csName2 = wsNameA + "_LOB_"
                + getCsClPostfix( fileCreateTime2, dataLocationShardingType );
        clName2 = "LOB_"
                + getCsClPostfix( fileCreateTime2, dataLocationShardingType );

        createFileSetCreateTime( fileCreateTime3, fileName + 3 );
        csName3 = wsNameA + "_LOB_"
                + getCsClPostfix( fileCreateTime3, dataLocationShardingType );
        clName3 = "LOB_"
                + getCsClPostfix( fileCreateTime3, dataLocationShardingType );

    }

    public void createFileSetCreateTime( String time, String filename )
            throws Exception {
        Date times = new SimpleDateFormat( "yyyy-MM-dd" ).parse( time );
        // 主站点创建文件
        ScmFile file = ScmFactory.File.createInstance( rootSiteWs );
        file.setFileName( filename );
        file.setAuthor( fileName );
        file.setCreateTime( times );
        file.setContent( filePath );
        ScmId fileId = file.save();
        file.updateContent( tmpPath );

        fileIdList.add( fileId );
    }

    public void createWs( ScmShardingType dataLocationShardingType )
            throws Exception {
        ScmWorkspaceUtil.deleteWs( wsNameA, rootSiteSession );

        ScmWorkspaceUtil.createWS( rootSiteSession, wsNameA,
                ScmInfo.getSiteNum(), dataLocationShardingType );
        ScmWorkspaceUtil.wsSetPriority( rootSiteSession, wsNameA );

        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsNameA,
                rootSiteSession );
        branSiteWs = ScmFactory.Workspace.getWorkspace( wsNameA,
                branSiteSession );
    }

    public static String getCsClPostfix( String time,
            ScmShardingType shardType ) throws ParseException {
        Date currTime = new SimpleDateFormat( "yyyy-MM-dd" ).parse( time );
        String currY = yearFm.format( currTime );
        String currM = monthFm.format( currTime );
        String postfix = null;
        if ( shardType.equals( ScmShardingType.NONE ) ) {
            postfix = "";
        } else if ( shardType.equals( ScmShardingType.YEAR ) ) {
            postfix = currY;
        } else if ( shardType.equals( ScmShardingType.QUARTER ) ) {
            int quarter = ( int ) Math.ceil( Double.parseDouble( currM ) / 3 );
            postfix = currY + "Q" + quarter;
        } else if ( shardType.equals( ScmShardingType.MONTH ) ) {
            postfix = currY + currM;
        }
        return postfix;
    }

    private static void putDataInSdb( SiteWrapper site, String lobPath,
            String csName, String clName ) throws IOException {
        Sequoiadb db = null;
        DBLob lobDB = null;
        InputStream ism = null;
        try {
            String dsUrl = site.getDataDsUrl();
            db = TestSdbTools.getSdb( dsUrl );

            if ( !db.isCollectionSpaceExist( csName ) ) {
                Assert.fail( "写lob失败,数据源上lob表不存在" );
            }

            if ( db.isCollectionSpaceExist( csName ) ) {
                DBCollection clDB = db.getCollectionSpace( csName )
                        .getCollection( clName );

                lobDB = clDB.createLob(
                        new ObjectId( "00005571c9f93f03e8d8dd57" ) );
                ism = new FileInputStream( lobPath );
                lobDB.write( ism );
            }
        } finally {
            if ( lobDB != null ) {
                lobDB.close();
            }
            if ( ism != null ) {
                ism.close();
            }
            if ( db != null ) {
                db.close();
            }
        }
    }
}