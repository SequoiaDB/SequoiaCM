package com.sequoiacm.scheduletask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
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
 * @Description SCM-3723:listTask接口指定skip参数获取task信息
 * @Author zhangyanan
 * @Date 2021.8.28
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.8.28
 * @version 1.00
 */
public class ListTask3723 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file3723";
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

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ScmSchedule copySchedule = ScmScheduleUtils.createCopySchedule(
                branchSiteSession, branchSite, rootSite, wsp, queryCond, region,
                zone );
        scheduleIds.add( copySchedule.getId() );
        ScmScheduleUtils.waitForTask( copySchedule, 10 );
        copySchedule.disable();
        ScmId taskId = copySchedule.getLatestTask().getId();
        ScmTaskUtils.waitTaskFinish( branchSiteSession, taskId );
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.Task.SCHEDULE_ID )
                .is( copySchedule.getId().get() ).get();
        BSONObject orderby = ScmQueryBuilder.start( ScmAttributeName.Task.ID )
                .is( 1 ).get();
        int limit = -1;
        int skip = 0;
        ScmCursor< ScmTaskBasicInfo > expTasksInfo = ScmSystem.Task
                .listTask( rootSiteSession, condition, orderby, skip, limit );
        List< ScmTaskBasicInfo > expTasksInfoList = new ArrayList<>();
        while ( expTasksInfo.hasNext() ) {
            expTasksInfoList.add( expTasksInfo.getNext() );
        }

        skip = 1;
        ScmCursor< ScmTaskBasicInfo > actTasksInfo1 = ScmSystem.Task
                .listTask( rootSiteSession, condition, orderby, skip, limit );
        List< ScmTaskBasicInfo > actTasksInfoList1 = new ArrayList<>();
        while ( actTasksInfo1.hasNext() ) {
            actTasksInfoList1.add( actTasksInfo1.getNext() );
        }
        List< ScmTaskBasicInfo > expTasksInfoList1 = expTasksInfoList
                .subList( skip, expTasksInfoList.size() );
        Assert.assertEquals( expTasksInfoList1.toString(),
                actTasksInfoList1.toString() );

        skip = 5;
        ScmCursor< ScmTaskBasicInfo > actTasksInfo2 = ScmSystem.Task
                .listTask( rootSiteSession, condition, orderby, skip, limit );
        List< ScmTaskBasicInfo > actTasksInfoList2 = new ArrayList<>();
        while ( actTasksInfo2.hasNext() ) {
            actTasksInfoList2.add( actTasksInfo2.getNext() );
        }
        List< ScmTaskBasicInfo > expTasksInfoList2 = expTasksInfoList
                .subList( skip, expTasksInfoList.size() );
        Assert.assertEquals( expTasksInfoList2.toString(),
                actTasksInfoList2.toString() );

        skip = expTasksInfoList.size();
        ScmCursor< ScmTaskBasicInfo > actTasksInfo3 = ScmSystem.Task
                .listTask( rootSiteSession, condition, orderby, skip, limit );
        List< ScmTaskBasicInfo > actTasksInfoList3 = new ArrayList<>();
        while ( actTasksInfo3.hasNext() ) {
            actTasksInfoList3.add( actTasksInfo3.getNext() );
        }
        List< ScmTaskBasicInfo > expTasksInfoList3 = new ArrayList<>();
        Assert.assertEquals( expTasksInfoList3.toString(),
                actTasksInfoList3.toString() );

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
