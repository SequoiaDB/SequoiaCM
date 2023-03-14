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
 * @Descreption SCM-3740:创建调度任务，类型为迁移，指定源站点为主站点，目标站点为分站点
 * @Author YiPan
 * @CreateDate 2021/9/8
 * @Version 1.0
 */
public class ScheduleTask3740 extends TestScmBase {
    private final static int fileSize = 1024;
    private final static String filename = "file3740";
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

    @BeforeClass(alwaysRun = true)
    public void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        rootSiteSession = ScmSessionUtils.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( filename ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    public void test() throws Exception {
        // 主站点创建文件
        ScmId fileId = ScmFileUtils.create( rootSiteWs, filename, filePath );
        fileIds.add( fileId );

        // 创建迁移任务
        scmSchedule = ScmScheduleUtils.createCopySchedule( rootSiteSession,
                rootSite, branchSite, wsp, queryCond );

        SiteWrapper[] expSites = { rootSite, branchSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSites );
        scmSchedule.disable();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
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