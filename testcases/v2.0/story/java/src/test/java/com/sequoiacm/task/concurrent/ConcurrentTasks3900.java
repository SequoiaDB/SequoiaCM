package com.sequoiacm.task.concurrent;

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
 * @descreption SCM-3900:并发执行清理任务，清理不同站点上不同文件
 * @author YiPan
 * @date 2021/10/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ConcurrentTasks3900 extends TestScmBase {
    private static final String fileName = "file3900_";
    private static final String fileAuthor = "file3900Author";
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
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 主站点创建2个文件缓存至分站点
        List< ScmId > file1Ids = createFileAndCache( fileName + 1 );
        List< ScmId > file2Ids = createFileAndCache( fileName + 2 );

        // 创建2个并发清理任务，清理不同站点上不同文件
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new CreateCleanTask( rootSite, wsp, fileName + 1 ) );
        t.addWorker( new CreateCleanTask( branchSite, wsp, fileName + 2 ) );
        t.run();

        // 校验文件元数据
        SiteWrapper[] file1ExpSite = { branchSite };
        SiteWrapper[] file2ExpSite = { rootSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs, file1Ids, file1ExpSite );
        ScmScheduleUtils.checkScmFile( rootSiteWs, file2Ids, file2ExpSite );
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

        public CreateCleanTask( SiteWrapper targetSite, WsWrapper wsp,
                String filename ) {
            this.targetSite = targetSite;
            this.wsp = wsp;
            this.filename = filename;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            try ( ScmSession session = ScmSessionUtils
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

    private List< ScmId > createFileAndCache( String fileName )
            throws Exception {
        List< ScmId > fileIds = new ArrayList<>();
        ScmFile file = ScmFactory.File.createInstance( rootSiteWs );
        file.setFileName( fileName );
        file.setAuthor( fileAuthor );
        file.setContent( filePath );
        ScmId fileId = file.save();
        fileIds.add( fileId );
        ScmFactory.File.asyncCache( branchSiteWs, fileId );
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileId, 2 );
        return fileIds;
    }
}