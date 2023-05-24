package com.sequoiacm.scheduletask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleMoveFileContent;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @Descreption SCM-6200:指定文件最大存在时间创建调度任务
 * @Author zhangYaNan
 * @CreateDate 2023.05.16
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class ScheduleTask6200 extends TestScmBase {
    private final static int fileNum = 10;
    private final static int fileSize = 100;
    private final static String name = "scheduleTask6200";
    private boolean runSuccess = false;
    private ScmSession ssA = null;
    private ScmSession ssM = null;
    private ScmWorkspace wsA = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private WsWrapper wsp = null;
    private ArrayList< ScmId > expFileIds = new ArrayList<>();
    private ArrayList< ScmId > unExpFileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;
    private ScmSchedule scmSchedule;
    private ScmScheduleContent content = null;
    private String cron = "* * * * * ?";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
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
        ssA = ScmSessionUtils.createSession( branSite );
        ssM = ScmSessionUtils.createSession( rootSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), ssA );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        readyScmFile( wsA, 0, fileNum, true, expFileIds );
        readyScmFile( wsA, fileNum, fileNum * 2, false, unExpFileIds );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        SiteWrapper[] expSites1 = { rootSite, branSite };
        SiteWrapper[] expSites2 = { rootSite };
        SiteWrapper[] expSites3 = { branSite };

        // 验证迁移调度任务
        createCopyScheduleTask();
        ScmScheduleUtils.checkScmFile( wsA, expFileIds, expSites1 );
        ScmScheduleUtils.checkScmFile( wsA, unExpFileIds, expSites3 );
        scmSchedule.delete();
        ScmScheduleUtils.cleanTask( ssA, scmSchedule.getId() );

        // 验证清理调度任务
        createCleanScheduleTask();
        ScmScheduleUtils.checkScmFile( wsA, expFileIds, expSites2 );
        ScmScheduleUtils.checkScmFile( wsA, unExpFileIds, expSites3 );
        scmSchedule.delete();
        ScmScheduleUtils.cleanTask( ssA, scmSchedule.getId() );

        // 验证迁移清理调度任务
        createMoveScheduleTask();
        ScmScheduleUtils.checkScmFile( wsA, expFileIds, expSites3 );
        ScmScheduleUtils.checkScmFile( wsA, unExpFileIds, expSites3 );
        scmSchedule.delete();
        ScmScheduleUtils.cleanTask( ssA, scmSchedule.getId() );

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( ssA != null ) {
                ssA.close();
            }
            if ( ssM != null ) {
                ssM.close();
            }
        }
    }

    private void readyScmFile( ScmWorkspace ws, int startNum, int endNum,
            boolean isBeforeDay, ArrayList< ScmId > fileIds )
            throws ScmException {
        for ( int i = startNum; i < endNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "file" + name + i );
            file.setAuthor( name );
            file.setContent( filePath );
            if ( isBeforeDay ) {
                // 设置文件创建时间为当前时间减一天
                file.setCreateTime( TestTools.DateTools.getBeforeDay( 2 ) );
            }
            fileIds.add( file.save() );
        }

    }

    private void createCopyScheduleTask() throws ScmException {
        String maxStayTime = "0d";
        String existenceTime = "1d";
        ScmScheduleCopyFileContent scmScheduleCopyFileContent = new ScmScheduleCopyFileContent(
                branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                queryCond, existenceTime );
        content = scmScheduleCopyFileContent;
        scmSchedule = ScmSystem.Schedule.create( ssA, wsp.getName(),
                ScheduleType.COPY_FILE, "schedule" + name, "", content, cron );
    }

    private void createCleanScheduleTask() throws ScmException {
        String maxStayTime = "0d";
        String existenceTime = "1d";
        ScmScheduleCleanFileContent scmScheduleCleanFileContent = new ScmScheduleCleanFileContent(
                branSite.getSiteName(), maxStayTime, queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, existenceTime );
        scmSchedule = ScmSystem.Schedule.create( ssA, wsp.getName(),
                ScheduleType.CLEAN_FILE, "schedule" + name, "",
                scmScheduleCleanFileContent, cron );
    }

    private void createMoveScheduleTask() throws ScmException {
        String maxStayTime = "0d";
        String existenceTime = "1d";
        ScmScheduleMoveFileContent scmScheduleMoveFileContent = new ScmScheduleMoveFileContent(
                rootSite.getSiteName(), branSite.getSiteName(), maxStayTime,
                queryCond, ScmType.ScopeType.SCOPE_CURRENT, existenceTime );
        scmSchedule = ScmSystem.Schedule.create( ssM, wsp.getName(),
                ScheduleType.MOVE_FILE, "schedule" + name, "",
                scmScheduleMoveFileContent, cron );
    }
}