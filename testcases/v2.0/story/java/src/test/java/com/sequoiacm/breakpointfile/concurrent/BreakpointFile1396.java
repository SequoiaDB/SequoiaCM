/**
 *
 */
package com.sequoiacm.breakpointfile.concurrent;

import java.io.File;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONException;
import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description BreakpointFile1396.java, 并发续传同一个文件
 * @author luweikang
 * @date 2018年5月21日
 */
public class BreakpointFile1396 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile1396";
    private int fileSize = 1024 * 1024 * 10;
    private File localPath = null;
    private String filePath = null;
    private String checkfilePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        checkfilePath =
                localPath + File.separator + "localFile_check" + fileSize +
                        ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws JSONException, ScmException, IOException {

        BreakpointUtil.createBreakpointFile( ws, filePath, fileName, 1024 * 512,
                ScmChecksumType.CRC32 );

        UploadBreakpointFileThread thread = new UploadBreakpointFileThread();
        thread.start( 10 );
        thread.isSuccess();
        //检查上传文件MD5
        BreakpointUtil.checkScmFile( ws, fileName, filePath, checkfilePath );

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

    private class UploadBreakpointFileThread extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            ScmSession session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), session );
            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .getInstance( ws, fileName );
            try {
                breakpointFile.upload( new File( filePath ) );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getErrorCode(),
                        ScmError.OUT_OF_BOUND.getErrorCode(),
                        "upload breakpointFile" );
            } finally {
                session.close();
            }
        }
    }

}
