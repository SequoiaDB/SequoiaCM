package com.sequoiacm.scheduletask.serial;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.element.ScmScheduleSpaceRecyclingContent;
import com.sequoiacm.client.element.ScmSpaceRecycleScope;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description SCM-6360:创建调度任务后检查startExecuteTime字段
 * @Author zhangyanan
 * @Date 2023.06.01
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2023.06.01
 * @version 1.00
 */
public class ScheduleTask6360 extends TestScmBase {
    private final static int fileSize = 1024;
    private final static int fileNum = 10;
    private final static String fileName = "file6360_";
    private final static String wsName = "ws6360";
    private String csName = null;
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
    private boolean runSuccess = false;

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

        rootSiteSession = ScmSessionUtils.createSession( rootSite );
        branSiteSession = ScmSessionUtils.createSession( branchSite );

        ScmWorkspaceUtil.deleteWs( wsName, rootSiteSession );
        ScmWorkspaceUtil.createWS( rootSiteSession, wsName,
                ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( rootSiteSession, wsName );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsName,
                rootSiteSession );
        branSiteWs = ScmFactory.Workspace.getWorkspace( wsName,
                branSiteSession );

        prepareDate();
        csName = wsName + "_LOB_" + getCsClPostfix( ScmShardingType.YEAR );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        // 验证迁移调度任务
        test1();
        // 验证清理调度任务
        test2();
        // 验证迁移清理调度任务
        test3();
        // 验证空间回收调度任务
        test4();
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, rootSiteSession );
            if ( rootSiteSession != null ) {
                rootSiteSession.close();
            }
            if ( branSiteSession != null ) {
                branSiteSession.close();
            }
        }
    }

    private void checkScmSchedule( ScmSchedule scmSchedule,
            SiteWrapper[] expSites ) throws Exception {
        Date time1 = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime( time1 );
        calendar.add( Calendar.SECOND, -2 );
        time1 = calendar.getTime();

        ScmScheduleUtils.waitForTask( scmSchedule, 2 );
        if ( expSites != null ) {
            ScmScheduleUtils.checkScmFile( branSiteWs, fileIdList, expSites );

        }
        List< ScmTask > tasks = scmSchedule.getTasks( new BasicBSONObject(),
                new BasicBSONObject( ScmAttributeName.Task.ID, 1 ), 0, -1 );

        Date time3 = new Date();
        calendar = Calendar.getInstance();
        calendar.setTime( time3 );
        calendar.add( Calendar.SECOND, 2 );
        time3 = calendar.getTime();

        for ( ScmTask task : tasks ) {
            Date time2 = task.getStartExecuteTime();

            if ( time2 != null ) {
                Assert.assertTrue( time2.after( time1 ) );
                Assert.assertTrue( time2.before( time3 ) );
            }
        }

        scmSchedule.disable();
        ScmSystem.Schedule.delete( branSiteSession, scmSchedule.getId() );
        ScmScheduleUtils.cleanTask( branSiteSession, scmSchedule.getId() );
    }

    public void test1() throws Exception {
        ScmSchedule scmSchedule = ScmScheduleUtils.createCopySchedule(
                rootSiteSession, rootSite, branchSite,
                ScmInfo.getWsWrapperByWsName( rootSiteSession, wsName ),
                queryCond );

        SiteWrapper[] expSites = { rootSite, branchSite };
        checkScmSchedule( scmSchedule, expSites );
    }

    public void test2() throws Exception {
        ScmSchedule scmSchedule = ScmScheduleUtils.createCleanSchedule(
                branSiteSession, branchSite,
                ScmInfo.getWsWrapperByWsName( branSiteSession, wsName ),
                queryCond );

        SiteWrapper[] expSites = { rootSite };
        checkScmSchedule( scmSchedule, expSites );
    }

    public void test3() throws Exception {
        ScmSchedule scmSchedule = ScmScheduleUtils.createMoveSchedule(
                rootSiteSession, rootSite, branchSite, wsName, queryCond,
                ScmDataCheckLevel.WEEK, false, false );

        SiteWrapper[] expSites = { branchSite };
        checkScmSchedule( scmSchedule, expSites );
    }

    public void test4() throws Exception {
        ScmSpaceRecycleScope scmSpaceRecycleScope = ScmSpaceRecycleScope
                .mothBefore( 0 );
        ScmScheduleSpaceRecyclingContent spaceRecyclingContent = new ScmScheduleSpaceRecyclingContent(
                rootSite.getSiteName(), scmSpaceRecycleScope );
        String cron = "0/1 * * * * ?";
        ScmSchedule scmSchedule = ScmSystem.Schedule.create( rootSiteSession,
                wsName, ScheduleType.RECYCLE_SPACE, "scmSchedule_6360", "",
                spaceRecyclingContent, cron );

        checkScmSchedule( scmSchedule, null );
        boolean isCsExits = ScmScheduleUtils.checkLobCS( rootSite, csName );
        Assert.assertFalse( isCsExits, csName + " should not exist" );
    }

    public void prepareDate() throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( rootSiteWs );
            file.setFileName( fileName + i );
            file.setAuthor( fileName );
            file.setContent( filePath );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    public static String getCsClPostfix( ScmShardingType shardType )
            throws ParseException {
        Date date = new Date();
        String currY = yearFm.format( date );
        String currM = monthFm.format( date );
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
}