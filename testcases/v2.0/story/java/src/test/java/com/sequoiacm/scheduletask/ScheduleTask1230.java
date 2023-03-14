package com.sequoiacm.scheduletask;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-1230:创建调度任务，类型为清理，清理主站点文件后再次清理分站点相同文件
 * @Author YiPan
 * @CreateDate 2021/9/8
 * @Version 1.0
 */
public class ScheduleTask1230 extends TestScmBase {
    private final static int fileSize = 1024;
    private final static String filename = "file1230";
    private ScmSession branchSiteASession = null;
    private ScmSession branchSiteBSession = null;
    private ScmWorkspace rootSiteWs = null;
    private ScmWorkspace branchSiteAWs = null;
    private ScmWorkspace branchSiteBWs = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchASite = null;
    private SiteWrapper branchBSite = null;
    private WsWrapper wsp = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;
    private BSONObject queryCond = null;
    private List< ScmSchedule > scmSchedules = new ArrayList<>();
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        downloadPath = localPath + File.separator + "download_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        List< SiteWrapper > branchSites = ScmInfo.getBranchSites( 2 );
        branchASite = branchSites.get( 0 );
        branchBSite = branchSites.get( 1 );
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        branchSiteASession = ScmSessionUtils.createSession( branchASite );
        branchSiteBSession = ScmSessionUtils.createSession( branchBSite );
        branchSiteAWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteASession );
        branchSiteBWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteBSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( filename ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "fourSite", GroupTags.base })
    public void test() throws Exception {
        // 主站点创建文件,缓存至分站点A、B
        ScmId fileId = ScmFileUtils.create( branchSiteAWs, filename, filePath );
        ScmFile instance = ScmFactory.File.getInstance( branchSiteBWs, fileId );
        instance.getContent( downloadPath );

        // 创建主站点清理任务
        ScmSchedule cleanRootSite = ScmScheduleUtils.createCleanSchedule(
                branchSiteASession, rootSite, wsp, queryCond );
        scmSchedules.add( cleanRootSite );
        SiteWrapper[] expSites1 = { branchASite, branchBSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSites1 );
        cleanRootSite.disable();

        // 分站点A创建清理任务
        ScmSchedule cleanbranchSite = ScmScheduleUtils.createCleanSchedule(
                branchSiteASession, branchASite, wsp, queryCond );
        scmSchedules.add( cleanbranchSite );
        SiteWrapper[] expSites2 = { branchBSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSites2 );
        cleanbranchSite.disable();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmSchedule schedule : scmSchedules ) {
                    ScmSystem.Schedule.delete( branchSiteASession,
                            schedule.getId() );
                    ScmFileUtils.cleanFile( wsp, queryCond );
                    TestTools.LocalFile.removeFile( localPath );
                    ScmScheduleUtils.cleanTask( branchSiteASession,
                            schedule.getId() );
                }
            }
        } finally {
            branchSiteASession.close();
            branchSiteBSession.close();
        }
    }
}