package com.sequoiacm.scheduletask;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-5246:修改空间回收任务为迁移任务
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class SpaceRecycle5246 extends TestScmBase {
    private String fileName = "file5246_";
    private String taskName = "task5246";
    private String fileAuthor = "author5246";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private ScmSession sessionM = null;
    private ScmWorkspace wsM;
    private WsWrapper wsp;
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
        wsp = ScmInfo.getWs();
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        createFile();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 创建空间回收任务
        ScmSpaceRecycleScope scmSpaceRecycleScope = ScmSpaceRecycleScope
                .mothBefore( 0 );
        ScmScheduleSpaceRecyclingContent spaceRecyclingContent = new ScmScheduleSpaceRecyclingContent(
                rootSite.getSiteName(), scmSpaceRecycleScope );
        String cron = "0/10 * * * * ?";
        sche = ScmSystem.Schedule.create( sessionM, wsp.getName(),
                ScheduleType.RECYCLE_SPACE, taskName, "", spaceRecyclingContent,
                cron );

        // 调度等待任务执行2次,校验任务类型
        ScmScheduleUtils.waitForTask( sche, 2 );
        ScmTask latestTask = sche.getLatestTask();
        Assert.assertEquals( latestTask.getType(),
                CommonDefine.TaskType.SCM_TASK_RECYCLE_SPACE );
        sche.disable();
        ScmScheduleUtils.cleanTask( sessionM, sche.getId() );

        // 修改任务为迁移任务，再次启动
        ScmScheduleCopyFileContent copyFileContent = new ScmScheduleCopyFileContent(
                rootSite.getSiteName(), branchSite.getSiteName(), "0d",
                queryCond, ScmType.ScopeType.SCOPE_CURRENT );
        sche.updateSchedule( ScheduleType.COPY_FILE, copyFileContent );
        sche.enable();
        ScmScheduleUtils.waitForTask( sche, 2 );
        SiteWrapper[] expSites = { rootSite, branchSite };
        ScmFileUtils.checkMetaAndData( wsp, fileIds, expSites, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                ScmSystem.Schedule.delete( sessionM, sche.getId() );
                ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
            } finally {
                sessionM.close();
            }
        }
    }

    private void createFile() throws ScmException {
        for ( int i = 0; i < recycleCSNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsM );
            file.setAuthor( fileAuthor );
            file.setFileName( fileName + i );
            file.setContent( filePath );
            fileIds.add( file.save() );
        }
    }
}