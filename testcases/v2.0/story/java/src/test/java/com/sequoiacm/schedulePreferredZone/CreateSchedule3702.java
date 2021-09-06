package com.sequoiacm.schedule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @Description SCM-3702:更新调度任务region为不存在值
 * @Author zhangyanan
 * @Date 2021.8.28
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.8.28
 * @version 1.00
 */
public class CreateSchedule3702 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file3702";
    private String region;
    private String updataRegion = "region1";
    private String zone;
    private SiteWrapper branchSite;
    private SiteWrapper rootSite;
    private ScmSession rootSiteSession;
    private ScmSession branchSiteSession;
    private ScmWorkspace rootSiteWorkspace;
    private ScmWorkspace branchSiteWorkspace;
    private List< ScmId > scheduleIds = new ArrayList<>();
    private BSONObject queryCond;
    private WsWrapper wsp = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private List< ScmTask > tasks = new ArrayList<>();
    private List< ScmTask > successTasks = new ArrayList<>();
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        zone = TestScmBase.zone1;
        region = TestScmBase.defaultRegion;
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        branchSite = ScmScheduleUtils.getSortBranchSites().get( 0 );
        rootSite = ScmInfo.getRootSite();

        wsp = ScmInfo.getWs();
        branchSiteSession = TestScmTools.createSession( branchSite );
        rootSiteSession = TestScmTools.createSession( rootSite );

        branchSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteSession );
        rootSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        ScmId scmId = ScmFileUtils.create( rootSiteWorkspace, fileName,
                filePath );
        fileIds.add( scmId );
    }

    @Test
    public void test() throws Exception {
        ScmFactory.File.asyncCache( branchSiteWorkspace, fileIds.get( 0 ) );

        BSONObject copyCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( fileName ).get();
        ScmSchedule cleanSchedule = ScmScheduleUtils.createCleanSchedule(
                branchSiteSession, branchSite, wsp, copyCondition, region,
                zone );
        ScmScheduleUtils.waitForTask( cleanSchedule, 5 );
        scheduleIds.add( cleanSchedule.getId() );

        SiteWrapper[] expSites1 = { rootSite };
        ScmScheduleUtils.checkScmFile( rootSiteWorkspace, fileIds, expSites1 );

        tasks = ScmScheduleUtils.getSuccessTasks( cleanSchedule );
        ScmScheduleUtils.checkNodeRegionAndZone( tasks, branchSiteSession,
                region, zone );

        Assert.assertEquals( cleanSchedule.getPreferredRegion(), region );
        Assert.assertEquals( cleanSchedule.getPreferredZone(), zone );

        cleanSchedule.updatePreferredRegion( updataRegion );
        cleanSchedule.updatePreferredZone( zone );
        ScmFactory.File.asyncCache( branchSiteWorkspace, fileIds.get( 0 ) );
        ScmScheduleUtils.checkScmFile( rootSiteWorkspace, fileIds, expSites1 );

        ScmScheduleUtils.waitForTask( cleanSchedule, 10 );
        successTasks = ScmScheduleUtils.getSuccessTasks( cleanSchedule );
        List< ScmTask > lastesSuccessTasks = successTasks.subList( 0, 1 );
        ScmScheduleUtils.checkNodeRegion( lastesSuccessTasks, branchSiteSession,
                region );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( ScmId schedule : scheduleIds ) {
                    ScmSystem.Schedule.delete( branchSiteSession, schedule );
                    ScmScheduleUtils.cleanTask( branchSiteSession, schedule );
                }
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                rootSiteSession.close();
                branchSiteSession.close();
            }
        }
    }
}