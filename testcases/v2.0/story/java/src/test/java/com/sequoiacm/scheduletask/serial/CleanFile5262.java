package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Descreption SCM-5262:清理多个文件，开启空间回收
 * @Author YiPan
 * @CreateDate 2022/9/26
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class CleanFile5262 extends TestScmBase {
    private String fileName = "file5262_";
    private String taskName = "task5262";
    private String fileAuthor = "author5262";
    private String wsName = "ws_5262";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private ScmSession sessionM = null;
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private ScmSchedule sche;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int recycleCSNum = 10;
    private long now;
    private AtomicInteger successTestCount = new AtomicInteger( 0 );

    @BeforeClass
    public void setUp() throws Exception {
        now = System.currentTimeMillis();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        List< SiteWrapper > branchSites = ScmInfo
                .getBranchSitesBySiteType( ScmType.DatasourceType.SEQUOIADB );
        branchSite = branchSites.get( 0 );
        rootSite = ScmInfo.getRootSite();
        sessionM = TestScmTools.createSession( rootSite );
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
        List< String > expCSNames = createFile( shardingType );
        // 创建清理任务对象，清理主站点
        ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
                branchSite.getSiteName(), "0d", queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, 120000, ScmDataCheckLevel.WEEK,
                false, true );

        // 启动清理调度任务
        String cron = "0/10 * * * * ?";
        sche = ScmSystem.Schedule.create( sessionM, wsName,
                ScheduleType.CLEAN_FILE, taskName, "", content, cron );

        // 调度等待任务执行2次
        ScmScheduleUtils.waitForTask( sche, 2 );
        sche.disable();

        // 校验结果
        SiteWrapper[] expSites = { rootSite };
        ScmFileUtils.checkMetaAndData( wsName, fileIds, expSites, localPath,
                filePath );

        // 校验统计记录
        ScmTask task = ScmScheduleUtils.getSuccessTasks( sche ).get( 0 );
        Assert.assertEquals( task.getEstimateCount(), recycleCSNum );
        Assert.assertEquals( task.getActualCount(), recycleCSNum );
        Assert.assertEquals( task.getSuccessCount(), recycleCSNum );
        Assert.assertEquals( task.getFailCount(), 0 );
        Assert.assertEquals( task.getProgress(), 100 );
        Object[] actCSName = ScmScheduleUtils.getTaskExtraInfo( task );
        Assert.assertEqualsNoOrder( actCSName, expCSNames.toArray(),
                "exp:" + expCSNames + " ,act:" + Arrays.toString( actCSName ) );

        // 校验数据源
        for ( String csName : expCSNames ) {
            Assert.assertFalse(
                    ScmScheduleUtils.checkLobCS( branchSite, csName ) );
        }

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

    private List< String > createFile( ScmShardingType shardingType )
            throws Exception {
        ScmScheduleUtils.cleanNullCS( sessionM, wsName );
        fileIds.clear();
        BSONObject bson = initCreateTime( shardingType, recycleCSNum );
        Set< String > key = bson.keySet();
        List< String > recycleCSNames = new ArrayList<>();
        for ( String createTime : key ) {
            ScmFile file = ScmFactory.File.createInstance( wsM );
            file.setCreateTime( new Date( Long.parseLong( createTime ) ) );
            recycleCSNames.add( ( String ) bson.get( createTime ) );
            file.setAuthor( fileAuthor );
            file.setFileName( fileName + UUID.randomUUID() );
            file.setContent( filePath );
            fileIds.add( file.save() );
        }
        ScmId task = ScmSystem.Task.startTransferTask( wsM, queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, branchSite.getSiteName() );
        ScmTaskUtils.waitTaskFinish( sessionM, task );
        return recycleCSNames;
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

    private BSONObject initCreateTime( ScmShardingType shardingType,
            int fileNum ) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime( new Date( now ) );
        BSONObject result = new BasicBSONObject();
        if ( shardingType == ScmShardingType.YEAR ) {
            for ( int i = 0; i < fileNum; i++ ) {
                // 时间向前偏移i年
                calendar.add( Calendar.YEAR, -i );
                int year = calendar.get( Calendar.YEAR );
                result.put( String.valueOf( calendar.getTimeInMillis() ),
                        wsName + "_LOB_" + year );
            }
        } else if ( shardingType == ScmShardingType.QUARTER ) {
            for ( int i = 0; i < fileNum; i++ ) {
                // 时间向前偏移i季
                calendar.add( Calendar.MONTH, -( i * 3 ) );
                int year = calendar.get( Calendar.YEAR );
                int month = calendar.get( Calendar.MONTH ) + 1;
                int quarter = ( month - 1 ) / 3 + 1;
                result.put( String.valueOf( calendar.getTimeInMillis() ),
                        wsName + "_LOB_" + year + "Q" + quarter );
            }

        } else {
            for ( int i = 0; i < fileNum; i++ ) {
                // 时间向前偏移i月
                calendar.add( Calendar.MONTH, -1 );
                int year = calendar.get( Calendar.YEAR );
                int month = calendar.get( Calendar.MONTH ) + 1;
                result.put( String.valueOf( calendar.getTimeInMillis() ), wsName
                        + "_LOB_" + year + String.format( "%02d", month ) );
            }
        }
        return result;
    }
}