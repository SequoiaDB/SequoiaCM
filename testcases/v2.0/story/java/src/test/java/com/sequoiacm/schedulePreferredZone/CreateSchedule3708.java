package com.sequoiacm.schedulePreferredZone;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-3708:创建相同调度任务指定相同region和zone
 * @author YiPan
 * @createDate 2021.8.26
 * @updateUser YiPan
 * @updateDate 2021.8.26
 * @updateRemark
 * @version v1.0
 */
public class CreateSchedule3708 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String fileName = "file3708";
    private String region;
    private String zone;
    private WsWrapper wsp = null;
    private ScmSession sourceSiteSession;
    private ScmSession targetSiteSession;
    private ScmWorkspace sourceSiteWs;
    private SiteWrapper sourceSite;
    private SiteWrapper targetSite;
    private List< ScmId > fileIds = new ArrayList<>();
    private List< ScmSchedule > schedules = new ArrayList<>();
    private BSONObject queryCond;
    private ScmScheduleBuilder schBuilder;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        region = TestScmBase.defaultRegion;
        zone = TestScmBase.zone1;
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
        sourceSiteSession = TestScmTools.createSession( sourceSite );
        targetSiteSession = TestScmTools.createSession( targetSite );
        sourceSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                sourceSiteSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        ScmId scmId = ScmFileUtils.create( sourceSiteWs, fileName, filePath );
        fileIds.add( scmId );
        schBuilder = ScmSystem.Schedule.scheduleBuilder( sourceSiteSession );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new CreateSchedule( "2 * * * * ?" ) );
        t.addWorker( new CreateSchedule( "3 * * * * ?" ) );
        t.run();
        // 迁移任务
        SiteWrapper[] expSites = { sourceSite, targetSite };
        ScmScheduleUtils.checkScmFile( sourceSiteWs, fileIds, expSites );
        int successCount = 0;
        for ( ScmSchedule schedule : schedules ) {
            List< ScmTask > tasks = ScmScheduleUtils
                    .getSuccessTasks( schedule );
            successCount = successCount + tasks.size();
            ScmScheduleUtils.checkNodeRegionAndZone( tasks, targetSiteSession,
                    region, zone );
        }
        Assert.assertEquals( successCount, 1 );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( ScmSchedule schedule : schedules ) {
                    schedule.disable();
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

    private class CreateSchedule {
        private String cron;

        public CreateSchedule( String cron ) {
            this.cron = cron;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            String maxStayTime = "0d";
            String scheduleName = "testCopy" + fileName;
            ScmScheduleCopyFileContent copyContent = new ScmScheduleCopyFileContent(
                    sourceSite.getSiteName(), targetSite.getSiteName(),
                    maxStayTime, queryCond );
            schBuilder.type( ScheduleType.COPY_FILE ).workspace( wsp.getName() )
                    .name( scheduleName ).description( "copy " + fileName )
                    .content( copyContent ).cron( cron ).enable( true )
                    .preferredRegion( region ).preferredZone( zone );
            ScmSchedule copySchedule = schBuilder.build();
            schedules.add( copySchedule );
        }
    }
}