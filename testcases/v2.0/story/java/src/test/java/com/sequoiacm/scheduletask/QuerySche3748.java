package com.sequoiacm.scheduletask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Description SCM-3748:过滤条件为ScmAttributeName.Task中的字段获取task信息
 * @Author zhangyanan
 * @Date 2021.9.13
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.9.13
 * @version 1.00
 */
public class QuerySche3748 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file3748";
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

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
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

    @Test(groups = { "twoSite", "fourSite", "star", GroupTags.base })
    public void test() throws Exception {
        ScmSchedule copySchedule = createCopySchedule( branchSiteSession,
                branchSite, rootSite, wsp, queryCond );
        scheduleIds.add( copySchedule.getId() );
        ScmScheduleUtils.waitForTask( copySchedule, 3 );
        copySchedule.disable();
        ScmId taskId = copySchedule.getLatestTask().getId();
        ScmTaskUtils.waitTaskFinish( branchSiteSession, taskId );
        BSONObject condition = null;
        int skip = 0;
        int limit = 1;
        BSONObject orderby = ScmQueryBuilder.start( ScmAttributeName.Task.ID )
                .is( 1 ).get();
        List< ScmTask > expTasks = copySchedule.getTasks( condition, orderby,
                skip, limit );

        condition = ScmQueryBuilder
                .start( ScmAttributeName.Task.ESTIMATE_COUNT )
                .is( expTasks.get( 0 ).getEstimateCount() ).get();
        List< ScmTask > actTasks2 = copySchedule.getTasks( condition, orderby,
                skip, limit );
        Assert.assertEquals( actTasks2.toString(),
                expTasks.subList( 0, 1 ).toString() );

        condition = ScmQueryBuilder.start( ScmAttributeName.Task.ACTUAL_COUNT )
                .is( expTasks.get( 0 ).getActualCount() ).get();
        List< ScmTask > actTasks3 = copySchedule.getTasks( condition, orderby,
                skip, limit );
        Assert.assertEquals( actTasks3.toString(),
                expTasks.subList( 0, 1 ).toString() );

        condition = ScmQueryBuilder.start( ScmAttributeName.Task.SUCCESS_COUNT )
                .is( expTasks.get( 0 ).getSuccessCount() ).get();
        List< ScmTask > actTasks4 = copySchedule.getTasks( condition, orderby,
                skip, limit );
        Assert.assertEquals( actTasks4.toString(),
                expTasks.subList( 0, 1 ).toString() );

        condition = ScmQueryBuilder.start( ScmAttributeName.Task.FAIL_COUNT )
                .is( expTasks.get( 0 ).getFailCount() ).get();
        List< ScmTask > actTasks5 = copySchedule.getTasks( condition, orderby,
                skip, limit );
        Assert.assertEquals( actTasks5.toString(),
                expTasks.subList( 0, 1 ).toString() );

        condition = ScmQueryBuilder.start( ScmAttributeName.Task.MAX_EXEC_TIME )
                .is( expTasks.get( 0 ).getMaxExecTime() ).get();
        List< ScmTask > actTasks6 = copySchedule.getTasks( condition, orderby,
                skip, limit );
        Assert.assertEquals( actTasks6.toString(),
                expTasks.subList( 0, 1 ).toString() );

        condition = ScmQueryBuilder.start( ScmAttributeName.Task.SCOPE ).is( 1 )
                .get();
        List< ScmTask > actTasks7 = copySchedule.getTasks( condition, orderby,
                skip, limit );
        Assert.assertEquals( actTasks7.toString(),
                expTasks.subList( 0, 1 ).toString() );

        condition = ScmQueryBuilder.start( ScmAttributeName.Task.TARGET_SITE )
                .is( 1 ).get();
        List< ScmTask > actTasks8 = copySchedule.getTasks( condition, orderby,
                skip, limit );
        Assert.assertEquals( actTasks8.toString(),
                expTasks.subList( 0, 1 ).toString() );

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

    public ScmSchedule createCopySchedule( ScmSession session,
            SiteWrapper sourceSite, SiteWrapper targetSite, WsWrapper wsp,
            BSONObject cond ) throws ScmException {
        UUID uuid = UUID.randomUUID();
        String maxStayTime = "0d";
        String scheduleName = "testCopy" + uuid;
        String description = "copy " + uuid;
        ScmScheduleBuilder schBuilder = ScmSystem.Schedule
                .scheduleBuilder( session );
        ScmScheduleCopyFileContent copyContent = new ScmScheduleCopyFileContent(
                sourceSite.getSiteName(), targetSite.getSiteName(), maxStayTime,
                cond );
        schBuilder.type( ScheduleType.COPY_FILE ).workspace( wsp.getName() )
                .name( scheduleName ).description( description )
                .content( copyContent ).cron( "* * * * * ?" ).enable( true );
        return schBuilder.build();
    }
}
