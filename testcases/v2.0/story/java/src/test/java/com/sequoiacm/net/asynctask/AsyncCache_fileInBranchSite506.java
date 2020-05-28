package com.sequoiacm.net.asynctask;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-506: 分中心存在但主中心不存在
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、在分中心A异步缓存单个文件； 2、检查执行结果正确性；
 */

public class AsyncCache_fileInBranchSite506 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private boolean runSuccess = false;
    private ScmSession sessionA = null;
    private ScmSession sessionB = null;
    private ScmId fileId = null;
    private String fileName = "AsyncCache506";
    private File localPath = null;
    private String filePath = null;

    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
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

            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( ws_T );
            sourceSite = siteList.get( 0 );
            targetSite = siteList.get( 1 );

            sessionA = TestScmTools.createSession( sourceSite );
            sessionB = TestScmTools.createSession( targetSite );

            prepareFiles( sessionA );
        } catch ( Exception e ) {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                sessionB );
        ScmFactory.File.asyncCache( ws, fileId );
        ScmTaskUtils.waitAsyncTaskFinished( ws, fileId, 2 );
        checkAsync();
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

    private void checkAsync() throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                sessionB );
        SiteWrapper[] expSiteList = { sourceSite, targetSite };
        ScmFileUtils.checkMeta( ws, fileId, expSiteList );
    }
}