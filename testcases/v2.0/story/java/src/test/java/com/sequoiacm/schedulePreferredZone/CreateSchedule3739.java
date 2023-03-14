package com.sequoiacm.schedulePreferredZone;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @description  SCM-3739:创建调度任务指定region和zone，并发刚更新zone
 * @author YiPan
 * @createDate 2021.8.26
 * @updateUser YiPan
 * @updateDate 2021.8.26
 * @updateRemark
 * @version v1.0
 */
public class CreateSchedule3739 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String fileName = "file3739";
    private String region;
    private String zone1;
    private String zone2;
    private WsWrapper wsp = null;
    private ScmSession sourceSiteSession;
    private ScmSession targetSiteSession;
    private ScmWorkspace sourceSiteWs;
    private SiteWrapper sourceSite;
    private SiteWrapper targetSite;
    private List< ScmId > fileIds = new ArrayList<>();
    private List< ScmSchedule > schedules = new ArrayList<>();
    private BSONObject queryCond;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        region = TestScmBase.defaultRegion;
        zone1 = TestScmBase.zone1;
        zone2 = TestScmBase.zone2;
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        tmpPath = localPath + File.separator + "tmpFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( tmpPath, fileSize / 2 );

        wsp = ScmInfo.getWs();
        sourceSite = ScmInfo.getBranchSite();
        targetSite = ScmInfo.getRootSite();
        sourceSiteSession = ScmSessionUtils.createSession( sourceSite );
        targetSiteSession = ScmSessionUtils.createSession( targetSite );
        sourceSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                sourceSiteSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test
    public void test() throws Exception {
        ScmSchedule copySchedule = ScmScheduleUtils.createCopySchedule(
                sourceSiteSession, sourceSite, targetSite, wsp, queryCond,
                region, "wrongZone" );
        schedules.add(copySchedule);

        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new UpdateScheduleZone( copySchedule, zone1 ) );
        t.addWorker( new UpdateScheduleZone( copySchedule, zone2 ) );
        t.run();
        ScmId fileId = ScmFileUtils.create( sourceSiteWs, fileName, filePath );
        fileIds.add( fileId );
        SiteWrapper[] expCleanSites = { sourceSite, targetSite };
        ScmScheduleUtils.checkScmFile( sourceSiteWs, fileIds, expCleanSites );
        List< ScmTask > successTasks = ScmScheduleUtils
                .getSuccessTasks( copySchedule );
        ScmScheduleUtils.checkNodeRegionAndZone( successTasks,
                sourceSiteSession, region, copySchedule.getPreferredZone() );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( ScmSchedule schedule : schedules ) {
                    ScmSystem.Schedule.delete( sourceSiteSession,
                            schedule.getId() );
                    ScmScheduleUtils.cleanTask( sourceSiteSession,
                            schedule.getId() );
                }
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                sourceSiteSession.close();
                targetSiteSession.close();
            }
        }
    }

    private class UpdateScheduleZone {
        private String zone;
        private ScmSchedule schedule;

        public UpdateScheduleZone( ScmSchedule schedule, String zone ) {
            this.zone = zone;
            this.schedule = schedule;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            schedule.updatePreferredZone( zone );
        }
    }
}