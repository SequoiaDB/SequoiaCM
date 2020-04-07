package com.sequoiacm.scheduletask;

import java.io.File;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @FileName SCM-2365:删除ws，会把该ws下调度任务删除
 * @Author fanyu
 * @Date 2019-01-28
 * @Version 1.00
 */

public class CreateSche_deleteWs2365 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmSession ssA = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private String wsName = "ws2365";
    private ScmWorkspace ws = null;
    private int fileSize = 0;
    private String name = "schetask2365";
    private File localPath = null;
    private String filePath = null;
    private ScmId scheduleId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        // ready local file
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        // get site and workspace, create session
        rootSite = ScmInfo.getRootSite();
        branSite = ScmInfo.getBranchSite();
        ssA = TestScmTools.createSession( branSite );
        ScmWorkspaceUtil.deleteWs( wsName, ssA );
        ScmWorkspaceUtil.createWS( ssA, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( ssA, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, ssA );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmFileUtils.create( ws, name, filePath );
        //create schedule
        this.createScheduleTask();
        //deletews
        ScmWorkspaceUtil.deleteWs( wsName, ssA );
        //check schedule was deleted in sdb
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.Schedule.ID )
                .is( scheduleId.get() ).get();
        ScmCursor< ScmScheduleBasicInfo > cursor = ScmSystem.Schedule
                .list( ssA, cond );
        Assert.assertFalse( cursor.hasNext() );
        cursor.close();

        //create ws
        ScmWorkspaceUtil.createWS( ssA, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( ssA, wsName );
        //create file to make sure schedule is not running again
        ScmCursor< ScmTaskBasicInfo > cursor1 = ScmSystem.Task
                .listTask( ssA, cond );
        Assert.assertFalse( cursor1.hasNext() );

        SiteWrapper[] sites = { branSite };
        ScmId fileId = ScmFileUtils.create( ws, name, filePath );
        ScmFileUtils.checkMeta( ws, fileId, sites );
        ScmFileUtils.checkData( ws, fileId, localPath, filePath );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            ScmWorkspaceUtil.deleteWs( wsName, ssA );
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( ssA != null ) {
                ssA.close();
            }
        }
    }

    private void createScheduleTask() throws ScmException {
        String maxStayTime = "0d";
        // create schedule task
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( name ).get();
        ScmScheduleContent content = new ScmScheduleCopyFileContent(
                branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                queryCond );
        String cron = "* * * * * ?";
        ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsName,
                ScheduleType.COPY_FILE, name, "", content, cron );
        scheduleId = sche.getId();
    }
}