package com.sequoiacm.asynctask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
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
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @FileName SCM-488: 分中心存在文件，主中心存在残留LOB，且大小一致
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、在分中心A异步迁移单个文件，该文件在主中心有残留LOB，且大小一致； 2、检查执行结果正确性；
 */

public class AsyncTransfer_whenLobRemain488 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private boolean runSuccess = false;
    private ScmId fileId = null;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "AsyncTransferWhenLobRemain488";
    private ScmSession sessionA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace ws = null;

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        branceSite = ScmScheduleUtils.getSortBranchSites().get( 0 );
        ws_T = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branceSite );
        sessionM = TestScmTools.createSession( rootSite );
        ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );
        prepareFiles( sessionA );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        // remain LOB
        TestSdbTools.Lob.putLob( rootSite, ws_T, fileId, filePath );
        ScmWorkspace wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                sessionA );
        ScmFactory.File.asyncTransfer( wsA, fileId, rootSite.getSiteName() );
        // check result
        SiteWrapper[] expSiteList = { rootSite, branceSite };
        ScmTaskUtils.waitAsyncTaskFinished( ws, fileId, expSiteList.length );
        ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                filePath );
        readFile( sessionM );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void prepareFiles( ScmSession session ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                session );
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setContent( filePath );
        scmfile.setFileName( fileName + "_" + UUID.randomUUID() );
        fileId = scmfile.save();
    }

    private void readFile( ScmSession session ) throws Exception {
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
}