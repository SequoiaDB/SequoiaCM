package com.sequoiacm.net.task.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
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
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-3899:并发执行清理任务，清理不同站点上相同文件,b场景
 * @author YiPan
 * @date 2021/10/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ConcurrentTasks3899B extends TestScmBase {
    private static final String fileName = "file3899B";
    private static final int fileSize = 1024 * 1024 * 50;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite1 = null;
    private SiteWrapper branchSite2 = null;
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private ScmSession branchSite1Session = null;
    private ScmWorkspace branchSite1Ws = null;
    private ScmSession branchSite2Session = null;
    private ScmWorkspace branchSite2Ws = null;
    private WsWrapper wsp = null;
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private BSONObject queryCond = null;
    private boolean runSuccess = false;

    @BeforeClass
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

        branchSite2Session = TestScmTools.createSession( branchSite2 );
        branchSite2Ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite1Session );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        // 主站点创建文件缓存至分站点1、分站点2
        ScmId fileId = ScmFileUtils.create( rootSiteWs, fileName, filePath );
        fileIds.add( fileId );
        ScmFactory.File.asyncCache( branchSite1Ws, fileId );
        ScmFactory.File.asyncCache( branchSite2Ws, fileId );
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileId, 2 );

        // 并发清理分站点1、分站点2
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new CreateCleanTask( branchSite1, wsp, fileName ) );
        t.addWorker( new CreateCleanTask( branchSite2, wsp, fileName ) );
        t.run();

        // 校验文件元数据
        SiteWrapper[] expSite = { rootSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSite );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            rootSiteSession.close();
            branchSite1Session.close();
            branchSite2Session.close();
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
                BSONObject query = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_NAME )
                        .is( this.filename ).get();
                ScmSystem.Task.startCleanTask( ws, query,
                        ScmType.ScopeType.SCOPE_CURRENT );
            }
        }
    }
}