/**
 *
 */
package com.sequoiacm.breakpointfile.concurrent;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @description SCM-4025:并发续传同一文件
 * @author zhangYanan
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class BreakpointFile4025 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "scmfile4025a";
    private int fileSize = 1024 * 1024 * 10;
    private File localPath = null;
    private String filePath = null;
    private String checkfilePath = null;
    private AtomicInteger runSuccessCount = new AtomicInteger( 0 );
    private int threadSuccessCount = 0;

    @BeforeClass()
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        checkfilePath = localPath + File.separator + "localFile_check"
                + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @DataProvider(name = "dataProvider")
    public Object[][] generateDate() {
        return new Object[][] { { ScmChecksumType.ADLER32 },
                { ScmChecksumType.NONE } };
    }

    @Test(groups = { "oneSite", "twoSite",
            "fourSite" }, dataProvider = "dataProvider")
    private void test( ScmChecksumType checksumType ) throws Exception {
        threadSuccessCount = 0;
        createBreakpointFile( checksumType );
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new ContinuesBreakpointFileThread() );
        es.addWorker( new ContinuesBreakpointFileThread() );
        es.run();
        Assert.assertEquals( threadSuccessCount, 1 );
        // 检查上传文件MD5
        BreakpointUtil.checkScmFile( ws, fileName, filePath, checkfilePath );
        runSuccessCount.incrementAndGet();
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccessCount.get() == generateDate().length
                || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class ContinuesBreakpointFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void continuesBreakpointFileThread()
                throws ScmException, FileNotFoundException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .getInstance( ws, fileName );
                FileInputStream fStream = new FileInputStream( filePath );
                breakpointFile.upload( fStream );
                threadSuccessCount++;
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT
                        .getErrorCode() ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    public void createBreakpointFile( ScmChecksumType checksumType )
            throws ScmException, IOException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, checksumType );
        InputStream inputStream = new BreakpointStream( filePath,
                1024 * 1024 * 5 );
        breakpointFile.incrementalUpload( inputStream, false );
        inputStream.close();
    }
}
