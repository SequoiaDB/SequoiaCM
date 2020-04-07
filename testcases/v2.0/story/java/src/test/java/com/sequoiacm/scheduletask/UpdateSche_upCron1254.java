package com.sequoiacm.scheduletask;

import java.io.File;
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
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @FileName SCM-1254:更新cron
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class UpdateSche_upCron1254 extends TestScmBase {
    private final static int fileNum = 10;
    private final static int fileSize = 100;
    private final static String name = "sche1254";
    private final static String maxStayTime = "0d";
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
    private void setUp() {
        try {
            // ready local file
            localPath = new File( TestScmBase.dataDirectory + File.separator +
                    TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize +
                    ".txt";
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
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            // ready scmFile
            this.writeScmFile( 0, fileNum / 2 );
            // create schedule task, type is copy, and check
            String cron = "* * * * * ?";
            this.createScheduleTask( cron );
            SiteWrapper[] expSites1 = { rootSite, branSite };
            ScmScheduleUtils
                    .checkScmFile( wsA, fileIds, 0, fileNum / 2, expSites1 );

            // ready scmFile again
            this.writeScmFile( fileNum / 2, fileNum );
            // update schedule content[extra_condition]
            cron = "*/10 * * * * ?";
            this.updateSheMaxStayTime( cron );
            Thread.sleep( 1000 );
            for ( int i = 0; i < 30; i++ ) {
                Thread.sleep( 1000 );
                int tranNum = this.getTranferTaskNum();
                Thread.sleep( 1000 );
                int tranNum2 = this.getTranferTaskNum();
                if ( tranNum2 != tranNum && i == 29 ) {
                    printTaskInfo();
                    Assert.fail( "update failed, " + "tranNum = " + tranNum +
                            ", tranNum2 = " + tranNum2
                            + ", scheduleId = " + scheduleId.get() );
                } else {
                    System.out.println( "i = " + i );
                    break;
                }
            }

            int tranNum3 = this.getTranferTaskNum();
            Thread.sleep( 3000 );
            int maxRetryTimes = 300;  // 5min
            int retryTimes = 0;
            while ( true ) {
                int tranNum4 = this.getTranferTaskNum();
                if ( tranNum4 > tranNum3 ) {
                    break;
                } else if ( retryTimes == maxRetryTimes ) {
                    throw new Exception( "update failed, "
                            + "tranNum4 = " + tranNum4 + ", tranNum3 = " +
                            tranNum3 + ", tranNum3 = " + tranNum3 );
                }
                retryTimes++;
                Thread.sleep( 1000 );
            }
            // check results
            SiteWrapper[] expSites2 = { rootSite, branSite };
            ScmScheduleUtils
                    .checkScmFile( wsA, fileIds, 0, fileNum, expSites2 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            printTaskInfo();
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmSystem.Schedule.delete( ssA, scheduleId );
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                ScmScheduleUtils.cleanTask( ssA, scheduleId );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( ssA != null ) {
                ssA.close();
            }
        }
    }

    private void writeScmFile( int startNum, int endNum ) throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set( Calendar.HOUR, calendar.get( Calendar.HOUR ) - 3 );
        for ( int i = startNum; i < endNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setContent( filePath );
            file.setFileName( name + "_" + i );
            file.setAuthor( name );
            file.setCreateTime( calendar.getTime() );
            ScmId fileId = file.save();
            fileIds.add( fileId );
        }
    }

    private void createScheduleTask( String cron ) {
        try {
            ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
                    branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                    queryCond );
            ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsp.getName(),
                    ScheduleType.COPY_FILE, name, "", content, cron );
            scheduleId = sche.getId();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void updateSheMaxStayTime( String cron ) {
        try {
            ScmSchedule sche = ScmSystem.Schedule.get( ssA, scheduleId );
            sche.updateCron( cron );

            // check shedule info
            ScmSchedule sche2 = ScmSystem.Schedule.get( ssA, scheduleId );
            Assert.assertEquals( sche2.getId(), scheduleId );
            Assert.assertEquals( sche2.getCron(), cron );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private int getTranferTaskNum() throws Exception {
        int transferNum = 0;
        ScmCursor< ScmTaskBasicInfo > cursor = null;
        try {
            BSONObject cond = ScmQueryBuilder.start( "schedule_id" )
                    .is( scheduleId.get() ).get();
            cursor = ScmSystem.Task.listTask( ssA, cond );
            while ( cursor.hasNext() ) {
                cursor.getNext();
                transferNum++;
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != cursor ) cursor.close();
        }
        return transferNum;
    }

    private void printTaskInfo() {
        ScmCursor< ScmTaskBasicInfo > cursor = null;
        try {
            BSONObject cond = ScmQueryBuilder.start( "schedule_id" )
                    .is( scheduleId.get() ).get();
            cursor = ScmSystem.Task.listTask( ssA, cond );
            while ( cursor.hasNext() ) {
                ScmTaskBasicInfo info = cursor.getNext();
                System.out.println( "info = " + info.toString() );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }
}