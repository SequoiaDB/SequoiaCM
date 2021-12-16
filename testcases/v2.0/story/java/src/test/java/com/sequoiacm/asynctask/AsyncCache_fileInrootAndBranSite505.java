package com.sequoiacm.asynctask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-505: 分中心和主中心均存在文件
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、在分中心A异步缓存单个文件； 2、检查执行结果正确性；
 */

public class AsyncCache_fileInrootAndBranSite505 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private boolean runSuccess = false;
    private ScmId fileId = null;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "AsyncCache505";
    private ScmSession sessionM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace ws = null;

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

            sessionM = TestScmTools.createSession( rootSite );
            sessionA = TestScmTools.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );
            prepareFiles();
        } catch ( Exception e ) {
            if ( sessionA != null ) {
                sessionA.close();
            }
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            readFile( sessionA, fileId );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    sessionA );
            ScmFactory.File.asyncCache( ws, fileId );
            checkResult();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() + " fileId = " + fileId.get() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), sessionA );
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void prepareFiles() throws Exception {
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setContent( filePath );
        scmfile.setFileName( fileName + "_" + UUID.randomUUID() );
        fileId = scmfile.save();
    }

    private void readFile( ScmSession session, ScmId fileId ) throws Exception {
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            fos = new FileOutputStream( new File( downloadPath ) );
            sis = ScmFactory.File.createInputStream( scmfile );
            sis.read( fos );
        } finally {
            if ( fos != null ) {
                fos.close();
            }
            if ( sis != null ) {
                sis.close();
            }
        }
    }

    private void checkResult() {
        try {
            SiteWrapper[] expSiteList = { rootSite, branceSite };
            ScmTaskUtils.waitAsyncTaskFinished( ws, fileId,
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + " fileId = " + fileId.get() );
        }
    }
}
