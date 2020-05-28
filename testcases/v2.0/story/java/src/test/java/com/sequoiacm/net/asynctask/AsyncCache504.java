package com.sequoiacm.net.asynctask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
import com.sequoiacm.exception.ScmError;
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
 * @FileName SCM-504: 异步缓存单个文件
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * 1、在分中心A异步缓存单个文件； 2、检查执行结果正确性； 3、后台异步缓存任务执行完成后检查缓存后的文件正确性；
 */
public class AsyncCache504 extends TestScmBase {
    private static final String fileName = "AsyncCache504";
    private boolean runSuccess = false;
    private ScmId fileId = null;
    private int fileSize = new Random().nextInt( 1024 ) + 1024;
    private File localPath = null;
    private String filePath = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;

    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branceSiteList = new ArrayList< SiteWrapper >();
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
            if ( ScmInfo.getSiteNum() > 2 ) {
                branceSiteList = ScmInfo.getBranchSites( 2 );
            } else {
                branceSiteList = ScmInfo.getBranchSites( 1 );
            }
            ws_T = ScmInfo.getWs();

            // login in
            sessionM = TestScmTools.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );
            sessionA = TestScmTools.createSession( branceSiteList.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
            writeFileFromMainCenter();
        } catch ( ScmException | IOException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" }, enabled = false) // bug:315
    private void test() throws Exception {
        AsyncCacheFromSubCenterB();
        checkResult();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void writeFileFromMainCenter() {
        try {
            ScmFile scmfile = ScmFactory.File.createInstance( wsM );
            scmfile.setContent( filePath );
            scmfile.setFileName( fileName + "_" + UUID.randomUUID() );
            fileId = scmfile.save();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void AsyncCacheFromSubCenterB() {
        try {
            // cache
            ScmFactory.File.asyncCache( wsA, fileId );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void checkResult() {
        try {
            SiteWrapper[] expSiteList = { rootSite, branceSiteList.get( 0 ) };
            ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId,
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePath );
            checkFreeSite();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void checkFreeSite() throws Exception {
        ScmSession session = null;
        try {
            if ( branceSiteList.size() == 2 ) {
                session = TestScmTools.createSession( branceSiteList.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );
                ScmFileUtils.checkData( ws, fileId, localPath, filePath );
                Assert.assertFalse( true,
                        "expect result is fail but actual is success." );
            }
        } catch ( ScmException e ) {
            if ( ScmError.DATA_NOT_EXIST != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
