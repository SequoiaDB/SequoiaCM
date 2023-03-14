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
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final String fileName = "file3910_";
    private static final String fileAuthor = "author3910";
    private static final int fileSize = 1024 * 50;
    private static int maxTaskNum;
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
    private static final String clean = "clean";
    private static final String trans = "trans";

    @BeforeClass
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
        session = ScmSessionUtils.createSession( rootSite );
        int nodeNumOfSite = ScmTaskUtils.getNodeNumOfSite( session,
                branchSite.getSiteName() );
        // 默认配置每个节点下最多可执行10个并发调度任务
        maxTaskNum = nodeNumOfSite * 10;

        rootSiteWs1 = ScmFactory.Workspace.getWorkspace( ws1.getName(),
                session );
        rootSiteWs2 = ScmFactory.Workspace.getWorkspace( ws2.getName(),
                session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 主站点在ws1和ws2下创建文件
        Map< String, List< ScmId > > ws1file = createFile( rootSiteWs1, 0,
                maxTaskNum / 2 );
        Map< String, List< ScmId > > ws2file = createFile( rootSiteWs2,
                maxTaskNum / 2, maxTaskNum );
        // 迁移编号为奇数文件至分站点
        transferFile( session, branchSite, ws1.getName() );
        transferFile( session, branchSite, ws2.getName() );

        ThreadExecutor t = new ThreadExecutor();
        for ( int i = 0; i < maxTaskNum; i++ ) {
            WsWrapper wsp;
            // 根据编号区分文件所属ws
            if ( i < maxTaskNum / 2 ) {
                wsp = ws1;
            } else {
                wsp = ws2;
            }
            // 根据编号奇偶区分文件执行任务类型，奇数编号执行清理，偶数编号执行迁移
            if ( i % 2 != 0 ) {
                t.addWorker(
                        new CreateCleanTask( branchSite, wsp, fileName + i ) );
            } else {
                t.addWorker( new CreateTransferTask( rootSite, branchSite, wsp,
                        fileName + i ) );
            }
        }
        t.run();

        // 校验文件存在站点
        SiteWrapper[] cleanExpSite = { rootSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs1, ws1file.get( clean ),
                cleanExpSite );
        ScmScheduleUtils.checkScmFile( rootSiteWs2, ws2file.get( clean ),
                cleanExpSite );
        SiteWrapper[] transExpSite = { rootSite, branchSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs1, ws1file.get( trans ),
                transExpSite );
        ScmScheduleUtils.checkScmFile( rootSiteWs2, ws2file.get( trans ),
                transExpSite );
        runSuccess = true;
    }

    @AfterClass
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
        private String fileName;

        public CreateTransferTask( SiteWrapper sourceSite,
                SiteWrapper targetSite, WsWrapper wsp, String fileName ) {
            this.sourceSite = sourceSite;
            this.targetSite = targetSite;
            this.wsp = wsp;
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmId taskId;
            try ( ScmSession session = ScmSessionUtils
                    .createSession( sourceSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_NAME )
                        .is( this.fileName ).get();
                taskId = ScmSystem.Task.startTransferTask( ws, cond,
                        ScmType.ScopeType.SCOPE_CURRENT,
                        targetSite.getSiteName() );
            }
            // 校验任务没有被abort
            ScmTask task = ScmSystem.Task.getTask( session, taskId );
            if ( task
                    .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_ABORT ) {
                throw new Exception( "this task is abort,taskInfo:" + task );
            }
        }
    }

    private class CreateCleanTask {
        private SiteWrapper targetSite;
        private WsWrapper wsp;
        private String fileName;

        public CreateCleanTask( SiteWrapper targetSite, WsWrapper wsp,
                String fileName ) {
            this.targetSite = targetSite;
            this.wsp = wsp;
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmId taskId;
            try ( ScmSession session = ScmSessionUtils
                    .createSession( targetSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                BSONObject query = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_NAME )
                        .is( this.fileName ).get();
                taskId = ScmSystem.Task.startCleanTask( ws, query,
                        ScmType.ScopeType.SCOPE_CURRENT );
            }
            // 校验任务没有被abort
            ScmTask task = ScmSystem.Task.getTask( session, taskId );
            if ( task
                    .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_ABORT ) {
                throw new Exception( "this task is abort,taskInfo:" + task );
            }
        }
    }

    private Map< String, List< ScmId > > createFile( ScmWorkspace ws,
            int startNum, int endNum ) throws ScmException {
        List< ScmId > cleanFileIds = new ArrayList<>();
        List< ScmId > transFileIds = new ArrayList<>();
        for ( int i = startNum; i < endNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + i );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            // 编号为奇数的设置为清理任务使用文件，其余为迁移任务使用
            if ( i % 2 != 0 ) {
                file.setTitle( clean );
                cleanFileIds.add( file.save() );
            } else {
                file.setTitle( trans );
                transFileIds.add( file.save() );
            }
        }
        Map< String, List< ScmId > > map = new HashMap<>();
        map.put( clean, cleanFileIds );
        map.put( trans, transFileIds );
        return map;
    }

    private void transferFile( ScmSession session, SiteWrapper targetSite,
            String wsName ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        BSONObject query = ScmQueryBuilder.start( ScmAttributeName.File.TITLE )
                .is( clean ).get();
        ScmId taskId = ScmSystem.Task.startTransferTask( ws, query,
                ScmType.ScopeType.SCOPE_CURRENT, targetSite.getSiteName() );
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }
}
