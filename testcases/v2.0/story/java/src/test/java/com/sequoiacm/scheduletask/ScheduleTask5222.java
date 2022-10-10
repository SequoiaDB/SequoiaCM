package com.sequoiacm.scheduletask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Description SCM-5222:创建迁移并清理任务，设置弱一致检测
 * @Author zhangyanan
 * @Date 2022.09.21
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2022.09.21
 * @version 1.00
 */
public class ScheduleTask5222 extends TestScmBase {
    private final static int fileSize = 1024;
    private final static String filename = "file5222";
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private SiteWrapper rootSite = null;
    private ScmSession branSiteSession = null;
    private ScmWorkspace branSiteWs = null;
    private SiteWrapper branchSite = null;
    private WsWrapper wsp = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private String tmpSameSizePath = null;
    private String tmpDiffSizePath = null;
    private BSONObject queryCond = null;
    private ScmSchedule scmSchedule;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        tmpSameSizePath = localPath + File.separator + "tmpFile_" + fileSize
                + ".txt";
        tmpDiffSizePath = localPath + File.separator + "tmpFile_" + fileSize + 1
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( tmpSameSizePath, fileSize );
        TestTools.LocalFile.createFile( tmpDiffSizePath, fileSize + 1 );

        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );

        branSiteSession = TestScmTools.createSession( branchSite );
        branSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( filename ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        prepareDate();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 创建迁移清理任务,目标站点于源站点文件大小相同，lob相同
        testMoveSchedule1();
        // 创建迁移清理任务,目标站点于源站点文件大小相同，lob不同
        testMoveSchedule2();
        // 创建迁移清理任务,目标站点于源站点文件大小不同
        testMoveSchedule3();

        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( rootSiteSession != null ) {
                rootSiteSession.close();
            }
            if ( branSiteSession != null ) {
                branSiteSession.close();
            }
        }
    }

    public void prepareDate() throws Exception {
        // 主站点创建文件后缓存至分站点
        ScmId fileId = ScmFileUtils.create( rootSiteWs, filename, filePath );
        fileIdList.add( fileId );
        ScmFactory.File.asyncCache( branSiteWs, fileId );
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileId, 2 );
    }

    public void testMoveSchedule1() throws Exception {
        // 创建迁移清理任务
        scmSchedule = ScmScheduleUtils.createMoveSchedule( rootSiteSession,
                rootSite, branchSite, wsp.getName(), queryCond,
                ScmDataCheckLevel.WEEK, false, false );
        ScmScheduleUtils.waitForTask( scmSchedule, 2 );

        SiteWrapper[] expSites1 = { branchSite };
        ScmScheduleUtils.checkScmFile( branSiteWs, fileIdList, expSites1 );

        // 校验统计记录
        List< ScmTask > successTasks = ScmScheduleUtils
                .getSuccessTasks( scmSchedule );
        ScmTask task = successTasks.get( 0 );
        Assert.assertEquals( task.getEstimateCount(), 1 );
        Assert.assertEquals( task.getActualCount(), 1 );
        Assert.assertEquals( task.getSuccessCount(), 1 );
        Assert.assertEquals( task.getFailCount(), 0 );
        Assert.assertEquals( task.getProgress(), 100 );

        scmSchedule.disable();
        ScmSystem.Schedule.delete( rootSiteSession, scmSchedule.getId() );
        ScmScheduleUtils.cleanTask( rootSiteSession, scmSchedule.getId() );
    }

    public void testMoveSchedule2() throws Exception {
        // 替换分站点上的数据文件lob,大小相同
        ScmFactory.File.asyncCache( rootSiteWs, fileIdList.get( 0 ) );
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileIdList.get( 0 ),
                2 );
        TestSdbTools.Lob.removeLob( branchSite, wsp, fileIdList.get( 0 ) );
        TestSdbTools.Lob.putLob( branchSite, wsp, fileIdList.get( 0 ),
                tmpSameSizePath );

        // 创建迁移清理任务
        scmSchedule = ScmScheduleUtils.createMoveSchedule( rootSiteSession,
                rootSite, branchSite, wsp.getName(), queryCond,
                ScmDataCheckLevel.WEEK, false, false );
        ScmScheduleUtils.waitForTask( scmSchedule, 2 );
        SiteWrapper[] expSites1 = { branchSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIdList, expSites1 );

        // 校验统计记录
        List< ScmTask > successTasks = ScmScheduleUtils
                .getSuccessTasks( scmSchedule );
        ScmTask task = successTasks.get( 0 );
        Assert.assertEquals( task.getEstimateCount(), 1 );
        Assert.assertEquals( task.getActualCount(), 1 );
        Assert.assertEquals( task.getSuccessCount(), 1 );
        Assert.assertEquals( task.getFailCount(), 0 );
        Assert.assertEquals( task.getProgress(), 100 );

        scmSchedule.disable();
        ScmSystem.Schedule.delete( rootSiteSession, scmSchedule.getId() );
        ScmScheduleUtils.cleanTask( rootSiteSession, scmSchedule.getId() );
    }

    public void testMoveSchedule3() throws Exception {
        // 替换分站点上的数据文件lob,大小不同
        ScmFactory.File.asyncCache( rootSiteWs, fileIdList.get( 0 ) );
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileIdList.get( 0 ),
                2 );
        TestSdbTools.Lob.removeLob( branchSite, wsp, fileIdList.get( 0 ) );
        TestSdbTools.Lob.putLob( branchSite, wsp, fileIdList.get( 0 ),
                tmpDiffSizePath );

        // 创建迁移清理任务
        scmSchedule = ScmScheduleUtils.createMoveSchedule( rootSiteSession,
                rootSite, branchSite, wsp.getName(), queryCond,
                ScmDataCheckLevel.WEEK, false, false );
        ScmScheduleUtils.waitForTask( scmSchedule, 2 );
        SiteWrapper[] expSites2 = { rootSite, branchSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIdList, expSites2 );

        // 校验统计记录
        BSONObject orderBy = ScmQueryBuilder
                .start( ScmAttributeName.Task.STOP_TIME ).is( -1 ).get();
        List< ScmTask > tasks = scmSchedule.getTasks( null, orderBy, 0, -1 );
        ScmTask task = tasks.get( 0 );
        Assert.assertEquals( task.getEstimateCount(), 1 );
        Assert.assertEquals( task.getActualCount(), 1 );
        Assert.assertEquals( task.getSuccessCount(), 0 );
        Assert.assertEquals( task.getFailCount(), 1 );
        Assert.assertEquals( task.getProgress(), 100 );

        scmSchedule.disable();
        ScmSystem.Schedule.delete( rootSiteSession, scmSchedule.getId() );
        ScmScheduleUtils.cleanTask( rootSiteSession, scmSchedule.getId() );
    }
}