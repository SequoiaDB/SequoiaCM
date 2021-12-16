package com.sequoiacm.task.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-3910:创建多个任务并发执行，未超过最大限制
 * @author YiPan
 * @date 2021/10/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ConcurrentTasks3910 extends TestScmBase {
    private static final String fileName = "file3910";
    private static final String fileAuthor = "author3910";
    private static final int fileSize = 1024 * 1024 * 50;
    private static final int taskNum = 10;
    private File localPath = null;
    private String filePath = null;
    private WsWrapper ws1 = null;
    private WsWrapper ws2 = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private ScmSession session = null;
    private ScmWorkspace rootSiteWs1 = null;
    private ScmWorkspace rootSiteWs2 = null;
    private BSONObject queryCond = null;
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

        List< WsWrapper > wss = ScmInfo.getWss( 2 );
        ws1 = wss.get( 0 );
        ws2 = wss.get( 1 );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( ws1, queryCond );
        ScmFileUtils.cleanFile( ws2, queryCond );

        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        session = TestScmTools.createSession( rootSite );
        rootSiteWs1 = ScmFactory.Workspace.getWorkspace( ws1.getName(),
                session );
        rootSiteWs2 = ScmFactory.Workspace.getWorkspace( ws2.getName(),
                session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 主站点在ws1和ws2下创建文件
        List< ScmId > filelist1 = createFile( rootSiteWs1, 0, taskNum / 2 );
        List< ScmId > filelist2 = createFile( rootSiteWs2, taskNum / 2,
                taskNum );

        // 执行并发迁移任务
        ThreadExecutor t = new ThreadExecutor();
        for ( int i = 0; i < taskNum / 2; i++ ) {
            t.addWorker( new CreateTransferTask( rootSite, branchSite, ws1,
                    fileName + i ) );
        }
        for ( int i = taskNum / 2; i < taskNum; i++ ) {
            t.addWorker( new CreateTransferTask( rootSite, branchSite, ws2,
                    fileName + i ) );
        }
        t.run();

        // 校验文件存在站点
        SiteWrapper[] expSite = { rootSite, branchSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs1, filelist1, expSite );
        ScmScheduleUtils.checkScmFile( rootSiteWs2, filelist2, expSite );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( ws1, queryCond );
                ScmFileUtils.cleanFile( ws2, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            session.close();
        }
    }

    private class CreateTransferTask {
        private SiteWrapper sourceSite;
        private SiteWrapper targetSite;
        private WsWrapper wsp;
        private String filename;
        private ScmId taskId;

        public CreateTransferTask( SiteWrapper sourceSite,
                SiteWrapper targetSite, WsWrapper wsp, String filename ) {
            this.sourceSite = sourceSite;
            this.targetSite = targetSite;
            this.wsp = wsp;
            this.filename = filename;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            try ( ScmSession session = TestScmTools
                    .createSession( sourceSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_NAME )
                        .is( this.filename ).get();
                taskId = ScmSystem.Task.startTransferTask( ws, cond,
                        ScmType.ScopeType.SCOPE_CURRENT,
                        targetSite.getSiteName() );
            }
            // 校验任务没有被abort
            ScmTask task = ScmSystem.Task.getTask( session, taskId );
            if ( task
                    .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_ABORT ) {
                throw new Exception(
                        "this task is abort,task id:" + task.getId() );
            }
        }
    }

    private List< ScmId > createFile( ScmWorkspace ws, int startNum,
            int endNum ) throws ScmException {
        List< ScmId > fileIds = new ArrayList<>();
        for ( int i = startNum; i < endNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + i );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            ScmId fileId = file.save();
            fileIds.add( fileId );
        }
        return fileIds;
    }
}