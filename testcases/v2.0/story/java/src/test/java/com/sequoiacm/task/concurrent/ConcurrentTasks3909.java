package com.sequoiacm.task.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
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
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-3909:并发执行迁移和清理任务，部分迁移源、目标站点和清理站点交叉，操作不同文件
 * @author YiPan
 * @date 2021/10/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ConcurrentTasks3909 extends TestScmBase {
    private static final String fileName = "file3909";
    private static final String fileAuthor = "author3909";
    private static final int fileSize = 1024 * 1024 * 5;
    private static final int fileNum = 10;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite1 = null;
    private SiteWrapper branchSite2 = null;
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private ScmSession branchSite1Session = null;
    private ScmWorkspace branchSite1Ws = null;
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
        List< SiteWrapper > branchSites = ScmInfo.getBranchSites( 2 );
        branchSite1 = branchSites.get( 0 );
        branchSite2 = branchSites.get( 1 );

        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        branchSite1Session = TestScmTools.createSession( branchSite1 );
        branchSite1Ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite1Session );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        // 主站点创建10个文件，缓存至分站点1
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = createFile( rootSiteWs, fileName + i );
            fileIds.add( fileId );
            ScmFactory.File.asyncCache( branchSite1Ws, fileId );
            ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileId, 2 );
        }

        // 创建2个并发任务，分站点1清理文件，主站点迁移文件至分站点2
        ThreadExecutor t = new ThreadExecutor();
        for ( int i = 0; i < fileNum / 2; i++ ) {
            t.addWorker(
                    new CreateCleanTask( branchSite1, wsp, fileName + i ) );
        }
        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            t.addWorker( new CreateTransferTask( rootSite, branchSite2, wsp,
                    fileName + i ) );
        }
        t.run();

        // 校验文件元数据
        List< ScmId > cleanFileIds = fileIds.subList( 0, fileNum / 2 );
        List< ScmId > transFileIds = fileIds.subList( fileNum / 2, fileNum );

        SiteWrapper[] expCleanSite = { rootSite };
        SiteWrapper[] expTransSite = { rootSite, branchSite1, branchSite2 };
        ScmScheduleUtils.checkScmFile( rootSiteWs, cleanFileIds, expCleanSite );
        ScmScheduleUtils.checkScmFile( rootSiteWs, transFileIds, expTransSite );
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
            branchSite1Session.close();
        }
    }

    private class CreateCleanTask {
        private SiteWrapper targetSite;
        private WsWrapper wsp;
        private String filename;

        public CreateCleanTask( SiteWrapper targetSite, WsWrapper wsp,
                String filename ) {
            this.targetSite = targetSite;
            this.wsp = wsp;
            this.filename = filename;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            try ( ScmSession session = TestScmTools
                    .createSession( targetSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_NAME )
                        .is( this.filename ).get();
                ScmSystem.Task.startCleanTask( ws, cond,
                        ScmType.ScopeType.SCOPE_CURRENT );
            }
        }
    }

    private class CreateTransferTask {
        private SiteWrapper sourceSite;
        private SiteWrapper targetSite;
        private WsWrapper wsp;
        private String filename;

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
                ScmSystem.Task.startTransferTask( ws, cond,
                        ScmType.ScopeType.SCOPE_CURRENT,
                        targetSite.getSiteName() );
            }
        }
    }

    private ScmId createFile( ScmWorkspace ws, String fileName )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileAuthor );
        file.setContent( filePath );
        return file.save();
    }
}