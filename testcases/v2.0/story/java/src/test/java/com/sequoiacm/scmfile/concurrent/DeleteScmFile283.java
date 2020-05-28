package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.IOException;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
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
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-283:同一个中心并发删除同一个文件 1、文件在主中心 2、多线程并发在主中心删除文件
 * @author huangxiaoni init
 * @date 2017.5.24
 */

public class DeleteScmFile283 extends TestScmBase {
    private static WsWrapper wsp = null;
    private static ScmSession sessionM = null;
    private static ScmSession sessionA = null;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private ScmWorkspace wsM = null;
    private ScmWorkspace wsA = null;

    private String fileName = "delete283";
    private ScmId mFileId = null;
    private ScmId aFileId = null;
    private int fileSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branSite = ScmInfo.getBranchSite();
            wsp = ScmInfo.getWs();

            sessionM = TestScmTools.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
            sessionA = TestScmTools.createSession( branSite );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( fileName + "_1" )
                    .get();
            ScmFileUtils.cleanFile( wsp, cond );
            BSONObject cond1 = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( fileName + "_2" )
                    .get();
            ScmFileUtils.cleanFile( wsp, cond1 );
            mFileId = ScmFileUtils.create( wsA, fileName + "_1", filePath );
            aFileId = ScmFileUtils.create( wsM, fileName + "_2", filePath );
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            DeleteFromM deleteFromM = new DeleteFromM();
            deleteFromM.start( 20 );
            DeleteFromA deleteFromA = new DeleteFromA();
            deleteFromA.start( 20 );

            if ( !( deleteFromM.isSuccess() && deleteFromA.isSuccess() ) ) {
                Assert.fail(
                        deleteFromM.getErrorMsg() + deleteFromA.getErrorMsg() );
            }

            checkResults( wsA, mFileId ); // mFileId is written in siteA
            checkResults( wsM, aFileId ); // aFileId is written in siteM
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void checkResults( ScmWorkspace ws, ScmId fileId )
            throws Exception {
        try {
            BSONObject cond = new BasicBSONObject( "id", fileId.get() );
            long cnt = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( cnt, 0 );

            ScmFileUtils.checkData( ws, fileId, localPath, filePath );
            Assert.assertFalse( true,
                    "File is unExisted, except throw e, but success." );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND,
                    e.getMessage() );
        }
    }

    private class DeleteFromM extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                ScmFactory.File.getInstance( ws, mFileId ).delete( true );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND,
                        e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteFromA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                ScmFactory.File.getInstance( ws, aFileId ).delete( true );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND,
                        e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
