package com.sequoiacm.scheduletask.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
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
import java.util.UUID;

/**
 * @Descreption SCM-5234:迁移并清理任务和创建文件并发
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class MoveFile5234 extends TestScmBase {
    private String fileName = "file5234_";
    private String fileAuthor = "author5234";
    private String titleA = "titleA5234";
    private String titleB = "titleB5234";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private ScmSession sessionM = null;
    private WsWrapper wsp;
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int fileNum = 10;
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
        branchSite = ScmInfo.getBranchSite();
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        createFile( wsM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        MoveFile moveFileThread = new MoveFile( titleA );
        CreateFile createFileA = new CreateFile( titleA );
        CreateFile createFileB = new CreateFile( titleB );
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( moveFileThread );
        t.addWorker( createFileA );
        t.addWorker( createFileB );
        t.run();

        // 校验存量文件被迁移
        SiteWrapper[] expSites = { branchSite };
        ScmFileUtils.checkMetaAndData( wsp, fileIds, expSites, localPath,
                filePath );
        // 校验文件B不受影响
        expSites = new SiteWrapper[] { rootSite };
        ScmFileUtils.checkMetaAndData( wsp, createFileB.getFileId(), expSites,
                localPath, filePath );
        // 根据任务执行结果判断文件A有没有被任务迁移并清理
        ScmTask task = ScmSystem.Task.getTask( sessionM,
                moveFileThread.getTaskId() );
        if ( task.getSuccessCount() == fileNum + 1 ) {
            expSites = new SiteWrapper[] { branchSite };
            ScmFileUtils.checkMetaAndData( wsp, createFileA.getFileId(),
                    expSites, localPath, filePath );
        } else if ( task.getSuccessCount() == fileNum ) {
            expSites = new SiteWrapper[] { rootSite };
            ScmFileUtils.checkMetaAndData( wsp, createFileA.getFileId(),
                    expSites, localPath, filePath );
        } else {
            throw new Exception( "moveTask:" + task.toString() );
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsp, queryCond );
            } finally {
                sessionM.close();
            }
        }
    }

    private class MoveFile {
        private ScmId taskId;
        private String title;

        public ScmId getTaskId() {
            return taskId;
        }

        public MoveFile( String title ) {
            this.title = title;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            try {
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.TITLE ).is( title ).get();
                ScmMoveTaskConfig config = new ScmMoveTaskConfig();
                config.setCondition( cond );
                config.setTargetSite( branchSite.getSiteName() );
                config.setWorkspace( ws );
                taskId = ScmSystem.Task.startMoveTask( config );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                session.close();
            }
        }
    }

    private class CreateFile {
        private String title;
        private ScmId fileId;

        public CreateFile( String title ) {
            this.title = title;
        }

        public ScmId getFileId() {
            return fileId;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            try {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName + UUID.randomUUID() );
                file.setContent( filePath );
                file.setAuthor( fileAuthor );
                file.setTitle( title );
                fileId = file.save();
            } finally {
                session.close();
            }
        }
    }

    private void createFile( ScmWorkspace ws ) throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + i );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            file.setTitle( titleA );
            fileIds.add( file.save() );
        }
    }
}