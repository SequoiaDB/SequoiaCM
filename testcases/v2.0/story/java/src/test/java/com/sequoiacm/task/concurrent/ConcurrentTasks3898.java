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
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-3898:并发执行清理任务，清理相同站点上相同文件
 * @author YiPan
 * @date 2021/10/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ConcurrentTasks3898 extends TestScmBase {
    private static final String fileName = "file3898";
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
    private List< ScmId > taskIds = new ArrayList<>();
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
        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        branchSiteSession = TestScmTools.createSession( branchSite );
        branchSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    public void test() throws Exception {
        // 主站点创建文件缓存至分站点
        ScmId fileId = ScmFileUtils.create( rootSiteWs, fileName, filePath );
        fileIds.add( fileId );
        ScmFactory.File.asyncCache( branchSiteWs, fileId );
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileId, 2 );

        // 创建3个并发清理任务清理分站点
        ThreadExecutor t = new ThreadExecutor();
        for ( int i = 0; i < 3; i++ ) {
            t.addWorker( new CreateCleanTask( branchSite, wsp, fileName ) );
        }
        t.run();

        // 校验文件元数据
        SiteWrapper[] expSite = { rootSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSite );

        // 校验任务执行记录,只能有一条成功记录
        long successCount = 0;
        for ( ScmId taskId : taskIds ) {
            ScmTask task = ScmSystem.Task.getTask( rootSiteSession, taskId );
            successCount = successCount + task.getSuccessCount();
        }
        Assert.assertEquals( 1, successCount );
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
            try ( ScmSession session = TestScmTools
                    .createSession( targetSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmId taskId = ScmSystem.Task.startCleanTask( ws, cond,
                        ScmType.ScopeType.SCOPE_CURRENT );
                taskIds.add( taskId );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            }
        }
    }
}