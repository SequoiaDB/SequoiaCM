package com.sequoiacm.schedulePreferredZone;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-3706:创建调度任务，指定region和zone中符合条件节点故障
 * @author YiPan
 * @createDate 2021.8.26
 * @updateUser YiPan
 * @updateDate 2021.8.26
 * @updateRemark
 * @version v1.0
 */
public class CreateSchedule3706 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String fileName = "file3706";
    private String region;
    private String zone;
    private WsWrapper wsp = null;
    private ScmSession sourceSiteSession;
    private ScmSession targetSiteSession;
    private ScmWorkspace sourceSiteWs;
    private ScmWorkspace targetSiteWs;
    private SiteWrapper sourceSite;
    private SiteWrapper targetSite;
    private List< ScmId > fileIds = new ArrayList<>();
    private List< ScmId > scheduleIds = new ArrayList<>();
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
        targetSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                targetSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        ScmId scmId = ScmFileUtils.create( sourceSiteWs, fileName, filePath );
        fileIds.add( scmId );
        ScmFile instance = ScmFactory.File.getInstance( targetSiteWs, scmId );
        instance.getContent( localPath + File.separator + "test.txt" );
        schBuilder = ScmSystem.Schedule.scheduleBuilder( sourceSiteSession );
    }

    // 星装结构可靠性用例，暂时未实现自动化，需要手工注入异常进行测试
    @Test(enabled = false)
    public void test() throws Exception {
        String maxStayTime = "0d";
        // 清理任务
        String scheduleName = "testClean" + fileName;
        ScmScheduleCleanFileContent cleanContent = new ScmScheduleCleanFileContent(
                sourceSite.getSiteName(), maxStayTime, queryCond );
        schBuilder.type( ScheduleType.CLEAN_FILE ).workspace( wsp.getName() )
                .name( scheduleName ).description( "clean " + fileName )
                .content( cleanContent ).cron( "0/1 * * * * ?" ).enable( true )
                .preferredRegion( region ).preferredZone( zone );
        ScmSchedule cleanSchedule = schBuilder.build();
        scheduleIds.add( cleanSchedule.getId() );

        for ( int i = 30; i > 0; i-- ) {
            Thread.sleep( 10000 );
            System.out.println( "创建任务后：" + i * 10 + "s" );
        }

        Assert.assertEquals( cleanSchedule.getPreferredRegion(), region );
        Assert.assertEquals( cleanSchedule.getPreferredZone(), zone );

        SiteWrapper[] expSites = { targetSite };
        ScmScheduleUtils.checkScmFile( targetSiteWs, fileIds, expSites );

        BSONObject bson = new BasicBSONObject();
        bson.put( "schedule_id", cleanSchedule.getId().get() );
        bson.put( "actual_count", 1 );
        List< ScmTask > tasks = cleanSchedule.getTasks( bson,
                new BasicBSONObject(), 0, -1 );
        ScmScheduleUtils.checkNodeRegionAndZone( tasks, targetSiteSession,
                region, zone );
        System.err.println( "---1---" + tasks.get( 0 ).getServerId() + "---"
                + tasks.get( 0 ).getStopTime() );
        check( cleanSchedule );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( ScmId schedule : scheduleIds ) {
                    ScmSystem.Schedule.delete( sourceSiteSession, schedule );
                    ScmScheduleUtils.cleanTask( sourceSiteSession, schedule );
                }
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                sourceSiteSession.close();
                targetSiteSession.close();
            }
        }
    }

    private void check( ScmSchedule cleanSchedule ) throws Exception {
        SiteWrapper[] expSites = { targetSite };
        BSONObject bson = new BasicBSONObject();
        bson.put( "schedule_id", cleanSchedule.getId().get() );
        bson.put( "actual_count", 1 );
        ScmSession session = TestScmTools
                .createSession( ScmInfo.getBranchSite() );
        ScmWorkspace workspace = ScmFactory.Workspace
                .getWorkspace( wsp.getName(), session );
        ScmFile instance = ScmFactory.File.getInstance( workspace,
                fileIds.get( 0 ) );
        instance.getContent( localPath + File.separator + "test1.txt" );
        System.out.println( "已创建文件" );
        System.err.println( "请手动注入异常" );
        for ( int i = 35; i > 0; i-- ) {
            Thread.sleep( 10000 );
            System.out.println( "进程暂停时间剩余：" + i * 10 + "s" );
        }

        ScmScheduleUtils.checkScmFile( targetSiteWs, fileIds, expSites );
        List< ScmTask > tasks = cleanSchedule.getTasks( bson,
                new BasicBSONObject(), 0, -1 );
        ScmScheduleUtils.checkNodeRegionAndZone( tasks, targetSiteSession,
                region, zone );
        for ( int i = 0; i < tasks.size(); i++ ) {
            System.err.println( "---2---" + tasks.get( i ).getServerId() + "---"
                    + tasks.get( i ).getStopTime() );
        }
        session.close();
    }
}