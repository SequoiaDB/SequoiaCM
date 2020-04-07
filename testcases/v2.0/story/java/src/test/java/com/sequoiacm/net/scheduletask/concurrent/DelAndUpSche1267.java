package com.sequoiacm.net.scheduletask.concurrent;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @Testcase: SCM-1267:并发删除、更新同一调度任务
 * @author huangxiaoni init
 * @date 2018.4.24
 */

public class DelAndUpSche1267 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( DelAndUpSche1267.class );
    private final static int fileNum = 10;
    private final static int fileSize = 100;
    private final static String name = "schetask1267";
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
        wsp = ScmInfo.getWs();
        ssA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), ssA );

        // clean environment
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        // ready scmFile and scheduleTask
        this.readyScmFile( wsA );
        this.createScheduleTask();
        ScmScheduleUtils.isRunningOfSche( ssA, scheduleId,
                CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );

        // scheduleServer host time is not sync with SCM host time, cause may
        // not match scmFile
        //ScmScheduleUtils.sleepStrategy(ssA, wsA, scheduleId, fileIds.get
        // (fileNum - 1), fileNum);
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        DeleteSchedule delSche = new DeleteSchedule();
        delSche.start();

        UpSchedule upSche = new UpSchedule();
        upSche.start();

        if ( !( delSche.isSuccess() && upSche.isSuccess() ) ) {
            Assert.fail( delSche.getErrorMsg() + upSche.getErrorMsg() );
        }

        // check results
        SiteWrapper[] expSites = { rootSite, branSite };
        ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites );

        try {
            ScmSystem.Schedule.get( ssA, scheduleId );
            Assert.fail( "expect fail but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "delete not exist schedule in test." );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( !runSuccess ) {
                ScmScheduleUtils.outputTaskInfo( ssA, scheduleId );
                ScmScheduleUtils.outputScmfileInfo( wsA,
                        fileIds.subList( 0, fileNum ) );
            }
            try {
                ScmSystem.Schedule.delete( ssA, scheduleId );
            } catch ( ScmException e ) {
                logger.info( "delete not exist schedule in teardown." );
            }

            if ( runSuccess || forceClear ) {
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

    private void createScheduleTask() throws ScmException {
        String maxStayTime = "0d";
        ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
                branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                queryCond );
        String cron = "* * * * * ?";
        ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsp.getName(),
                ScheduleType.COPY_FILE, name, "", content, cron );
        scheduleId = sche.getId();
    }

    private void readyScmFile( ScmWorkspace ws )
            throws ScmException, ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.set( Calendar.HOUR, calendar.get( Calendar.HOUR ) - 3 );
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePath );
            file.setFileName( name + "_" + i );
            file.setAuthor( name );
            file.setCreateTime( calendar.getTime() );
            ScmId fileId = file.save();
            fileIds.add( fileId );
        }
    }

    private class DeleteSchedule extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                ScmSystem.Schedule.delete( ssA, scheduleId );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpSchedule extends TestThreadBase {
        @Override
        public void exec() throws ScmException {
            ScmSession session = null;
            try {
                ScmSchedule sche = ScmSystem.Schedule.get( ssA, scheduleId );

                ScheduleType taskType = ScheduleType.CLEAN_FILE;
                String maxStayTime = "0d";
                ScmScheduleCleanFileContent content = new
                        ScmScheduleCleanFileContent(
                        branSite.getSiteName(), maxStayTime, queryCond );
                sche.updateSchedule( taskType, content );
            } catch ( ScmException e ) {
                if ( ScmError.HTTP_NOT_FOUND != e.getError()
                        &&
                        ScmError.HTTP_INTERNAL_SERVER_ERROR != e.getError() ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
