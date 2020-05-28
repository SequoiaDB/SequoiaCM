package com.sequoiacm.scheduletask.concurrent;

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
 * @Testcase: SCM-1268:并发删除、更新不同调度任务
 * @author huangxiaoni init
 * @date 2018.4.24
 */

public class DelAndUpSche1268 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( DelAndUpSche1268.class );
    private final static int fileNum = 10;
    private final static int fileSize = 100;
    private final static String name = "schetask1268";
    private boolean runSuccess = false;
    private ScmSession ssA = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsB = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private List< WsWrapper > wss = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;
    private ScmScheduleCleanFileContent newContent = null;

    private List< ScmId > scheIds = new ArrayList<>();

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
            rootSite = ScmInfo.getRootSite();
            branSite = ScmInfo.getBranchSite();
            wss = ScmInfo.getWss( 2 );
            ssA = TestScmTools.createSession( branSite );
            wsA = ScmFactory.Workspace.getWorkspace( wss.get( 0 ).getName(),
                    ssA );
            wsB = ScmFactory.Workspace.getWorkspace( wss.get( 1 ).getName(),
                    ssA );

            // clean environment
            queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( name ).get();
            ScmFileUtils.cleanFile( wss.get( 0 ), queryCond );
            ScmFileUtils.cleanFile( wss.get( 1 ), queryCond );

            // ready scmFile and scheTask
            this.readyScmFile( wsA, 0, fileNum / 2 );
            this.readyScmFile( wsB, fileNum / 2, fileNum );

            this.createSchedule( wss.get( 0 ).getName() );
            this.createSchedule( wss.get( 1 ).getName() );
            for ( ScmId scheduleId : scheIds ) {
                ScmScheduleUtils.isRunningOfSche( ssA, scheduleId,
                        CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
            }

            // scheduleServer host time is not sync with SCM host time, cause
            // may not match scmFile
            // ScmScheduleUtils.sleepStrategy(ssA, wsA, scheIds.get(0),
            // fileIds.get((fileNum / 2) -1), fileNum / 2);
            // ScmScheduleUtils.sleepStrategy(ssA, wsB, scheIds.get(1),
            // fileIds.get(fileNum -1), fileNum / 2);
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        DeleteSchedule delSche = new DeleteSchedule( scheIds.get( 0 ) );
        delSche.start();

        UpdateSchedule upSche = new UpdateSchedule( scheIds.get( 1 ) );
        upSche.start();

        if ( !( delSche.isSuccess() && upSche.isSuccess() ) ) {
            Assert.fail( delSche.getErrorMsg() + upSche.getErrorMsg() );
        }

        // for updateSchedule, wait task running
        ScmScheduleUtils.isRunningOfSche( ssA, scheIds.get( 1 ),
                CommonDefine.TaskType.SCM_TASK_CLEAN_FILE );

        // check results
        SiteWrapper[] expSites = { rootSite, branSite };
        ScmScheduleUtils.checkScmFile( wsA, fileIds, 0, fileNum / 2, expSites );

        try {
            ScmSystem.Schedule.get( ssA, scheIds.get( 0 ) );
            Assert.fail( "expect fail but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "delete not exist schedule in test." );
        }

        SiteWrapper[] expSites2 = { rootSite };
        ScmScheduleUtils.checkScmFile( wsB, fileIds, fileNum / 2, fileNum,
                expSites2 );

        ScmSchedule sche = ScmSystem.Schedule.get( ssA, scheIds.get( 1 ) );
        Assert.assertEquals( sche.getContent(), newContent );

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( !runSuccess ) {
            for ( ScmId scheduleId : scheIds ) {
                ScmScheduleUtils.outputTaskInfo( ssA, scheduleId );
            }
            ScmScheduleUtils.outputScmfileInfo( wsA,
                    fileIds.subList( 0, fileNum / 2 ) );
            ScmScheduleUtils.outputScmfileInfo( wsB,
                    fileIds.subList( fileNum / 2, fileNum ) );
        }
        for ( ScmId scheduleId : scheIds ) {
            if ( null != scheduleId ) {
                try {
                    ScmSystem.Schedule.delete( ssA, scheduleId );
                } catch ( ScmException e ) {
                    logger.info( "delete not exist schedule in teardown." );
                }
                if ( runSuccess || forceClear ) {
                    ScmScheduleUtils.cleanTask( ssA, scheduleId );
                }
            }
        }

        try {
            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( wss.get( 0 ), queryCond );
                ScmFileUtils.cleanFile( wss.get( 1 ), queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( ssA != null ) {
                ssA.close();
            }

        }
    }

    private void createSchedule( String wsName ) throws ScmException {
        String maxStayTime = "0d";
        ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
                branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                queryCond );
        String cron = "* * * * * ?";
        ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsName,
                ScheduleType.COPY_FILE, name, "", content, cron );
        ScmId scheduleId = sche.getId();
        scheIds.add( scheduleId );
    }

    private void readyScmFile( ScmWorkspace ws, int startNum, int endNum )
            throws ScmException, ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.set( Calendar.HOUR, calendar.get( Calendar.HOUR ) - 3 );
        for ( int i = startNum; i < endNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePath );
            file.setFileName( name + "_" + i );
            file.setAuthor( name );
            file.setCreateTime( calendar.getTime() );
            file.setTitle( "" + i );
            ScmId fileId = file.save();
            fileIds.add( fileId );
        }
    }

    private class DeleteSchedule extends TestThreadBase {
        private ScmId scheduleId = null;

        public DeleteSchedule( ScmId scheduleId ) {
            this.scheduleId = scheduleId;
        }

        @Override
        public void exec() throws ScmException {
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

    private class UpdateSchedule extends TestThreadBase {
        private ScmId scheduleId = null;

        public UpdateSchedule( ScmId scheduleId ) {
            this.scheduleId = scheduleId;
        }

        @Override
        public void exec() throws ScmException {
            ScmSession session = null;
            try {
                ScmSchedule sche = ScmSystem.Schedule.get( ssA, scheduleId );

                ScheduleType taskType = ScheduleType.CLEAN_FILE;
                String maxStayTime = "0d";
                newContent = new ScmScheduleCleanFileContent(
                        branSite.getSiteName(), maxStayTime, queryCond );
                sche.updateSchedule( taskType, newContent );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
