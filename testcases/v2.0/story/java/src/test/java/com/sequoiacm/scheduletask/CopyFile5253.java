package com.sequoiacm.scheduletask;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
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
 * @Descreption SCM-5253:创建迁移任务，设置快速启动，关闭快速启动，等待任务触发
 * @Author YiPan
 * @CreateDate 2022/9/26
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class CopyFile5253 extends TestScmBase {
    private String fileName = "file5253_";
    private String taskName = "task5253";
    private String fileAuthor = "author5253";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private List< ScmId > fileIds = null;
    private ScmSession sessionM = null;
    private WsWrapper wsp;
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private ScmSchedule sche;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int fileNum = 10;
    private boolean runSuccess = false;
    private ScmScheduleCopyFileContent content = null;

    @BeforeClass
    public void setUp() throws IOException, ScmException, InterruptedException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

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
        // 创建迁移任务，设置快速启动
        copyFileQuickStart();
        // 关闭快速启动，创建文件，等待调度任务触发
        modifyNoQuickStart();
        runSuccess = true;
    }

    public void modifyNoQuickStart() throws Exception {
        // 创建迁移任务
        content.setQuickStart( false );
        sche.updateSchedule( ScheduleType.COPY_FILE, content );

        // 创建文件
        createFile( wsM );

        sche.enable();

        checkResult( content.isQuickStart() );

    }

    public void copyFileQuickStart() throws Exception {
        // 创建迁移任务
        content = new ScmScheduleCopyFileContent( rootSite.getSiteName(),
                branchSite.getSiteName(), "0d", queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, 120000, ScmDataCheckLevel.WEEK,
                true );

        // 启动迁移调度任务
        String cron = "0/1 * * * * ?";
        sche = ScmSystem.Schedule.create( sessionM, wsp.getName(),
                ScheduleType.COPY_FILE, taskName, "", content, cron );

        checkResult( content.isQuickStart() );

        sche.disable();
        ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    private void checkResult( boolean isQuickStart ) throws Exception {
        // 校验任务执行结果
        ScmScheduleUtils.waitForTask( sche, 2 );
        SiteWrapper[] expSites = { rootSite, branchSite };
        ScmFileUtils.checkMetaAndData( wsp, fileIds, expSites, localPath,
                filePath );

        // 校验统计记录
        List< ScmTask > successTasks = ScmScheduleUtils.getSuccessTasks( sche );
        ScmTask task = successTasks.get( 0 );
        if ( isQuickStart ) {
            Assert.assertEquals( task.getEstimateCount(), -1 );
        } else {
            Assert.assertEquals( task.getEstimateCount(), fileNum );
        }
        Assert.assertEquals( task.getActualCount(), fileNum );
        Assert.assertEquals( task.getSuccessCount(), fileNum );
        Assert.assertEquals( task.getFailCount(), 0 );
        Assert.assertEquals( task.getProgress(), 100 );
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
        fileIds = new ArrayList<>();
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + i );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            fileIds.add( file.save() );
        }
        // 降低时间不同步的敏感度
        Thread.sleep( 500 );
    }
}