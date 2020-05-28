package com.sequoiacm.asynctask;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
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
 * @FileName SCM-487:分中心不存在但主中心存在
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * 1、在分中心B异步迁移单个文件； 2、检查执行结果正确性；
 */
public class AsyncTransfer_inRootSite487 extends TestScmBase {

    private boolean runSuccess = false;
    private int fileSize = 100;
    private File localPath = null;
    private String filePath = null;
    private ScmId fileId = null;
    private String fileName = "AsyncTransferWhenMExistFile487";
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

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            // login in
            sessionM = TestScmTools.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );

            writeFileFromMainCenter( wsM, filePath );
        } catch ( ScmException | IOException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            asyncTransferFromSubCenterA();
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
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void writeFileFromMainCenter( ScmWorkspace ws, String filePath ) {
        try {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setContent( filePath );
            scmfile.setFileName( fileName + "_" + UUID.randomUUID() );
            fileId = scmfile.save();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void asyncTransferFromSubCenterA() throws ScmException {
        ScmSession sessionB = null;
        try {
            sessionB = TestScmTools.createSession( branceSite );
            ScmWorkspace wsB = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), sessionB );
            ScmFactory.File.asyncTransfer( wsB, fileId );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.DATA_NOT_EXIST != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        } finally {
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    private void checkResult() {
        try {
            SiteWrapper[] expSiteList = { rootSite };
            ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId,
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}
