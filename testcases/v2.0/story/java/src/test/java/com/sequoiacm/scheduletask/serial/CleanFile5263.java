package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Descreption SCM-5263:清理多版本文件，开启空间回收
 * @Author YiPan
 * @CreateDate 2022/9/26
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class CleanFile5263 extends TestScmBase {
    private String fileName = "file5263_";
    private String taskName = "task5263";
    private String fileAuthor = "author5263";
    private String wsName = "ws_5263";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private ScmId fileId = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private ScmSchedule sche;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private long now;
    private AtomicInteger successTestCount = new AtomicInteger( 0 );

    @BeforeClass
    public void setUp() throws Exception {
        now = System.currentTimeMillis();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "updateFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, fileSize / 2 );

        List< SiteWrapper > branchSites = ScmInfo
                .getBranchSitesBySiteType( ScmType.DatasourceType.SEQUOIADB );
        branchSite = branchSites.get( 0 );
        rootSite = ScmInfo.getRootSite();
        sessionM = ScmSessionUtils.createSession( rootSite );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
    }

    @DataProvider(name = "data")
    private Object[] shardingTypes() {
        return new Object[] { ScmShardingType.YEAR, ScmShardingType.QUARTER,
                ScmShardingType.MONTH };
    }

    @Test(groups = { "twoSite", "fourSite" }, dataProvider = "data")
    public void test( ScmShardingType shardingType ) throws Exception {
        createWs( shardingType );
        wsM = ScmFactory.Workspace.getWorkspace( wsName, sessionM );
        ScmFileUtils.cleanFile( wsName, queryCond );
        String recycleCSName = createVersionFile( shardingType );
        // 缓存所有版本文件至分站点
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .in( fileId.get() ).get();
        ScmId taskId = ScmSystem.Task.startTransferTask( wsM, cond,
                ScmType.ScopeType.SCOPE_ALL, branchSite.getSiteName() );
        ScmTaskUtils.waitTaskFinish( sessionM, taskId );

        // 创建清理任务,清理分站点历史版本1的文件
        cond.put( ScmAttributeName.File.MAJOR_VERSION, 1 );
        ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
                branchSite.getSiteName(), "0d", cond,
                ScmType.ScopeType.SCOPE_HISTORY, 120000, ScmDataCheckLevel.WEEK,
                false, true );

        // 启动清理调度任务
        String cron = "0/1 * * * * ?";
        sche = ScmSystem.Schedule.create( sessionM, wsName,
                ScheduleType.CLEAN_FILE, taskName, "", content, cron );
        ScmScheduleUtils.waitForTask( sche, 2 );
        sche.disable();

        // 校验当前版本未被清理
        SiteWrapper[] expSites = { rootSite, branchSite };
        ScmFileUtils.checkMetaAndData( wsName, fileId, expSites, localPath,
                updatePath );

        // 校验历史版本被清理
        expSites = new SiteWrapper[] { rootSite };
        List< ScmId > fileIds = new ArrayList<>();
        fileIds.add( fileId );
        ScmFileUtils.checkHistoryFileMetaAndData( wsName, fileIds, expSites,
                localPath, filePath, 1, 0 );

        // 校验统计记录
        List< ScmTask > successTasks = ScmScheduleUtils.getSuccessTasks( sche );
        ScmTask task = successTasks.get( 0 );
        Assert.assertEquals( task.getEstimateCount(), 1 );
        Assert.assertEquals( task.getActualCount(), 1 );
        Assert.assertEquals( task.getSuccessCount(), 1 );
        Assert.assertEquals( task.getFailCount(), 0 );
        Assert.assertEquals( task.getProgress(), 100 );
        Object[] taskExtraInfo = ScmScheduleUtils.getTaskExtraInfo( task );
        String[] expRecycleCS = { recycleCSName };
        Assert.assertEquals( taskExtraInfo, expRecycleCS,
                "exp:" + Arrays.toString( expRecycleCS ) + " ,act:"
                        + Arrays.toString( taskExtraInfo ) );
        // 校验数据源
        Assert.assertFalse(
                ScmScheduleUtils.checkLobCS( branchSite, recycleCSName ) );

        ScmSystem.Schedule.delete( sessionM, sche.getId() );
        ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
        successTestCount.getAndIncrement();
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( successTestCount.get() == shardingTypes().length
                || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsName, queryCond );
                ScmWorkspaceUtil.deleteWs( wsName, sessionM );
            } finally {
                sessionM.close();
            }
        }
    }

    private String createVersionFile( ScmShardingType shardingType )
            throws Exception {
        ScmScheduleUtils.cleanNullCS( sessionM, wsM.getName() );
        Object[] result = initCreateTime( shardingType );
        ScmFile file = ScmFactory.File.createInstance( wsM );
        Date date = new Date( ( long ) result[ 0 ] );
        file.setCreateTime( date );
        file.setAuthor( fileAuthor );
        file.setFileName( fileName );
        file.setContent( filePath );
        fileId = file.save();
        file.updateContent( updatePath );
        return ( String ) result[ 1 ];
    }

    private Object[] initCreateTime( ScmShardingType shardingType ) {
        long create_time;
        String recycleCSName;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime( new Date( now ) );
        if ( shardingType == ScmShardingType.YEAR ) {
            // 时间向前偏移1年
            calendar.add( Calendar.YEAR, -1 );
            int year = calendar.get( Calendar.YEAR );
            recycleCSName = wsName + "_LOB_" + year;
            create_time = calendar.getTimeInMillis();
        } else if ( shardingType == ScmShardingType.QUARTER ) {
            // 时间向前偏移1季
            calendar.add( Calendar.MONTH, -3 );
            int year = calendar.get( Calendar.YEAR );
            int month = calendar.get( Calendar.MONTH ) + 1;
            int quarter = ( month - 1 ) / 3 + 1;
            recycleCSName = wsName + "_LOB_" + year + "Q" + quarter;
            create_time = calendar.getTimeInMillis();
        } else {
            // 时间向前偏移1月
            calendar.add( Calendar.MONTH, -1 );
            int year = calendar.get( Calendar.YEAR );
            int month = calendar.get( Calendar.MONTH ) + 1;
            recycleCSName = wsName + "_LOB_" + year
                    + String.format( "%02d", month );
            create_time = calendar.getTimeInMillis();
        }
        return new Object[] { create_time, recycleCSName };
    }

    private void createWs( ScmShardingType shardingType ) throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, sessionM );
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( ScmWorkspaceUtil
                .getDataLocationList( ScmInfo.getSiteNum(), shardingType ) );
        conf.setMetaLocation(
                ScmWorkspaceUtil.getMetaLocation( shardingType ) );
        conf.setName( wsName );
        ScmWorkspaceUtil.createWS( sessionM, conf );
        ScmWorkspaceUtil.wsSetPriority( sessionM, wsName );
    }
}