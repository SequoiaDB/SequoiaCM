package com.sequoiacm.net.scheduletask;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

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
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmTask;
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
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @FileName SCM-2205:创建调度任务，类型为清理,设置和更新maxExecTime
 * @Author fanyu
 * @Date 2018-09-05
 * @Version 1.00
 */

public class CreateSche_maxExecTime2205 extends TestScmBase {
    private final static int fileNum = 3;
    private final static int fileSize = 100;
    private final static String name = "UpdateSche_maxExecTime2205";
    private final static String cron = "* * * * * ?";
    private boolean runSuccess = false;
    private ScmSession ssR = null;
    private ScmWorkspace wsR = null;
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

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            // ready local file
            localPath = new File( TestScmBase.dataDirectory + File.separator
                    + TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            // get site and workspace, create session
            wsp = ScmInfo.getWs();
            List< SiteWrapper > sites = ScmNetUtils.getCleanSites( wsp );
            rootSite = sites.get( 1 );
            branSite = sites.get( 0 );
            ssR = TestScmTools.createSession( rootSite );
            wsR = ScmFactory.Workspace.getWorkspace( wsp.getName(), ssR );
            ssA = TestScmTools.createSession( branSite );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), ssA );

            // clean environment
            queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( name ).get();
            ScmFileUtils.cleanFile( wsp, queryCond );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        SiteWrapper[] expSites = { rootSite };
        try {
            // ready scmFile
            readyScmFile();
            // create schedule task, type is clean, and check
            long maxExecTime = 0L;
            this.createScheduleTask( maxExecTime );

            ScmScheduleUtils.checkScmFile( wsA, fileIds, 0, fileNum, expSites );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.Schedule.ID )
                    .is( scheduleId.get() ).and( "max_exec_time" )
                    .is( maxExecTime ).get();
            checkTaskInfo( cond, maxExecTime );

            // update maxExecTime < 0;
            maxExecTime = -1L;
            this.readyScmFile();
            updateScheMaxExecTime( maxExecTime );
            ScmScheduleUtils.checkScmFile( wsA, fileIds, fileNum, 2 * fileNum,
                    expSites );
            BSONObject cond1 = ScmQueryBuilder
                    .start( ScmAttributeName.Schedule.ID )
                    .is( scheduleId.get() ).and( "max_exec_time" )
                    .is( maxExecTime ).get();
            checkTaskInfo( cond1, maxExecTime );

            // update maxExecTime > 0;
            maxExecTime = 1000 * 60 * 5L;
            this.readyScmFile();
            updateScheMaxExecTime( maxExecTime );
            ScmScheduleUtils.checkScmFile( wsA, fileIds, 2 * fileNum,
                    3 * fileNum, expSites );
            BSONObject cond2 = ScmQueryBuilder
                    .start( ScmAttributeName.Schedule.ID )
                    .is( scheduleId.get() ).and( "max_exec_time" )
                    .is( maxExecTime ).get();
            checkTaskInfo( cond2, maxExecTime );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmSystem.Schedule.delete( ssA, scheduleId );
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                ScmScheduleUtils.cleanTask( ssA, scheduleId );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( ssR != null ) {
                ssR.close();
            }
            if ( ssA != null ) {
                ssA.close();
            }
        }
    }

    private void readyScmFile() throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = writeScmFile( wsA );
            readScmFile( wsR, fileId );
            fileIds.add( fileId );
        }
    }

    private ScmId writeScmFile( ScmWorkspace ws ) throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set( Calendar.HOUR, calendar.get( Calendar.HOUR ) - 3 );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( name + "_" + UUID.randomUUID() );
        file.setAuthor( name );
        file.setCreateTime( calendar.getTime() );
        ScmId fileId = file.save();
        return fileId;
    }

    private void readScmFile( ScmWorkspace ws, ScmId fileId ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );
    }

    private void createScheduleTask( long maxExecTime ) {
        try {
            ScheduleType taskType = ScheduleType.CLEAN_FILE;
            String maxStayTime = "0d";
            ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
                    branSite.getSiteName(), maxStayTime, queryCond );
            content.setMaxExecTime( maxExecTime );
            ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsp.getName(),
                    taskType, name, "", content, cron );
            scheduleId = sche.getId();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void updateScheMaxExecTime( long maxExecTime ) {
        ScmSession ss = null;
        String maxStayTime = "0d";
        try {
            ss = TestScmTools.createSession( branSite );

            ScmSchedule sche = ScmSystem.Schedule.get( ss, scheduleId );
            ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
                    branSite.getSiteName(), maxStayTime, queryCond );
            content.setMaxExecTime( maxExecTime );
            sche.updateContent( content );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != ss )
                ss.close();
        }
    }

    private void checkTaskInfo( BSONObject cond, long maxExecTime )
            throws ScmException {
        ScmCursor< ScmTaskBasicInfo > cursor = ScmSystem.Task.listTask( ssA,
                cond );
        try {
            while ( cursor.hasNext() ) {
                ScmTaskBasicInfo info = cursor.getNext();
                ScmTask taskInfo = ScmSystem.Task.getTask( ssA, info.getId() );
                Assert.assertEquals( taskInfo.getRunningFlag(),
                        CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
                Assert.assertEquals( taskInfo.getType(),
                        CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
                Assert.assertEquals( taskInfo.getWorkspaceName(),
                        wsp.getName() );

                Assert.assertEquals( taskInfo.getMaxExecTime(), maxExecTime );
                Assert.assertNotNull( taskInfo.getId() );
                Assert.assertNotNull( taskInfo.getStartTime() );
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }
}