package com.sequoiacm.scheduletask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @Descreption SCM-1226:创建调度任务，类型为迁移，指定源和目标站点正确
 * @Author huangxiaoni
 * @CreateDate 2018-04-17
 * @UpdateUser YiPan
 * @UpdateDate 2021/9/8
 * @UpdateRemark 优化用例
 * @Version 1.0
 */
public class ScheduleTask1226 extends TestScmBase {
    private final static int fileNum = 10;
    private final static int fileSize = 100;
    private final static String name = "scheTask1226";
    private boolean runSuccess = false;
    private ScmSession ssA = null;
    private ScmWorkspace wsA = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private WsWrapper wsp = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;
    private ScmSchedule scmSchedule;
    private ScmScheduleContent content = null;
    private String cron = "* * * * * ?";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        branSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        ssA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), ssA );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        readyScmFile( wsA, 0, fileNum );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void test() throws Exception {
        SiteWrapper[] expSites = { rootSite, branSite };

        createScheduleTask();
        checkScheduleTaskInfo();
        ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites );

        readyScmFile( wsA, fileNum, fileNum + 10 );
        ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites );

        // checkTask info
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.Schedule.ID )
                .is( scmSchedule.getId().get() ).get();
        checkTaskInfo( cond );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            ScmSystem.Schedule.delete( ssA, scmSchedule.getId() );
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                ScmScheduleUtils.cleanTask( ssA, scmSchedule.getId() );
            }
        } finally {
            ssA.close();
        }
    }

    private void readyScmFile( ScmWorkspace ws, int startNum, int endNum )
            throws ScmException {
        for ( int i = startNum; i < endNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "file" + name + i );
            file.setAuthor( name );
            file.setContent( filePath );
            fileIds.add( file.save() );
        }
    }

    private void createScheduleTask() throws ScmException {
        String maxStayTime = "0d";
        ScmScheduleCopyFileContent scmScheduleCopyFileContent = new ScmScheduleCopyFileContent(
                branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                queryCond );
        Assert.assertEquals( scmScheduleCopyFileContent.getSourceSiteName(),
                branSite.getSiteName() );
        Assert.assertEquals( scmScheduleCopyFileContent.getTargetSiteName(),
                rootSite.getSiteName() );
        Assert.assertEquals( scmScheduleCopyFileContent.getMaxStayTime(),
                maxStayTime );
        Assert.assertEquals( scmScheduleCopyFileContent.getExtraCondition(),
                queryCond );
        content = scmScheduleCopyFileContent;
        scmSchedule = ScmSystem.Schedule.create( ssA, wsp.getName(),
                ScheduleType.COPY_FILE, "schedule" + name, "", content, cron );
    }

    private void checkScheduleTaskInfo() {
        Assert.assertEquals( scmSchedule.getType(), ScheduleType.COPY_FILE );
        Assert.assertEquals( scmSchedule.getName(), "schedule" + name );
        Assert.assertEquals( scmSchedule.getDesc(), "" );
        Assert.assertEquals( scmSchedule.getContent(), content );
        Assert.assertEquals( scmSchedule.getCron(), cron );
        Assert.assertEquals( scmSchedule.getWorkspace(), wsp.getName() );
    }

    private void checkTaskInfo( BSONObject cond ) throws ScmException {
        ScmCursor< ScmTaskBasicInfo > cursor = ScmSystem.Task.listTask( ssA,
                cond );
        while ( cursor.hasNext() ) {
            ScmTaskBasicInfo info = cursor.getNext();
            Assert.assertEquals( info.getRunningFlag(),
                    CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
            Assert.assertEquals( info.getType(),
                    CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
            Assert.assertEquals( info.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( info.getScheduleId(), scmSchedule.getId() );
            Assert.assertNotNull( info.getId() );
            Assert.assertEquals( info.getTargetSite(),
                    ScmInfo.getRootSite().getSiteId() );
            Assert.assertNotNull( info.getStartTime() );
        }
    }
}