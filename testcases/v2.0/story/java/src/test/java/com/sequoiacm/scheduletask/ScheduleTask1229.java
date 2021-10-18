package com.sequoiacm.scheduletask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @FileName SCM-1229:创建调度任务，类型为清理，指定站点为分站点
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class ScheduleTask1229 extends TestScmBase {
    private final static int fileNum = 10;
    private final static int fileSize = 100;
    private final static String name = "scheTask1229";
    private boolean runSuccess = false;
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private ScmSession branchSiteSession = null;
    private ScmWorkspace branchSiteWs = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private WsWrapper wsp = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;
    private ScmSchedule scmSchedule;
    private ScmScheduleContent content = null;
    private String cron = "* * * * * ?";

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        rootSite = ScmInfo.getRootSite();
        branSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        branchSiteSession = TestScmTools.createSession( branSite );
        branchSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        readyScmFile( 0, fileNum );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        SiteWrapper[] expSites = { rootSite };
        createScheduleTask();
        checkScheduleTaskInfo();
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSites );
        runSuccess = true;
    }

    @AfterClass
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
            branchSiteSession.close();
        }
    }

    private void readyScmFile( int startNum, int endNum ) throws Exception {
        for ( int i = startNum; i < endNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( branchSiteWs );
            file.setFileName( "file" + name + i );
            file.setAuthor( name );
            file.setContent( filePath );
            ScmId fileId = file.save();
            fileIds.add( fileId );

            ScmFile file2 = ScmFactory.File.getInstance( rootSiteWs, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file2.getContent( downloadPath );
        }
    }

    private void createScheduleTask() throws ScmException {
        String maxStayTime = "0d";
        ScmScheduleCleanFileContent cleanFileContent = new ScmScheduleCleanFileContent(
                branSite.getSiteName(), maxStayTime, queryCond );
        Assert.assertEquals( cleanFileContent.getSiteName(),
                branSite.getSiteName() );
        Assert.assertEquals( cleanFileContent.getMaxStayTime(), maxStayTime );
        Assert.assertEquals( cleanFileContent.getExtraCondition(), queryCond );
        content = cleanFileContent;
        scmSchedule = ScmSystem.Schedule.create( branchSiteSession,
                wsp.getName(), ScheduleType.CLEAN_FILE, name, "", content,
                cron );
    }

    private void checkScheduleTaskInfo() {
        Assert.assertEquals( scmSchedule.getType(), ScheduleType.CLEAN_FILE );
        Assert.assertEquals( scmSchedule.getName(), name );
        Assert.assertEquals( scmSchedule.getDesc(), "" );
        Assert.assertEquals( scmSchedule.getContent(), content );
        Assert.assertEquals( scmSchedule.getCron(), cron );
        Assert.assertEquals( scmSchedule.getWorkspace(), wsp.getName() );
    }
}