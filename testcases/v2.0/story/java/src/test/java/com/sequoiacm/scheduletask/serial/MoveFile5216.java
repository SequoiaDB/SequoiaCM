package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

/**
 * @Descreption SCM-5216:修改迁移并清理任务为空间回收任务
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class MoveFile5216 extends TestScmBase {
    private String fileName = "file5216_";
    private String taskName = "task5216";
    private String fileAuthor = "author5216";
    private String wsName = "ws_5216";
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
        branchSite = ScmInfo.getBranchSite();
        sessionM = TestScmTools.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, sessionM );
        wsM = ScmWorkspaceUtil.createWS( sessionM, wsName,
                ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( sessionM, wsName );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        prepare();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 创建迁移并清理任务
        ScmScheduleMoveFileContent content = new ScmScheduleMoveFileContent(
                rootSite.getSiteName(), branchSite.getSiteName(), "0d",
                queryCond, ScmType.ScopeType.SCOPE_CURRENT );

        // 启动迁移并清理调度任务
        String cron = "0/1 * * * * ?";
        sche = ScmSystem.Schedule.create( sessionM, wsName,
                ScheduleType.MOVE_FILE, taskName, "", content, cron );

        // 调度等待任务执行2次
        ScmScheduleUtils.waitForTask( sche, 2 );
        sche.disable();
        ScmScheduleUtils.cleanTask( sessionM, sche.getId() );

        // 修改任务为空间回收任务，再次启动
        ScmScheduleSpaceRecyclingContent spaceRecyclingContent = new ScmScheduleSpaceRecyclingContent(
                rootSite.getSiteName(), ScmSpaceRecycleScope.mothBefore( 0 ) );
        sche.updateSchedule( ScheduleType.RECYCLE_SPACE,
                spaceRecyclingContent );
        sche.enable();
        ScmScheduleUtils.waitForTask( sche, 2 );

        // 校验统计记录
        ScmTask task = ScmScheduleUtils.getSuccessTasks( sche ).get( 0 );
        Assert.assertEquals( task.getEstimateCount(), recycleCSNum );
        Assert.assertEquals( task.getActualCount(), recycleCSNum );
        Assert.assertEquals( task.getSuccessCount(), recycleCSNum );
        Assert.assertEquals( task.getFailCount(), 0 );
        Assert.assertEquals( task.getProgress(), 100 );
        Object[] actCSName = ScmScheduleUtils.getTaskExtraInfo( task );
        Set< String > expCsName = ScmScheduleUtils.initLobCSName( wsName,
                ScmShardingType.YEAR, System.currentTimeMillis(),
                recycleCSNum );
        Assert.assertEqualsNoOrder( actCSName, expCsName.toArray() );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsName, queryCond );
                ScmSystem.Schedule.delete( sessionM, sche.getId() );
                ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
                ScmWorkspaceUtil.deleteWs( wsName, sessionM );
            } finally {
                sessionM.close();
            }
        }
    }

    // 构造空的cs
    private void prepare() throws Exception {
        Calendar instance = Calendar.getInstance();
        ScmScheduleUtils.checkMonthChange( instance );
        for ( int i = 0; i < recycleCSNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsM );
            file.setCreateTime( instance.getTime() );
            file.setAuthor( fileAuthor );
            file.setFileName( fileName + i );
            file.setContent( filePath );
            fileIds.add( file.save() );
            instance.add( Calendar.YEAR, -1 );
        }
        ScmFileUtils.cleanFile( wsName, queryCond );
    }
}