package com.sequoiacm.readcachefile.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.util.List;
import java.util.UUID;

/**
 * @Testcase: SCM-3654:多个站点并发指定跨站点读不缓存至本地
 * @author YiPan
 * @date 2021.7.9
 */
public class AcrossCenterReadFile3654 extends TestScmBase {
    private final int branSitesNum = 2;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file3654";
    private WsWrapper wsp = null;
    private List< SiteWrapper > branSites = null;
    private SiteWrapper branchSite1;
    private SiteWrapper branchSite2;
    private SiteWrapper branchSite3;
    private ScmSession branchSite1Session;
    private ScmWorkspace branchSite1Workspace;
    private ScmId fileId = null;
    private int numOfCycles = 10;
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

        branSites = ScmInfo.getBranchSites( branSitesNum );
        wsp = ScmInfo.getWs();
        branchSite1 = branSites.get( 0 );
        branchSite2 = branSites.get( 1 );
        // branchSite3 = branSites.get( 2 );
        branchSite3 = ScmInfo.getRootSite();
        branchSite1Session = ScmSessionUtils.createSession( branchSite1 );
        branchSite1Workspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite1Session );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        // branchSite1创建文件
        fileId = ScmFileUtils.create( branchSite1Workspace,
                fileName + "_" + UUID.randomUUID(), filePath );

        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new GetContentReadFileWithNoCache( branchSite1 ) );
        t.addWorker( new GetInputStreamReadFileWithNoCache( branchSite2 ) );
        t.addWorker( new GetContentReadFileWithNoCache( branchSite3 ) );
        t.addWorker( new GetInputStreamReadFileWithNoCache(
                ScmInfo.getRootSite() ) );
        t.run();

        SiteWrapper[] expSites = { branchSite1 };
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFactory.File.deleteInstance( branchSite1Workspace, fileId,
                        true );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                branchSite1Session.close();
            }
        }
    }

    private class GetContentReadFileWithNoCache {
        private SiteWrapper siteWrapper;

        public GetContentReadFileWithNoCache( SiteWrapper siteWrapper ) {
            this.siteWrapper = siteWrapper;
        }

        @ExecuteOrder(step = 1)
        private void test() throws Exception {
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            OutputStream os = null;
            ScmSession session = ScmSessionUtils.createSession( siteWrapper );
            try {
                ScmWorkspace workspace = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile instance = ScmFactory.File.getInstance( workspace,
                        fileId );
                for ( int i = 0; i < numOfCycles; i++ ) {
                    os = new FileOutputStream( new File( downloadPath + i ) );
                    instance.getContent( os,
                            CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE );
                }
            } finally {
                if ( os != null ) {
                    os.close();
                }
                session.close();
            }
            for ( int i = 0; i < numOfCycles; i++ ) {
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath + i ) );
            }
        }
    }

    private class GetInputStreamReadFileWithNoCache {
        private SiteWrapper siteWrapper;

        public GetInputStreamReadFileWithNoCache( SiteWrapper siteWrapper ) {
            this.siteWrapper = siteWrapper;
        }

        @ExecuteOrder(step = 1)
        private void test() throws Exception {
            OutputStream os = null;
            ScmInputStream is = null;
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            ScmSession session = ScmSessionUtils.createSession( siteWrapper );
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
