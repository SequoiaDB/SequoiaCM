package com.sequoiacm.scheduletask;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
 * @FileName SCM-2370:schedule任务从启用到禁用再到启用
 * @Author fanyu
 * @Date 2019-01-28
 * @Version 1.00
 */

public class CreateSche_enable2dis2370 extends TestScmBase {
    private final static int fileNum = 10;
    private final static int fileSize = 100;
    private final static String name = "schetask2370";
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

    private ScmId scheduleId = null;
    private ScmScheduleContent content = null;
    private String cron = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException, ParseException {
        // ready local file
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        // get site and workspace, create session
        rootSite = ScmInfo.getRootSite();
        branSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        ssA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), ssA );
        // clean environment
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        // ready scmFile
        this.readyScmFile( wsA, 0, fileNum );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        // enable
        SiteWrapper[] expSites = { rootSite, branSite };
        ScmSchedule schedule = this.createScheduleTask();
        this.checkScheduleTaskInfo( true );
        ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites );
        this.readyScmFile( wsA, fileNum, fileNum + 10 );
        ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites );
        // checkTask info
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.Schedule.ID )
                .is( scheduleId.get() ).get();
        checkTaskInfo( cond );

        // disable
        schedule.disable();
        waitSchedueDisable( cond );
        SiteWrapper[] expSites1 = { branSite };
        this.checkScheduleTaskInfo( false );
        this.readyScmFile( wsA, fileNum + 10, fileNum + 20 );
        ScmScheduleUtils.checkScmFile( wsA,
                fileIds.subList( fileNum + 10, fileNum + 20 ), expSites1 );

        // enable
        schedule.enable();
        SiteWrapper[] expSites2 = { rootSite, branSite };
        this.checkScheduleTaskInfo( true );
        ScmScheduleUtils.checkScmFile( wsA,
                fileIds.subList( fileNum + 10, fileNum + 20 ), expSites2 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            ScmSystem.Schedule.delete( ssA, scheduleId );
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                ScmScheduleUtils.cleanTask( ssA, scheduleId );
            }
        } finally {
            if ( ssA != null ) {
                ssA.close();
            }
        }
    }

    private void readyScmFile( ScmWorkspace ws, int startNum, int endNum )
            throws ScmException, ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.set( Calendar.HOUR, calendar.get( Calendar.HOUR ) - 3 );
        for ( int i = startNum; i < endNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePath );
            file.setFileName( name + "_" + i );
            file.setCreateTime( calendar.getTime() );
            file.setAuthor( name );
            ScmId fileId = file.save();
            fileIds.add( fileId );
        }
    }

    private ScmSchedule createScheduleTask() throws ScmException {
        String maxStayTime = "0d";
        // create schedule task
        content = new ScmScheduleCopyFileContent( branSite.getSiteName(),
                rootSite.getSiteName(), maxStayTime, queryCond );
        cron = "* * * * * ?";
        ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsp.getName(),
                ScheduleType.COPY_FILE, name, "", content, cron );
        scheduleId = sche.getId();
        return sche;
    }

    private void checkScheduleTaskInfo( boolean isEnable ) throws ScmException {
        ScmSchedule sche = ScmSystem.Schedule.get( ssA, scheduleId );
        Assert.assertEquals( sche.getId(), scheduleId );
        Assert.assertEquals( sche.getType(), ScheduleType.COPY_FILE );
        Assert.assertEquals( sche.getName(), name );
        Assert.assertEquals( sche.getDesc(), "" );
        Assert.assertEquals( sche.getContent(), content );
        Assert.assertEquals( sche.getCron(), cron );
        Assert.assertEquals( sche.getWorkspace(), wsp.getName() );
        Assert.assertEquals( sche.isEnable(), isEnable );
    }

    private void checkTaskInfo( BSONObject cond ) throws ScmException {
        ScmCursor< ScmTaskBasicInfo > cursor = ScmSystem.Task.listTask( ssA,
                cond );
        try {
            while ( cursor.hasNext() ) {
                ScmTaskBasicInfo info = cursor.getNext();
                Assert.assertEquals( info.getRunningFlag(),
                        CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
                Assert.assertEquals( info.getType(),
                        CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
                Assert.assertEquals( info.getWorkspaceName(), wsp.getName() );
                Assert.assertEquals( info.getScheduleId(), scheduleId );
                Assert.assertNotNull( info.getId() );
                Assert.assertEquals( info.getTargetSite(),
                        ScmInfo.getRootSite().getSiteId() );
                Assert.assertNotNull( info.getStartTime() );
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    private void waitSchedueDisable( BSONObject cond ) throws Exception {
        int interval = 200;
        int count = 0;
        for ( int i = 0; i < 60; i++ ) {
            Thread.sleep( interval );
            int j = 0;
            ScmCursor< ScmTaskBasicInfo > cursor = ScmSystem.Task.listTask( ssA,
                    cond );
            while ( cursor.hasNext() ) {
                cursor.getNext();
                j++;
            }
            if ( j == count ) {
                break;
            } else {
                if ( i == 59 ) {
                    throw new Exception(
                            "wait time out,disabled schedule is still runing!!!"
                                    + " scheduleId = " + scheduleId.get() );
                }
            }
            count = j;
        }
    }
}