package com.sequoiacm.scheduletask;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleMoveFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-5218:修改迁移并清理任务为清理任务
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class MoveFile5218 extends TestScmBase {
    private String fileName = "file5218_";
    private String taskName = "task5218";
    private String fileTitleA = "titleA5218";
    private String fileTitleB = "titleB5218";
    private String fileAuthor = "author5218";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private List< ScmId > cleanFileIds = new ArrayList<>();
    private List< ScmId > moveFileIds = new ArrayList<>();
    private ScmSession sessionM = null;
    private WsWrapper wsp;
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private ScmSchedule sche;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int fileNum = 10;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        sessionM = ScmSessionUtils.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        createFile( wsM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 创建迁移并清理任务，迁移A部分文件到分站点
        BSONObject moveCond = ScmQueryBuilder
                .start( ScmAttributeName.File.TITLE ).is( fileTitleA ).get();
        ScmScheduleMoveFileContent content = new ScmScheduleMoveFileContent(
                rootSite.getSiteName(), branchSite.getSiteName(), "0d",
                moveCond, ScmType.ScopeType.SCOPE_CURRENT );

        // 启动迁移并清理调度任务
        String cron = "0/1 * * * * ?";
        sche = ScmSystem.Schedule.create( sessionM, wsp.getName(),
                ScheduleType.MOVE_FILE, taskName, "", content, cron );

        // 校验迁移并清理任务执行结果
        ScmScheduleUtils.waitForTask( sche, 2 );
        SiteWrapper[] expSites = { branchSite };
        ScmFileUtils.checkMetaAndData( wsp, moveFileIds, expSites, localPath,
                filePath );
        sche.disable();
        ScmScheduleUtils.cleanTask( sessionM, sche.getId() );

        // 先缓存B部分文件到主和分站点
        BSONObject cleanCond = ScmQueryBuilder
                .start( ScmAttributeName.File.TITLE ).is( fileTitleB ).get();
        ScmId task = ScmSystem.Task.startTransferTask( wsM, cleanCond,
                ScmType.ScopeType.SCOPE_CURRENT, branchSite.getSiteName() );
        ScmTaskUtils.waitTaskFinish( sessionM, task );

        // 修改为清理类型,清理分站点
        ScmScheduleCleanFileContent cleanContent = new ScmScheduleCleanFileContent(
                branchSite.getSiteName(), "0d", cleanCond,
                ScmType.ScopeType.SCOPE_CURRENT );
        sche.updateSchedule( ScheduleType.CLEAN_FILE, cleanContent );
        sche.enable();

        // 校验迁移任务执行结果
        ScmScheduleUtils.waitForTask( sche, 2 );
        expSites = new SiteWrapper[] { rootSite };
        ScmFileUtils.checkMetaAndData( wsp, cleanFileIds, expSites, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsp, queryCond );
                ScmSystem.Schedule.delete( sessionM, sche.getId() );
                ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
            } finally {
                sessionM.close();
            }
        }
    }

    private void createFile( ScmWorkspace ws ) throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + i );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            if ( i % 2 == 0 ) {
                file.setTitle( fileTitleA );
                moveFileIds.add( file.save() );
            } else {
                file.setTitle( fileTitleB );
                cleanFileIds.add( file.save() );
            }
        }
    }
}