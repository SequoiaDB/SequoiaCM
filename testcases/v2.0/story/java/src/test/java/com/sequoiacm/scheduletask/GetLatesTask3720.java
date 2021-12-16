package com.sequoiacm.scheduletask;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
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
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Description SCM-3720:getLatesTask接口获取Schedule最新创建的多个Task
 * @Author zhangyanan
 * @Date 2021.8.28
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.8.28
 * @version 1.00
 */
public class GetLatesTask3720 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file3720";
    private String region;
    private String zone;
    private SiteWrapper branchSite;
    private SiteWrapper rootSite;
    private ScmSession rootSiteSession;
    private ScmSession branchSiteSession;
    private ScmWorkspace branchSiteWorkspace;
    private List< ScmId > scheduleIds = new ArrayList<>();
    private BSONObject queryCond;
    private WsWrapper wsp = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private List< ScmTask > tasks = new ArrayList<>();
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
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

        branchSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        branchSiteSession = TestScmTools.createSession( branchSite );
        rootSiteSession = TestScmTools.createSession( rootSite );
        branchSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        ScmId scmId = ScmFileUtils.create( branchSiteWorkspace, fileName,
                filePath );
        fileIds.add( scmId );
    }

    @Test(groups = { "twoSite", "fourSite", "star" })
    public void test() throws Exception {
        ScmSchedule copySchedule = ScmScheduleUtils.createCopySchedule(
                branchSiteSession, branchSite, rootSite, wsp, queryCond, region,
                zone );
        scheduleIds.add( copySchedule.getId() );
        ScmScheduleUtils.waitForTask( copySchedule, 10 );
        copySchedule.disable();
        ScmId taskId = copySchedule.getLatestTask().getId();
        ScmTaskUtils.waitTaskFinish( branchSiteSession, taskId );
        tasks = copySchedule.getTasks( new BasicBSONObject(),
                new BasicBSONObject(), 0, -1 );

        int count = 5;
        List< ScmTask > latestTasks1 = copySchedule.getLatestTasks( count );
        Assert.assertEquals( latestTasks1.size(), count );
        Date time1 = latestTasks1.get( 0 ).getStartTime();
        for ( int i = 0; i < tasks.size() - count; i++ ) {
            if ( ( time1.compareTo( tasks.get( i ).getStartTime() ) < 0 ) ) {
                throw new Exception(
                        "the latesTask starttime not biggest,the latesTaskInfo="
                                + latestTasks1 + "actTaskInfo=" + tasks );
            }
        }

        count = tasks.size();
        List< ScmTask > latestTasks2 = copySchedule.getLatestTasks( count );
        Assert.assertEquals( latestTasks2.size(), count );

        count = tasks.size() + 1;
        List< ScmTask > latestTasks3 = copySchedule.getLatestTasks( count );
        Assert.assertEquals( latestTasks3.size(), tasks.size() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
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