package com.sequoiacm.scheduletask;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-1228:创建调度任务，类型为清理，指定站点为主站点
 * @author huangxiaoni
 * @createDate 2018-04-17
 * @updateUser YiPan
 * @updateDate 2021.9.2
 * @updateRemark 星状主站点不能创建清理任务约束已取消，适配用例
 * @version 1.1
 */

public class ScheduleTask1228 extends TestScmBase {
    private final static int fileSize = 1024;
    private final static String filename = "scheTask1228";
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
    private void setUp() throws ScmException, IOException {
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
        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( filename ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        // 主站点创建文件
        ScmFileUtils.create( rootSiteWs, filename, filePath );

        // 创建迁移任务
        scmSchedule = ScmScheduleUtils.createCleanSchedule( rootSiteSession,
                rootSite, wsp, queryCond );

        SiteWrapper[] expSites = { rootSite, branchSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSites );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            ScmSystem.Schedule.delete( rootSiteSession, scmSchedule.getId() );
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                ScmScheduleUtils.cleanTask( rootSiteSession,
                        scmSchedule.getId() );
            }
        } finally {
            rootSiteSession.close();
        }
    }

}