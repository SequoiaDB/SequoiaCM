package com.sequoiacm.schedulePreferredZone;

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
import com.sequoiacm.client.core.ScmFile;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @Description SCM-3701:更新调度任务region和zone中存在符合条件节点
 *              SCM-3750:更新调度任务优先选择regin和zone，迁移相同文件
 * @Author zhangyanan
 * @Date 2021.8.28
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.8.28
 * @version 1.00
 */
public class CreateSchedule3701_3750 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file3701_3750";
    private String region;
    private String zone;
    private String updateZone;
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
        updateZone = TestScmBase.zone2;
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        branchSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();

        wsp = ScmInfo.getWs();
        branchSiteSession = ScmSessionUtils.createSession( branchSite );
        rootSiteSession = ScmSessionUtils.createSession( rootSite );

        branchSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteSession );
        rootSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        ScmId scmId = ScmFileUtils.create( branchSiteWorkspace, fileName,
                filePath );
        fileIds.add( scmId );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        BSONObject copyCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( fileName ).get();
        ScmSchedule copySchedule = ScmScheduleUtils.createCopySchedule(
                branchSiteSession, branchSite, rootSite, wsp, copyCondition,
                region, zone );
        scheduleIds.add( copySchedule.getId() );
        ScmScheduleUtils.waitForTask( copySchedule, 5 );
        SiteWrapper[] expSites1 = { rootSite, branchSite };
        ScmScheduleUtils.checkScmFile( rootSiteWorkspace, fileIds, expSites1 );

        tasks = ScmScheduleUtils.getSuccessTasks( copySchedule );
        ScmScheduleUtils.checkNodeRegionAndZone( tasks, rootSiteSession, region,
                zone );

        copySchedule.updatePreferredRegion( region );
        copySchedule.updatePreferredZone( updateZone );

        ScmFile file = ScmFactory.File.createInstance( branchSiteWorkspace );
        file.setFileName( fileName + "1" );
        file.setAuthor( fileName );
        file.setContent( filePath );
        fileIds.add( file.save() );

        Assert.assertEquals( copySchedule.getPreferredRegion(), region );
        Assert.assertEquals( copySchedule.getPreferredZone(), updateZone );
        ScmScheduleUtils.checkScmFile( rootSiteWorkspace, fileIds, expSites1 );

        successTasks = ScmScheduleUtils.getSuccessTasks( copySchedule );
        List< ScmTask > lastesSuccessTasks = successTasks.subList( 0, 1 );
        ScmScheduleUtils.checkNodeRegionAndZone( lastesSuccessTasks,
                rootSiteSession, region, updateZone );

        BSONObject orderBy = ScmQueryBuilder
                .start( ScmAttributeName.Task.STOP_TIME ).is( -1 ).get();
        List< ScmTask > actualTasks = copySchedule.getTasks( ScmQueryBuilder
                .start( ScmAttributeName.Task.ACTUAL_COUNT ).is( 1 ).get(),
                orderBy, 0, -1 );

        Assert.assertEquals( actualTasks.size(), 2 );
        // 校验更新后actual_count为1的taskd
        ScmScheduleUtils.checkNodeRegionAndZone( actualTasks.subList( 0, 1 ),
                rootSiteSession, region, updateZone );
        // 校验更新前actual_count为1的task
        ScmScheduleUtils.checkNodeRegionAndZone( actualTasks.subList( 1, 2 ),
                rootSiteSession, region, zone );
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