package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @Testcase: SCM-287:读取文件内容过程中删除文件 1、文件已存在 2、A线程读取文件内容 3、B线程删除该文件
 * @author huangxiaoni init
 * @date 2017.5.24
 */

public class DeleteScmFile287 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String author = "delete287";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 3;
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

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );
            fileId = ScmFileUtils.create( ws, author, filePath );
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void test() {
        try {
            ReadScmFile readScmFile = new ReadScmFile();
            readScmFile.start( 20 );

            DeleteScmFile deleteScmFile = new DeleteScmFile();
            Thread.sleep( new Random().nextInt( 1000 ) );
            deleteScmFile.start( 20 );

            if ( !( readScmFile.isSuccess() && deleteScmFile.isSuccess() ) ) {
                Assert.fail( readScmFile.getErrorMsg()
                        + deleteScmFile.getErrorMsg() );
            }

            checkResults();

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

    private class ReadScmFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                String dwPath = TestTools.LocalFile.initDownloadPath( localPath,
                        TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                file.getContent( dwPath );
                // check results
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( dwPath ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DATA_ERROR
                        && e.getError() != ScmError.FILE_NOT_FOUND
                        && e.getError() != ScmError.DATA_NOT_EXIST
                        && e.getError() != ScmError.DATA_UNAVAILABLE
                        && e.getError() != ScmError.DATA_CORRUPTED ) {
                    System.out.println( "read file, fileId = " + fileId.get()
                            + ", errno = " + e.getErrorCode() );
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteScmFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // delete
                ScmFactory.File.getInstance( ws, fileId ).delete( true );
            } catch ( ScmException e ) {
                if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                    System.out.println( "delete file, fileId = " + fileId.get()
                            + ", errno = " + e.getErrorCode() );
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
