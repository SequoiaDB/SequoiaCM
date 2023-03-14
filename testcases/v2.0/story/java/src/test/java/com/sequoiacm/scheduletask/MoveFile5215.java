package com.sequoiacm.scheduletask;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleMoveFileContent;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-5215:创建多版本文件迁移并清理任务
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class MoveFile5215 extends TestScmBase {
    private String fileName = "file5215_";
    private String taskName = "task5215";
    private String fileAuthor = "author5215";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private List< String > fileIdsToString = new ArrayList<>();
    private ScmSession sessionM = null;
    private WsWrapper wsp;
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private ScmSchedule sche;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private int fileNum = 10;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws IOException, ScmException, InterruptedException {
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

        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        sessionM = ScmSessionUtils.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        createFile( wsM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 创建迁移并清理任务,迁移历史版本
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .in( fileIdsToString ).get();
        cond.put( ScmAttributeName.File.MAJOR_VERSION, 1 );
        ScmScheduleMoveFileContent content = new ScmScheduleMoveFileContent(
                rootSite.getSiteName(), branchSite.getSiteName(), "0d", cond,
                ScmType.ScopeType.SCOPE_HISTORY );

        // 启动迁移并清理调度任务
        String cron = "0/1 * * * * ?";
        sche = ScmSystem.Schedule.create( sessionM, wsp.getName(),
                ScheduleType.MOVE_FILE, taskName, "", content, cron );
        ScmScheduleUtils.waitForTask( sche, 2 );
        sche.disable();

        // 校验当前版本未被迁移并清理
        SiteWrapper[] expSites = { rootSite };
        ScmFileUtils.checkMetaAndData( wsp, fileIds, expSites, localPath,
                updatePath );

        expSites = new SiteWrapper[] { branchSite };
        ScmFileUtils.checkHistoryFileMetaAndData( wsp.getName(), fileIds,
                expSites, localPath, filePath, 1, 0 );

        // 校验统计记录
        List< ScmTask > successTasks = ScmScheduleUtils.getSuccessTasks( sche );
        ScmTask task = successTasks.get( 0 );
        Assert.assertEquals( task.getEstimateCount(), fileNum );
        Assert.assertEquals( task.getActualCount(), fileNum );
        Assert.assertEquals( task.getSuccessCount(), fileNum );
        Assert.assertEquals( task.getFailCount(), 0 );
        Assert.assertEquals( task.getProgress(), 100 );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsp, queryCond );
                ScmSystem.Schedule.delete( sessionM, sche.getId() );
                ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
            } finally {
                sessionM.close();
            }
        }
    }

    private void createFile( ScmWorkspace ws ) throws ScmException, InterruptedException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + i );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            ScmId fileID = file.save();
            fileIds.add( fileID );
            fileIdsToString.add( fileID.toString() );
            file.updateContent( updatePath );
        }
        // 降低时间不同步的敏感度
        Thread.sleep( 500 );
    }
}