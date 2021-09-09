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

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmScheduleBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
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
 * @Description SCM-3719:getLatesTask接口获取Schedule最新创建Task
 * @Author zhangyanan
 * @Date 2021.8.28
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.8.28
 * @version 1.00
 */
public class GetLatesTask3719 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file3719";
    private String region;
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
        rootSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        ScmId scmId = ScmFileUtils.create( branchSiteWorkspace, fileName,
                filePath );
        fileIds.add( scmId );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ScmScheduleContent copyContent = new ScmScheduleCopyFileContent(
                branchSite.getSiteName(), rootSite.getSiteName(), "0d",
                queryCond, ScopeType.SCOPE_CURRENT, 36000000 );
        ScmScheduleBuilder scheduleBuilder = ScmSystem.Schedule
                .scheduleBuilder( branchSiteSession );
        ScmSchedule copySchedule = scheduleBuilder
                .workspace( branchSiteWorkspace.getName() ).name( "迁移文件" )
                .description( "一个迁移的调度" ).type( ScheduleType.COPY_FILE )
                .content( copyContent ).cron( "0 0 23 L * ?" )
                .preferredRegion( region ).preferredZone( zone ).build();
        scheduleIds.add( copySchedule.getId() );
        Assert.assertNull( copySchedule.getLatestTask() );
        ScmSchedule copySchedule1 = ScmScheduleUtils.createCopySchedule(
                branchSiteSession, branchSite, rootSite, wsp, queryCond, region,
                zone );
        scheduleIds.add( copySchedule1.getId() );
        SiteWrapper[] expSites1 = { rootSite, branchSite };
        ScmScheduleUtils.checkScmFile( rootSiteWorkspace, fileIds, expSites1 );
        ScmScheduleUtils.waitForTask( copySchedule1, 10 );
        tasks = copySchedule1.getTasks( new BasicBSONObject(),
                new BasicBSONObject(), 0, -1 );
        Date time = copySchedule1.getLatestTask().getStartTime();
        for ( int i = 0; i < tasks.size(); i++ ) {
            if ( ( time.compareTo( tasks.get( i ).getStartTime() ) < 0 ) ) {
                throw new Exception(
                        "the task starttime not Biggest,the taskInfo="
                                + tasks );
            }
        }
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