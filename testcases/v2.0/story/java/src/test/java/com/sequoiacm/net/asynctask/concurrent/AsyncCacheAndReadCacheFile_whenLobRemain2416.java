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
 * @Description: SCM-2416 :: 目标站点存在残留文件，并发异步缓存和跨中心读取文件
 * @author fanyu
 * @Date:2019年02月28日
 * @version:1.0
 */

public class AsyncCacheAndReadCacheFile_whenLobRemain2416 extends TestScmBase {
    private boolean runSuccess = false;
    private int fileSize = 1024;
    private File localPath = null;
    private String filePath = null;
    private String remainFilePath = null;
    private ScmId fileId = null;
    private String fileName = "file2416";
    private ScmSession sessionM = null;
    private ScmWorkspace ws = null;
    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        remainFilePath =
                localPath + File.separator + "localFile_" + fileSize / 2 +
                        ".txt";
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
        sessionM = TestScmTools.createSession( sourceSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        writeFileFromMainCenter();
        //make remain in main center
        TestSdbTools.Lob.putLob( targetSite, wsp, fileId, remainFilePath );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        CacheFromSubCenterA cacheT = new CacheFromSubCenterA();
        ReadFile readA = new ReadFile();
        cacheT.start();
        readA.start();
        Assert.assertEquals( cacheT.isSuccess(), true, cacheT.getErrorMsg() );
        Assert.assertEquals( readA.isSuccess(), true, readA.getErrorMsg() );
        SiteWrapper[] expSiteList = { sourceSite, targetSite };
        ScmTaskUtils.waitAsyncTaskFinished( ws, fileId, expSiteList.length );
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSiteList, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void writeFileFromMainCenter() throws ScmException {
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setContent( filePath );
        scmfile.setFileName( fileName + "_" + UUID.randomUUID() );
        scmfile.setAuthor( fileName );
        fileId = scmfile.save();
    }

    private class CacheFromSubCenterA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession sessionA = null;
            try {
                // login
                sessionA = TestScmTools.createSession( targetSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), sessionA );
                // cache
                ScmFactory.File.asyncCache( ws, fileId );
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }
    }

    private class ReadFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession sessionA = null;
            OutputStream fos = null;
            ScmInputStream sis = null;
            try {
                // login
                sessionA = TestScmTools.createSession( targetSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), sessionA );
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile
                        .initDownloadPath( localPath, TestTools.getMethodName(),
                                Thread.currentThread().getId() );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis = ScmFactory.File.createInputStream( scmfile );
                sis.read( fos );
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
                if ( fos != null ) {
                    fos.close();
                }
                if ( sis != null ) {
                    sis.close();
                }
            }
        }
    }
}
