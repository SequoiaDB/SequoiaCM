package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.IOException;

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
 * @Testcase: SCM-289:获取文件实例过程中删除文件 1、文件已存在 2、A线程读取文件内容 3、B线程删除该文件
 * @author huangxiaoni init
 * @date 2017.5.24
 */

public class DeleteScmFile289 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "delete289";
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
                    .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                    .get();
            ScmFileUtils.cleanFile( wsp, cond );
            fileId = ScmFileUtils.create( ws, fileName, filePath );
        } catch ( IOException | ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void test() {
        try {
            GetInstance getFileInstance = new GetInstance();
            getFileInstance.start( 20 );

            DeleteScmFile deleteScmFile = new DeleteScmFile();
            deleteScmFile.start( 10 );

            if ( !( getFileInstance.isSuccess()
                    && deleteScmFile.isSuccess() ) ) {
                Assert.fail( getFileInstance.getErrorMsg()
                        + deleteScmFile.getErrorMsg() );
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

    private class GetInstance extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read
                while ( true ) {
                    ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                    file.getAuthor();
                }
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
                    e.printStackTrace();
                    Assert.fail( "delete file, fileId = " + fileId.get() + ", "
                            + e.getMessage() );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
