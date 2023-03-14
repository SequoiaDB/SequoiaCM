package com.sequoiacm.schedulePreferredZone;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-3695:ScmScheduleBuilder指定region和zone创建调度任务
 * @author YiPan
 * @createDate 2021.8.26
 * @updateUser YiPan
 * @updateDate 2021.8.26
 * @updateRemark
 * @version v1.0
 */
public class CreateSchedule3695 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String fileName = "file3695";
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
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        region = TestScmBase.defaultRegion;
        zone = TestScmBase.zone2;
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
        rootStieSession = ScmSessionUtils.createSession( rootStie );
        branchSite1Session = ScmSessionUtils.createSession( branchSite1 );
        branchSite2Session = ScmSessionUtils.createSession( branchSite2 );

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
    }

    @Test
    public void test() throws Exception {
        // 迁移任务
        ScmSchedule copySchedule = ScmScheduleUtils.createCopySchedule(
                rootStieSession, branchSite1, rootStie, wsp, queryCond, region,
                zone );
        scheduleIds.add( copySchedule.getId() );

        Assert.assertEquals( copySchedule.getPreferredRegion(), region );
        Assert.assertEquals( copySchedule.getPreferredZone(), zone );
        SiteWrapper[] expCopySites = { rootStie, branchSite1, branchSite2 };
        ScmScheduleUtils.checkScmFile( rootStieWs, fileIds, expCopySites );
        copySchedule.disable();
        List< ScmTask > tasks = ScmScheduleUtils
                .getSuccessTasks( copySchedule );
        ScmScheduleUtils.checkNodeRegionAndZone( tasks, rootStieSession, region,
                zone );

        // 清理任务
        ScmSchedule cleanSchedule = ScmScheduleUtils.createCleanSchedule(
                rootStieSession, branchSite1, wsp, queryCond, region, zone );
        scheduleIds.add( cleanSchedule.getId() );

        Assert.assertEquals( cleanSchedule.getPreferredRegion(), region );
        Assert.assertEquals( cleanSchedule.getPreferredZone(), zone );
        SiteWrapper[] expCleanSites = { rootStie, branchSite2 };
        ScmScheduleUtils.checkScmFile( rootStieWs, fileIds, expCleanSites );
        cleanSchedule.disable();
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
                rootStieSession.close();
                branchSite1Session.close();
                branchSite2Session.close();
            }
        }
    }
}