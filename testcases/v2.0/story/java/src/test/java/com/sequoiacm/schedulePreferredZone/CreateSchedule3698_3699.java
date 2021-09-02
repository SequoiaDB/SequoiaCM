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
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description SCM-3698:region中存在符合条件节点、zone中不存在符合条件节点
 *              SCM-3699:region中存在符合条件节点、zone不存在
 * @author YiPan
 * @createDate 2021.8.26
 * @updateUser YiPan
 * @updateDate 2021.8.26
 * @updateRemark
 * @version v1.0
 */
public class CreateSchedule3698_3699 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String fileName = "file3698";
    private String region;
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
    private List< ScmId > scheduleIds = new ArrayList<>();
    private List< ScmId > fileIds = new ArrayList<>();
    private ScmFile file;
    private BSONObject queryCond;
    private AtomicInteger runSuccessCount = new AtomicInteger( 0 );

    @BeforeClass
    public void setUp() throws Exception {
        region = TestScmBase.defaultRegion;
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
        file = ScmFactory.File.getInstance( rootStieWs, fileId );
        file.getContent( localPath + File.separator + "cache.txt" );
        fileIds.add( fileId );
        file = ScmFactory.File.getInstance( branchSite2Ws, fileId );
    }

    @DataProvider(name = "DataProvider")
    public Object[] Zones() {
        Object[] zone = { "zone3", "wrong" };
        return zone;
    }

    @Test(dataProvider = "DataProvider")
    public void test( String zone ) throws Exception {
        String cachefile = localPath + File.separator + "cache.txt";
        TestTools.LocalFile.removeFile( cachefile );
        file.getContent( cachefile );

        // 清理任务
        ScmSchedule cleanSchedule = ScmScheduleUtils.createCleanSchedule(
                branchSite1Session, branchSite1, wsp, queryCond, region, zone );
        scheduleIds.add( cleanSchedule.getId() );

        Assert.assertEquals( cleanSchedule.getPreferredRegion(), region );
        Assert.assertEquals( cleanSchedule.getPreferredZone(), zone );
        SiteWrapper[] expCleanSites = { rootStie, branchSite2 };
        ScmScheduleUtils.checkScmFile( branchSite1Ws, fileIds, expCleanSites );
        List< ScmTask > tasks = ScmScheduleUtils
                .getSuccessTasks( cleanSchedule );
        ScmScheduleUtils.checkNodeRegion( tasks, branchSite1Session, region );
        runSuccessCount.incrementAndGet();
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccessCount.get() == Zones().length
                || TestScmBase.forceClear ) {
            try {
                for ( ScmId schedule : scheduleIds ) {
                    ScmSystem.Schedule.delete( branchSite1Session, schedule );
                    ScmScheduleUtils.cleanTask( branchSite1Session, schedule );
                }
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                branchSite1Session.close();
                branchSite2Session.close();
            }
        }
    }
}