package com.sequoiacm.scheduletask.concurrent;

import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.statistics.StatisticsFile3858;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.*;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;

/**
 * @description SCM-3901:并发执行迁移任务，相同源站点，相同目标站点，迁移相同文件
 * @author ZhangYanan
 * @createDate 2021.10.27
 * @updateUser ZhangYanan
 * @updateDate 2021.10.27
 * @updateRemark
 * @version v1.0
 */
public class ConcurrentTasks3901 extends TestScmBase {
    private String fileName = "file3901";
    private SiteWrapper branchSite;
    private SiteWrapper rootSite;
    private ScmSession rootSiteSession;
    private ScmWorkspace rootSiteWorkspace = null;
    private File localPath = null;
    private BSONObject queryCond = null;
    private int fileSizes = 1024 * 1024;
    private int threadNums = 6;
    private WsWrapper wsp;
    private List< ScmId > fileIdList = new ArrayList<>();
    private CopyOnWriteArrayList< ScmId > taskIdList = new CopyOnWriteArrayList< ScmId >();
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmScheduleUtils.getSortBranchSites().get( 0 );

        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

        String filePath = localPath + File.separator + "localFile_" + fileSizes
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSizes );

        ScmId fileId = ScmFileUtils.create( rootSiteWorkspace, fileName,
                filePath );
        fileIdList.add( fileId );
    }

    // 问题单SEQUOIACM-744未修改
    @Test(groups = { "twoSite", "fourSite", GroupTags.base }, enabled = false)
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        for ( int i = 0; i < threadNums; i++ ) {
            es.addWorker( new ThreadFileMigration() );
        }
        es.run();

        SiteWrapper[] expSites1 = { rootSite, branchSite };
        ScmScheduleUtils.checkScmFile( rootSiteWorkspace, fileIdList,
                expSites1 );

        int successCountSum = 0;
        for ( int i = 0; i < taskIdList.size(); i++ ) {
            ScmTask task = ScmSystem.Task.getTask( rootSiteSession,
                    taskIdList.get( i ) );
            successCountSum += task.getSuccessCount();
        }
        Assert.assertEquals( successCountSum, 1 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                for ( ScmId taskID : taskIdList ) {
                    TestSdbTools.Task.deleteMeta( taskID );
                }
            } finally {
                rootSiteSession.close();
            }
        }
    }

    private class ThreadFileMigration extends ResultStore {

        @ExecuteOrder(step = 1)
        private void fileMigration() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( rootSite );
                ScmWorkspace workSpacesp = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                        .get();
                ScmId taskID = ScmSystem.Task.startTransferTask( workSpacesp,
                        condition, ScmType.ScopeType.SCOPE_CURRENT,
                        branchSite.getSiteName() );
                ScmTaskUtils.waitTaskFinish( session, taskID );
                taskIdList.add( taskID );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}