package com.sequoiacm.readcachefile.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.readcachefile.AcrossCenterReadFileUtils;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
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
 * @descreption SCM-3653:跨站点读不缓存和指定UNSEEKABLE至本地操作并发
 * @author YiPan
 * @date 2021.7.9
 * @updateUser YiPan
 * @updateDate 2021/11/24
 * @updateRemark
 * @version 1.0
 */
public class AcrossCenterReadFile3653 extends TestScmBase {
    private final int branSitesNum = 2;
    private int fileSize = 1024 * 1024 * 50;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file3653";
    private WsWrapper wsp = null;
    private List< SiteWrapper > branSites = null;
    private ScmSession branchSite1session;
    private ScmSession branchSite2session;
    private ScmWorkspace branchSite1Ws;
    private ScmWorkspace branchSite2Ws;
    private SiteWrapper branchSite1;
    private SiteWrapper branchSite2;
    private BSONObject queryCond;
    private ScmId fileId = null;
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
        branchSite1session = TestScmTools.createSession( branchSite1 );
        branchSite2session = TestScmTools.createSession( branchSite2 );
        branchSite1Ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite1session );
        branchSite2Ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite2session );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        // branchSite1创建文件
        fileId = ScmFileUtils.create( branchSite1Ws, fileName, filePath );

        // branchSite2并发操作
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new SeekFile() );
        t.addWorker( new GetContentReadFile() );
        t.addWorker( new GetInputStreamReadFile() );
        t.run();

        SiteWrapper[] expMetaSites = { branchSite1 };
        Object[] expDataSites = { branchSite1.getSiteId() };
        ScmFileUtils.checkMeta( branchSite2Ws, fileId, expMetaSites );
        Object[] actDataSites = AcrossCenterReadFileUtils.getCacheDataSites(
                wsp.getName(), fileId, localPath, filePath );
        Assert.assertEqualsNoOrder( actDataSites, expDataSites );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                branchSite1session.close();
                branchSite2session.close();
            }
        }
    }

    private class SeekFile {
        @ExecuteOrder(step = 1)
        private void test() throws Exception {
            ScmSession session = TestScmTools.createSession( branchSite2 );
            OutputStream os = null;
            ScmInputStream is = null;
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            try {
                ScmWorkspace workspace = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile instance = ScmFactory.File.getInstance( workspace,
                        fileId );
                is = ScmFactory.File.createInputStream(
                        ScmType.InputStreamType.UNSEEKABLE, instance );
                os = new FileOutputStream( downloadPath );
                int len;
                int times = 0;
                byte[] buffer = new byte[ 1024 ];
                while ( ( len = is.read( buffer, 0, buffer.length ) ) != -1
                        && ( times * 1024 ) < ( fileSize / 2 ) ) {
                    os.write( buffer, 0, len );
                    times++;
                }

            } finally {
                if ( is != null ) {
                    is.close();
                }
                if ( os != null ) {
                    os.close();
                }
                session.close();
            }
            String tmpPath = TestTools.LocalFile.initDownloadPath( localPath,
                    TestTools.getMethodName(),
                    Thread.currentThread().getId() + 1 );
            TestTools.LocalFile.readFile( filePath, 0, fileSize / 2, tmpPath );
            Assert.assertEquals( TestTools.getMD5( downloadPath ),
                    TestTools.getMD5( tmpPath ) );
        }
    }

    private class GetContentReadFile {
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

    private class GetInputStreamReadFile {
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