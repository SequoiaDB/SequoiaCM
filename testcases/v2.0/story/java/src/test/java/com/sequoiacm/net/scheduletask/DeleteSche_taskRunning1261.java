package com.sequoiacm.net.scheduletask;

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
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @FileName SCM-1261:删除执行中的任务
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class DeleteSche_taskRunning1261 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( DeleteSche_taskRunning1261.class );
    private final static int fileNum = 10;
    private final static int fileSize = 100;
    private final static String name = "schetask1261";
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
        // ready scmFile
        this.readyScmFile( wsA, 0, fileNum );

        // ready schedule
        this.createScheduleTask();
        //to wait for the task to run
        ScmScheduleUtils.isRunningOfSche( ssA, scheduleId );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmSystem.Schedule.delete( ssA, scheduleId );

        SiteWrapper[] expSites1 = { rootSite, branSite };
        ScmScheduleUtils.checkScmFile( wsA, fileIds, 0, fileNum, expSites1 );

        //the schedule was deleted, task is not running again
        this.readyScmFile( wsA, fileNum, fileNum + 10 );
        SiteWrapper[] expSites2 = { branSite };
        ScmScheduleUtils
                .checkScmFile( wsA, fileIds, fileNum, fileNum + 10, expSites2 );

        try {
            //check shedule was deleted
            ScmSystem.Schedule.get( ssA, scheduleId );
            Assert.fail( "expect fail but actual succ." );
        } catch ( ScmException e ) {
            if ( ScmError.HTTP_NOT_FOUND != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
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
            if ( null != scheduleId ) {
                try {
                    ScmSystem.Schedule.delete( ssA, scheduleId );
                } catch ( ScmException e ) {
                    logger.info( "delete not exist schedule in teardown." );
                }
            }

            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                ScmScheduleUtils.cleanTask( ssA, scheduleId );
            }
        } catch ( ScmException e ) {
            if ( ScmError.HTTP_NOT_FOUND != e.getError() ) {
                throw e;
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
            file.setAuthor( name );
            file.setCreateTime( calendar.getTime() );
            ScmId fileId = file.save();
            fileIds.add( fileId );
        }
    }

    private void createScheduleTask() throws ScmException {
        try {
            String maxStayTime = "0d";
            content = new ScmScheduleCopyFileContent(
                    branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                    queryCond );
            System.out.println( content.toBSONObject() );
            cron = "* * * * * ?";
            ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsp.getName(),
                    ScheduleType.COPY_FILE, name, "", content, cron );
            scheduleId = sche.getId();
        } catch ( ScmException e ) {
            e.printStackTrace();
            throw e;
        }
    }
}