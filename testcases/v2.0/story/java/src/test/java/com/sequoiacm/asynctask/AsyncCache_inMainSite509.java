package com.sequoiacm.asynctask;

import java.io.File;
import java.io.IOException;
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
 * @FileName SCM-509: 在主中心异步缓存单个文件
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * 1、在主中心异步缓存单个文件； 2、检查执行结果正确性；
 */
public class AsyncCache_inMainSite509 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmId fileId = null;
    private int fileSize = new Random().nextInt( 1024 ) + 1024;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "AsyncCache509";

    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;

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

            // login in
            sessionA = TestScmTools.createSession( branceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
            sessionM = TestScmTools.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );
            writeFileFromSubCenterA( wsA );
        } catch ( ScmException | IOException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            AsyncCacheFromMainCenter();
            checkResult();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
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
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void writeFileFromSubCenterA( ScmWorkspace ws ) {
        try {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setContent( filePath );
            scmfile.setFileName( fileName + "_" + UUID.randomUUID() );
            fileId = scmfile.save();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void AsyncCacheFromMainCenter() throws ScmException {
        try {
            ScmFactory.File.asyncCache( wsM, fileId );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.OPERATION_UNSUPPORTED != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    private void checkResult() {
        try {
            SiteWrapper[] expSiteIdList = { branceSite };
            ScmTaskUtils.waitAsyncTaskFinished( wsA, fileId,
                    expSiteIdList.length );
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteIdList,
                    localPath, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}
