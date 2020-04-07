package com.sequoiacm.session.concurrent;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmFactory;
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
 * @FileName SCM-229: 多线程登入同一用户，A线程登出，B线程继续操作
 * @Author linsuqiang
 * @Date 2017-06-13
 * @Version 1.00
 */

/*
 * 1、多线程登入同一用户，A线程登出，B线程继续操作 2、检查操作结果
 */

public class OprAndLogout229 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private final int FILE_SIZE = 100;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;
    private String fileName = "session229";
    private File localPath = null;
    private String filePath = null;
    private ScmId fileId = null;
    private AtomicInteger logoutThdIsDone = new AtomicInteger();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + FILE_SIZE + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, FILE_SIZE );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            fileId = ScmFileUtils.create( ws, fileName, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        ScmSession sessionB = null;
        try {
            LogoutThread logoutThd = new LogoutThread();
            DeleteThread deleteThd = new DeleteThread();

            logoutThd.start();
            deleteThd.start();

            if ( !( deleteThd.isSuccess() && logoutThd.isSuccess() ) ) {
                Assert.fail(
                        deleteThd.getErrorMsg() + logoutThd.getErrorMsg() );
            }
            this.checkResults();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
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
            long cnt = ScmFactory.File
                    .countInstance( ws, ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( cnt, 0 );

            ScmFileUtils.checkData( ws, fileId, localPath, filePath );
            Assert.assertFalse( true,
                    "File is unExisted, except throw e, but success." );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.FILE_NOT_FOUND.getErrorCode(), e.getMessage() );
        }
    }

    private class LogoutThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
            } catch ( Exception e ) {
                logoutThdIsDone.set( 2 );
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
            logoutThdIsDone.set( 1 );
            // System.out.println("debug: logout flag is true!");
        }
    }

    private class DeleteThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                // System.out.println("debug: begin to wait");
                while ( true ) {
                    if ( logoutThdIsDone.get() == 1 ) {
                        break;
                    }
                    if ( logoutThdIsDone.get() == 2 ) {
                        throw new Exception( "LogoutThread is fail" );
                    }
                }
                // System.out.println("debug: wait finish");
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.File.getInstance( ws, fileId ).delete( true );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}