/**
 *
 */
package com.sequoiacm.breakpointfile.concurrent;

import java.io.File;
import java.io.IOException;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description BreakpointFile1399.java, 并发创建和删除同一断点文件
 * @author luweikang
 * @date 2018年5月22日
 */
public class BreakpointFile1399 extends TestScmBase {

    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean deleteSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile1399";
    private int fileSize = 1024 * 1024 * 60;
    private File localPath = null;
    private String filePath = null;
    private String checkFilePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        checkFilePath = localPath + File.separator + "localFile_check"
                + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException,
            IOException {
        CreateBreakpointFileThread createThread = new CreateBreakpointFileThread();
        createThread.start();

        Thread.sleep( 3000 );

        DeleteBreakpointFileThread deleteThread = new DeleteBreakpointFileThread();
        deleteThread.start();

        deleteSuccess = deleteThread.isSuccess();
        checkBreakpointFile( createThread.isSuccess(), deleteSuccess );

    }

    @AfterClass
    private void tearDown() {
        try {
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkBreakpointFile( boolean createResult,
            boolean deleteResult ) throws ScmException, IOException {
        ScmCursor< ScmBreakpointFile > cursor = ScmFactory.BreakpointFile
                .listInstance( ws,
                        new BasicBSONObject( "file_name", fileName ) );
        if ( createResult ) {
            if ( deleteResult ) {
                if ( cursor.hasNext() ) {
                    Assert.fail( "delete success breakFile should not exist" );
                }
            } else {
                if ( !cursor.hasNext() ) {
                    Assert.fail( "breakFile should be exist" );
                }
                BreakpointUtil.checkScmFile( ws, fileName, filePath,
                        checkFilePath );
            }
        } else {
            if ( cursor.hasNext() ) {
                Assert.fail( "create fail breakFile should not exist" );
            }
        }
        cursor.close();
    }

    private class CreateBreakpointFileThread extends TestThreadBase {

        @Override
        public void exec() throws ScmException {
            ScmBreakpointFile breakpointFile;
            breakpointFile = ScmFactory.BreakpointFile.createInstance( ws,
                    fileName, ScmChecksumType.ADLER32 );
            breakpointFile.upload( new File( filePath ) );
        }

    }

    private class DeleteBreakpointFileThread extends TestThreadBase {

        @Override
        public void exec() throws ScmException {
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
        }

    }
}
