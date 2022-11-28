package com.sequoiacm.scheduletask.serial;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBLob;
import com.sequoiadb.base.Sequoiadb;
import org.bson.BSONObject;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleMoveFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

import static com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil.getDataLocationList;
import static com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil.getMetaLocation;

/**
 * @Description SCM-5225:迁移并清理多个文件，开启空间回收
 * @Author zhangyanan
 * @Date 2022.09.21
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2022.09.21
 * @version 1.00
 */
public class ScheduleTask5225 extends TestScmBase {
    private final static int fileSize = 1024;
    private final static String fileName = "file5225_";
    private final static String wsName = "ws5225";
    private String csName1 = null;
    private String clName1 = null;
    private String csName2 = null;
    private String clName2 = null;
    private String csName3 = null;
    private String clName3 = null;
    private ScmSession rootSiteSession = null;
    private SiteWrapper rootSite = null;
    private ScmSession branSiteSession = null;
    private SiteWrapper branchSite = null;
    private ScmWorkspace rootSiteWs = null;
    private ScmWorkspace branSiteWs = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
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

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();

        List< SiteWrapper > sites = ScmInfo
                .getBranchSitesBySiteType( ScmType.DatasourceType.SEQUOIADB );
        branchSite = sites.get( 0 );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();

        rootSiteSession = TestScmTools.createSession( rootSite );
        branSiteSession = TestScmTools.createSession( branchSite );
    }

    @DataProvider(name = "dataProvider")
    public Object[][] generateDate() {
        // 数据源按年分区
        String createTimeOfYear1 = "2022-01-01";
        String createTimeOfYear2 = "2021-01-01";
        String createTimeOfYear3 = "2020-01-01";

        // 数据源按季度分区
        String createTimeOfQuarter1 = "2022-01-01";
        String createTimeOfQuarter2 = "2022-05-01";
        String createTimeOfQuarter3 = "2022-09-01";

        // 数据源按月分区
        String createTimeOfMonth1 = "2022-01-01";
        String createTimeOfMonth2 = "2022-02-01";
        String createTimeOfMonth3 = "2022-03-01";

        return new Object[][] {
                { ScmShardingType.YEAR, createTimeOfYear1, createTimeOfYear2,
                        createTimeOfYear3 },
                { ScmShardingType.QUARTER, createTimeOfQuarter1,
                        createTimeOfQuarter2, createTimeOfQuarter3 },
                { ScmShardingType.MONTH, createTimeOfMonth1, createTimeOfMonth2,
                        createTimeOfMonth3 } };
    }

    // 问题单SEQUOIACM-1159影响，用例屏蔽
    @Test(groups = { "twoSite",
            "fourSite" }, dataProvider = "dataProvider", enabled = false)
    public void test( ScmShardingType dataLocationShardingType,
            String fileCreateTime1, String fileCreateTime2,
            String fileCreateTime3 ) throws Exception {
        createWs( dataLocationShardingType );
        prepareDate( dataLocationShardingType, fileCreateTime1, fileCreateTime2,
                fileCreateTime3 );

        ScmSchedule scmSchedule = ScmScheduleUtils.createMoveSchedule(
                rootSiteSession, rootSite, branchSite, wsName, queryCond,
                ScmDataCheckLevel.WEEK, true, false );

        ScmScheduleUtils.waitForTask( scmSchedule, 3 );
        SiteWrapper[] expSites1 = { branchSite };
        ScmScheduleUtils.checkScmFile( branSiteWs, fileIdList, expSites1 );
        scmSchedule.disable();
        ScmSystem.Schedule.delete( rootSiteSession, scmSchedule.getId() );
        ScmScheduleUtils.cleanTask( rootSiteSession, scmSchedule.getId() );

        boolean isexits1 = ScmScheduleUtils.checkLobCS( rootSite, csName1 );
        boolean isexits2 = ScmScheduleUtils.checkLobCS( rootSite, csName2 );
        boolean isexits3 = ScmScheduleUtils.checkLobCS( rootSite, csName3 );
        Assert.assertTrue( isexits1 );
        Assert.assertFalse( isexits2 );
        Assert.assertFalse( isexits3 );

        ScmScheduleUtils.deleteLobCS( rootSite, csName1 );
        ScmWorkspaceUtil.deleteWs( wsName, rootSiteSession );
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
        // 设置创建时间为2022-01-01
        fileIdList.clear();
        createFileSetCreateTime( fileCreateTime1, fileName + 1 );
        csName1 = wsName + "_LOB_"
                + getCsClPostfix( fileCreateTime1, dataLocationShardingType );
        clName1 = "LOB_"
                + getCsClPostfix( fileCreateTime1, dataLocationShardingType );
        // 分区插入无关lob
        putDataInSdb( rootSite, filePath, csName1, clName1 );

        createFileSetCreateTime( fileCreateTime2, fileName + 2 );
        csName2 = wsName + "_LOB_"
                + getCsClPostfix( fileCreateTime2, dataLocationShardingType );
        clName2 = "LOB_"
                + getCsClPostfix( fileCreateTime2, dataLocationShardingType );

        createFileSetCreateTime( fileCreateTime3, fileName + 3 );
        csName3 = wsName + "_LOB_"
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
        file.setContent( filePath );
        file.setCreateTime( times );
        ScmId fileId = file.save();

        fileIdList.add( fileId );
    }

    public void createWs( ScmShardingType dataLocationShardingType )
            throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, rootSiteSession );

        ScmWorkspaceUtil.createWS( rootSiteSession, wsName,
                ScmInfo.getSiteNum(), dataLocationShardingType );
        ScmWorkspaceUtil.wsSetPriority( rootSiteSession, wsName );

        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsName,
                rootSiteSession );

        branSiteWs = ScmFactory.Workspace.getWorkspace( wsName,
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