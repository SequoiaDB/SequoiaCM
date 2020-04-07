package com.sequoiacm.asynctask;

import java.io.File;
import java.io.IOException;
import java.util.Random;
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
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-522: asyncCache参数校验
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * ScmFactory.File.asyncCache接口参数校验： 1）有效参数：ws: 存在 fileid：存在 2）无效参数：ws: 不存在、null
 * fileid：不存在、null 2、检查校验结果
 */
public class AsyncCache_param_asyncCache522 extends TestScmBase {
    private static final String fileName = "AsyncCache522";
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private boolean runSuccess3 = false;
    private boolean runSuccess4 = false;
    private int fileSize = new Random().nextInt( 1024 ) + 1024;
    private File localPath = null;
    private ScmId fileId = null;
    private String filePath;
    private BSONObject cond = null;
    private ScmSession sessionA = null;
    private ScmWorkspace ws = null;

    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            // clean file
            cond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                    .is( fileName ).get();
            ScmFileUtils.cleanFile( ws_T, cond );
            // login in
            sessionA = TestScmTools.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
            writeFileFromMainCenter();
        } catch ( ScmException | IOException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testWsNoExist() throws ScmException {
        try {
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( "test503", sessionA );
            ScmFactory.File.asyncCache( ws, fileId );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.WORKSPACE_NOT_EXIST != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
        runSuccess1 = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testWSIsNULL() throws ScmException {
        try {
            ScmFactory.File.asyncCache( null, fileId );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
        runSuccess2 = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testfileIdIsNull() throws ScmException {
        try {
            ScmFactory.File.asyncCache( ws, null );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
        runSuccess3 = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testfileIdNotExist() throws ScmException {
        try {
            ScmId fileId = new ScmId( "00ff00ff00ff00ff00ff00ff" );
            ScmFactory.File.asyncCache( ws, fileId );
            Assert.fail( "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
        runSuccess4 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( ( runSuccess1 && runSuccess2 && runSuccess3 && runSuccess4 ) ||
                    forceClear ) {
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

    private void writeFileFromMainCenter() {
        try {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setContent( filePath );
            scmfile.setFileName( fileName + "_" + UUID.randomUUID() );
            fileId = scmfile.save();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}
