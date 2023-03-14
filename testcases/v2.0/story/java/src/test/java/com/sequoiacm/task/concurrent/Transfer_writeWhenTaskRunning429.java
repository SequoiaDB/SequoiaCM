package com.sequoiacm.task.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.client.common.ScmType;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-429: “执行迁移任务”过程中写文件
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、A线程在分中心A创建迁移任务； 2、“执行迁移任务”过程中（调用ScmTask.getRunningFlag()接口获取任务状态为running）
 * B线程在分中心A写入满足迁移条件的文件； 3、检查A、B线程执行结果正确性；
 */

public class Transfer_writeWhenTaskRunning429 extends TestScmBase {
    private final int fileSize = 1024 * 1024;
    private final int fileNum = 50;
    private final String authorName = "WriteWhenTaskRunning429";
    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;

    private ScmSession sessionA = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    private ScmId taskId = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            sessionA = ScmSessionUtils.createSession( branceSite );
            prepareFiles( sessionA );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            taskId = transferAllFile( sessionA );

            WriteThread wThd = new WriteThread();
            wThd.start( 10 );
            Assert.assertTrue( wThd.isSuccess(), wThd.getErrorMsg() );

            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
            checkTransfered();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), sessionA );
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestSdbTools.Task.deleteMeta( taskId );

                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void prepareFiles( ScmSession session ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                session );
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            scmfile.setContent( filePath );
            fileIdList.add( scmfile.save() );
        }
    }

    private ScmId transferAllFile( ScmSession session ) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                session );
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        return ScmSystem.Task.startTransferTask( ws, condition,
                ScmType.ScopeType.SCOPE_CURRENT, rootSite.getSiteName() );
    }

    private void checkTransfered() {
        try {
            SiteWrapper[] expSiteList = { rootSite, branceSite };
            ScmFileUtils.checkMetaAndData( ws_T,
                    fileIdList.subList( 0, fileNum ), expSiteList, localPath,
                    filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private class WriteThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession ss = null;
            try {
                ss = ScmSessionUtils.createSession( branceSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), ss );
                synchronized ( fileIdList ) {
                    ScmId fileId = ScmFileUtils.create( ws,
                            authorName + UUID.randomUUID(), filePath );
                    fileIdList.add( fileId );
                }
            } finally {
                ss.close();
            }
        }
    }
}