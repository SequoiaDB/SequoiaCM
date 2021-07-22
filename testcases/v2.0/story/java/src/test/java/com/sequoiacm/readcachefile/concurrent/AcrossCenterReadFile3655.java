package com.sequoiacm.readcachefile.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.readcachefile.AcrossCenterReadFileUtils;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.util.List;
import java.util.UUID;

/**
 * @Testcase: SCM-3655:跨站点读不缓存和清理缓存操作并发
 * @author YiPan
 * @date 2021.7.9
 */
public class AcrossCenterReadFile3655 extends TestScmBase {
    private final int branSitesNum = 2;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file3655";
    private WsWrapper wsp = null;
    private List< SiteWrapper > branSites = null;
    private SiteWrapper branchSite1;
    private SiteWrapper branchSite2;
    private ScmSession branchSite1Session;
    private ScmSession branchSite2Session;
    private ScmWorkspace branchSite1Workspace;
    private ScmWorkspace branchSite2Workspace;
    private ScmId fileId = null;
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

        branSites = ScmInfo.getBranchSites( branSitesNum );
        wsp = ScmInfo.getWs();
        branchSite1 = branSites.get( 0 );
        branchSite2 = branSites.get( 1 );
        branchSite1Session = TestScmTools.createSession( branchSite1 );
        branchSite2Session = TestScmTools.createSession( branchSite2 );
        branchSite1Workspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite1Session );
        branchSite2Workspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite2Session );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        // branchSite1创建文件
        fileId = ScmFileUtils.create( branchSite1Workspace,
                fileName + "_" + UUID.randomUUID(), filePath );

        // 读取文件缓存至branchSite2
        AcrossCenterReadFileUtils.readFile( branchSite2Workspace, fileId,
                localPath );

        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new CleanFile() );
        t.addWorker( new GetContentReadFileWithNoCache() );
        t.addWorker( new GetInputStreamReadFileWithNoCache() );
        t.run();
        SiteWrapper[] expSites = { branchSite1, ScmInfo.getRootSite() };
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess ) {
            try {
                ScmFactory.File.deleteInstance( branchSite1Workspace, fileId,
                        true );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                branchSite1Session.close();
                branchSite2Session.close();
            }
        }
    }

    private class CleanFile {
        @ExecuteOrder(step = 1)
        private void test() throws Exception {
            ScmSession session = TestScmTools.createSession( branchSite2 );
            try {
                ScmWorkspace workspace = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_ID )
                        .is( fileId.toString() ).get();
                ScmId scmId = ScmSystem.Task.startCleanTask( workspace,
                        condition );
                ScmTaskUtils.waitTaskFinish( session, scmId );
            } finally {
                session.close();
            }
        }
    }

    private class GetContentReadFileWithNoCache {
        @ExecuteOrder(step = 1)
        private void test() throws Exception {
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            OutputStream os = null;
            ScmSession session = TestScmTools.createSession( branchSite2 );
            try {
                ScmWorkspace workspace = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile instance = ScmFactory.File.getInstance( workspace,
                        fileId );
                os = new FileOutputStream( new File( downloadPath ) );
                instance.getContent( os,
                        CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE );
            } finally {
                if ( os != null ) {
                    os.close();
                }
                session.close();
            }
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
        }
    }

    private class GetInputStreamReadFileWithNoCache {
        @ExecuteOrder(step = 1)
        private void test() throws Exception {
            OutputStream os = null;
            ScmInputStream is = null;
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            ScmSession session = TestScmTools.createSession( branchSite2 );
            try {
                ScmWorkspace workspace = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile instance = ScmFactory.File.getInstance( workspace,
                        fileId );
                is = ScmFactory.File.createInputStream( instance,
                        CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE );
                os = new FileOutputStream( downloadPath );
                is.read( os );
            } finally {
                if ( is != null ) {
                    is.close();
                }
                if ( os != null ) {
                    os.close();
                }
                session.close();
            }
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
        }
    }
}