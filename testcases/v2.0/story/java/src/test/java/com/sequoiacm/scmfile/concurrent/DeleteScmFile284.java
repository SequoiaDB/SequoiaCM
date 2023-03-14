package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-284:不同中心并发删除同一个文件 1、文件在主中心 2、多线程并发在主中心删除文件
 * @author huangxiaoni init
 * @date 2017.5.24
 */

public class DeleteScmFile284 extends TestScmBase {
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private ScmWorkspace ws = null;

    private String fileName = "delete284";
    private ScmId fileId = null;
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

            rootSite = ScmInfo.getSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( rootSite );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( fileName ).get();
            ScmFileUtils.cleanFile( wsp, cond );
            fileId = ScmFileUtils.create( ws, fileName, filePath );
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            DeleteFromM deleteFromM = new DeleteFromM();
            deleteFromM.start( 10 );

            DeleteFromA deleteFromA = new DeleteFromA();
            deleteFromA.start( 10 );

            DeleteFromB deleteFromB = new DeleteFromB();
            deleteFromB.start( 10 );

            if ( !( deleteFromM.isSuccess() && deleteFromA.isSuccess()
                    && deleteFromB.isSuccess() ) ) {
                Assert.fail(
                        deleteFromM.getErrorMsg() + deleteFromA.getErrorMsg()
                                + deleteFromB.getErrorMsg() );
            }

            checkResults();

        } catch ( Exception e ) {
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
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void checkResults() throws Exception {
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
                // login
                session = ScmSessionUtils.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // delete
                ScmFactory.File.getInstance( ws, fileId ).delete( true );
            } catch ( ScmException e ) {
                Assert.assertEquals( ScmError.FILE_NOT_FOUND, e.getError(),
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
                session = ScmSessionUtils.createSession( branSites.get( 0 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                ScmFactory.File.getInstance( ws, fileId ).delete( true );
            } catch ( ScmException e ) {
                Assert.assertEquals( ScmError.FILE_NOT_FOUND, e.getError(),
                        e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteFromB extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( branSites.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.delete( true );
            } catch ( ScmException e ) {
                Assert.assertEquals( ScmError.FILE_NOT_FOUND, e.getError(),
                        e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
