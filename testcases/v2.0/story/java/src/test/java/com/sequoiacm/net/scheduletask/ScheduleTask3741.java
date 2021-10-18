package com.sequoiacm.net.scheduletask;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-3741:创建清理任务，类型为网状结构清理任务，清理最后一级站点文件
 * @Author YiPan
 * @CreateDate 2021/9/8
 * @Version 1.0
 */
public class ScheduleTask3741 extends TestScmBase {
    private final static int fileSize = 1024;
    private final static String filename = "file3741";
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private WsWrapper wsp = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;
    private ScmSchedule scmSchedule;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > sortBranchSites = ScmScheduleUtils
                .getSortBranchSites();
        branchSite = sortBranchSites.get( sortBranchSites.size() - 1 );
        wsp = ScmInfo.getWs();
        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( filename ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 主站点创建文件
        ScmId fileId = ScmFileUtils.create( rootSiteWs, filename, filePath );
        fileIds.add( fileId );

        // 创建迁移任务
        scmSchedule = ScmScheduleUtils.createCleanSchedule( rootSiteSession,
                branchSite, wsp, queryCond );

        SiteWrapper[] expSites = { rootSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSites );
        scmSchedule.disable();
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmSystem.Schedule.delete( rootSiteSession,
                        scmSchedule.getId() );
                ScmScheduleUtils.cleanTask( rootSiteSession,
                        scmSchedule.getId() );
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            rootSiteSession.close();
        }
    }

}