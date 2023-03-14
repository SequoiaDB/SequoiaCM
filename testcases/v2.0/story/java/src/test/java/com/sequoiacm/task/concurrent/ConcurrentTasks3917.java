package com.sequoiacm.task.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-3917:清理任务和跨中心读取文件并发，清理文件跨中心读取站点
 * @author YiPan
 * @date 2021/10/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ConcurrentTasks3917 extends TestScmBase {
    private static final String fileName = "file3917";
    private static final int fileSize = 1024 * 1024 * 5;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private ScmSession branchSiteSession = null;
    private ScmWorkspace branchSiteWs = null;
    private WsWrapper wsp = null;
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private BSONObject queryCond = null;
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
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

        rootSiteSession = ScmSessionUtils.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        branchSiteSession = ScmSessionUtils.createSession( branchSite );
        branchSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    public void test() throws Exception {
        // 主站点创建文件
        ScmId fileId = ScmFileUtils.create( rootSiteWs, fileName, filePath );
        fileIds.add( fileId );

        // 创建2个并发任务，分站点清理文件，分站点跨中心读
        CreateCleanTask cleanTask = new CreateCleanTask( branchSite, wsp,
                fileName );
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( cleanTask );
        t.addWorker( new CacheReadFile( branchSite, wsp, fileId ) );
        t.run();

        // 校验文件存在站点
        SiteWrapper[] expSite;
        if ( cleanTask.taskInfo.getSuccessCount() == 1 ) {
            expSite = new SiteWrapper[] { rootSite };
        } else {
            Assert.assertEquals( cleanTask.taskInfo.getSuccessCount(), 0 );
            expSite = new SiteWrapper[] { rootSite, branchSite };
        }
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSite );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            rootSiteSession.close();
            branchSiteSession.close();
        }
    }

    private class CreateCleanTask {
        private SiteWrapper targetSite;
        private WsWrapper wsp;
        private String filename;
        private BSONObject cond;
        private ScmTask taskInfo;

        public CreateCleanTask( SiteWrapper targetSite, WsWrapper wsp,
                String filename ) throws ScmException {
            this.targetSite = targetSite;
            this.wsp = wsp;
            this.filename = filename;
            cond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                    .is( this.filename ).get();
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            try ( ScmSession session = ScmSessionUtils
                    .createSession( targetSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmId taskId = ScmSystem.Task.startCleanTask( ws, cond,
                        ScmType.ScopeType.SCOPE_CURRENT );
                taskInfo = ScmSystem.Task.getTask( session, taskId );
            }
        }
    }

    private class CacheReadFile {
        private SiteWrapper downloadSite;
        private WsWrapper wsp;
        private ScmId fileId;

        public CacheReadFile( SiteWrapper downloadSite, WsWrapper wsp,
                ScmId fileId ) {
            this.downloadSite = downloadSite;
            this.wsp = wsp;
            this.fileId = fileId;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            try ( ScmSession session = ScmSessionUtils
                    .createSession( downloadSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.getContent( downloadPath );
            }
        }
    }
}