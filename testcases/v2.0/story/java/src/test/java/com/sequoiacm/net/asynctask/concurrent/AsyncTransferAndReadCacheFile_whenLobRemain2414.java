package com.sequoiacm.net.asynctask.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Description: SCM-2403 ::目标站点存在残留文件，并发迁移和跨中心读
 * @author fanyu
 * @Date:2019年02月28日
 * @version:1.0
 */

public class AsyncTransferAndReadCacheFile_whenLobRemain2414
        extends TestScmBase {
    private final int fileSize = 10 * 1024 * 1024;
    private boolean runSuccess = false;
    private ScmId fileId = null;
    private File localPath = null;
    private String filePath = null;
    private String remainFilePath = null;
    private String fileName = "file2414";
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        remainFilePath = localPath + File.separator + "localFile_"
                + fileSize / 2 + ".txt";
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( remainFilePath, fileSize / 2 );
        wsp = ScmInfo.getWs();
        List< SiteWrapper > siteList = ScmNetUtils.getSortSites( wsp );
        sourceSite = siteList.get( 0 );
        targetSite = siteList.get( 1 );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        // login in
        sessionA = TestScmTools.createSession( sourceSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        prepareFiles();
        // make remain
        TestSdbTools.Lob.putLob( targetSite, wsp, fileId, remainFilePath );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        TransferThread transferThd = new TransferThread();
        transferThd.start();
        ReadThread readThd = new ReadThread();
        readThd.start();
        Assert.assertEquals( transferThd.isSuccess(), true,
                transferThd.getErrorMsg() );
        Assert.assertEquals( readThd.isSuccess(), true, readThd.getErrorMsg() );
        // check result
        SiteWrapper[] expSiteList = { sourceSite, targetSite };
        ScmTaskUtils.waitAsyncTaskFinished( wsA, fileId, expSiteList.length );
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSiteList, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void prepareFiles() throws Exception {
        ScmFile scmfile = ScmFactory.File.createInstance( wsA );
        scmfile.setContent( filePath );
        scmfile.setFileName( fileName + "_" + UUID.randomUUID() );
        scmfile.setAuthor( fileName );
        fileId = scmfile.save();
    }

    private class TransferThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession sessionA = null;
            try {
                sessionA = TestScmTools.createSession( sourceSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), sessionA );
                ScmFactory.File.asyncTransfer( ws, fileId );
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }
    }

    private class ReadThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession sessionA = null;
            OutputStream fos = null;
            ScmInputStream sis = null;
            try {
                sessionA = TestScmTools.createSession( targetSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), sessionA );
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
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }
    }
}