package com.sequoiacm.schedulePreferredZone;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateSchedule3696 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String fileName = "file3696";
    private String region;
    private String zone;
    private WsWrapper wsp = null;
    private ScmSession rootStieSession;
    private ScmSession branchSite1Session;
    private ScmSession branchSite2Session;
    private SiteWrapper rootStie;
    private SiteWrapper branchSite1;
    private SiteWrapper branchSite2;
    private ScmWorkspace rootStieWs;
    private ScmWorkspace branchSite1Ws;
    private ScmWorkspace branchSite2Ws;
    private List< ScmId > fileIds = new ArrayList<>();
    private List< ScmId > scheduleIds = new ArrayList<>();
    private BSONObject queryCond;
    private ScmSession session;
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
        rootStie = ScmInfo.getRootSite();
        List< SiteWrapper > sortBranchSites = ScmScheduleUtils
                .getSortBranchSites();
        branchSite1 = sortBranchSites.get( 0 );
        branchSite2 = sortBranchSites.get( 1 );
        rootStieSession = TestScmTools.createSession( rootStie );
        branchSite1Session = TestScmTools.createSession( branchSite1 );
        branchSite2Session = TestScmTools.createSession( branchSite2 );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        rootStieWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootStieSession );
        branchSite1Ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite1Session );
        branchSite2Ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite2Session );

        ScmId fileId = ScmFileUtils.create( branchSite1Ws, fileName, filePath );
        fileIds.add( fileId );
        ScmFile file = ScmFactory.File.getInstance( branchSite2Ws, fileId );
        file.getContent( localPath + File.separator + "cache.txt" );

        ScmConfigOption scmConfigOption = new ScmConfigOption(
                gateWayList.get( 0 ) + "/" + rootSiteServiceName, region, zone,
                "admin", "admin", null );
        session = ScmFactory.Session.createSession( scmConfigOption );
    }

    @Test
    public void test() throws Exception {
        String maxStayTime = "0d";
        // 迁移任务
        String scheduleName = "testCopy" + fileName;
        ScmScheduleCopyFileContent copyContent = new ScmScheduleCopyFileContent(
                branchSite2.getSiteName(), rootStie.getSiteName(), maxStayTime,
                queryCond );
        ScmSchedule copySchedule = ScmSystem.Schedule.create( session,
                wsp.getName(), ScheduleType.COPY_FILE, scheduleName, "",
                copyContent, "* * * * * ?" );
        scheduleIds.add( copySchedule.getId() );

        Assert.assertEquals( copySchedule.getPreferredRegion(), region );
        Assert.assertEquals( copySchedule.getPreferredZone(), zone );
        SiteWrapper[] expCopySites = { rootStie, branchSite1, branchSite2 };
        ScmScheduleUtils.checkScmFile( rootStieWs, fileIds, expCopySites );
        List< ScmTask > tasks = ScmScheduleUtils
                .getSuccessTasks( copySchedule );
        ScmScheduleUtils.checkNodeRegionAndZone( tasks, session, region, zone );

        // 清理任务
        scheduleName = "testClean" + fileName;
        ScmScheduleCleanFileContent cleanContent = new ScmScheduleCleanFileContent(
                branchSite1.getSiteName(), maxStayTime, queryCond );
        ScmSchedule cleanSchedule = ScmSystem.Schedule.create( session,
                wsp.getName(), ScheduleType.CLEAN_FILE, scheduleName, "",
                cleanContent, "* * * * * ?" );
        scheduleIds.add( cleanSchedule.getId() );

        Assert.assertEquals( cleanSchedule.getPreferredRegion(), region );
        Assert.assertEquals( cleanSchedule.getPreferredZone(), zone );
        SiteWrapper[] expSites1 = { rootStie, branchSite2 };
        ScmScheduleUtils.checkScmFile( rootStieWs, fileIds, expSites1 );
        tasks = ScmScheduleUtils.getSuccessTasks( cleanSchedule );
        ScmScheduleUtils.checkNodeRegionAndZone( tasks, rootStieSession, region,
                zone );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( ScmId schedule : scheduleIds ) {
                    ScmSystem.Schedule.delete( rootStieSession, schedule );
                    ScmScheduleUtils.cleanTask( rootStieSession, schedule );
                }
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                session.close();
                rootStieSession.close();
                branchSite1Session.close();
                branchSite2Session.close();
            }
        }
    }

}