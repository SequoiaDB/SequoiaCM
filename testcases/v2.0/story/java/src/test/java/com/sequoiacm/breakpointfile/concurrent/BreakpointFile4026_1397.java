/**
 *
 */
package com.sequoiacm.breakpointfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @descreption SCM-4026:并发续传不同文件 SCM-1397:并发续传不同文件
 * @author YiPan
 * @date 2021/10/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BreakpointFile4026_1397 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private int fileSize = 1024 * 1024 * 10;
    private File localPath = null;
    private String filePath = null;
    private String checkFilePath = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        checkFilePath = localPath + File.separator + "localFile_check"
                + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        BreakpointUtil.createBreakpointFile( ws, filePath, "TestFile1392_3",
                1024 * 1024 * 5, ScmChecksumType.ADLER32 );
        BreakpointUtil.createBreakpointFile( ws, filePath, "TestFile1392_4",
                1024 * 1024 * 10, ScmChecksumType.CRC32 );

        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new CreateBreakpointFileThread( "TestFile1392_1" ) );
        t.addWorker( new CreateBreakpointFileThread( "TestFile1392_2" ) );
        t.addWorker( new UploadBreakpointFileThread( "TestFile1392_3" ) );
        t.addWorker( new UploadBreakpointFileThread( "TestFile1392_4" ) );
        t.run();

        BreakpointUtil.checkScmFile( ws, "TestFile1392_1", filePath,
                checkFilePath );
        BreakpointUtil.checkScmFile( ws, "TestFile1392_2", filePath,
                checkFilePath );
        BreakpointUtil.checkScmFile( ws, "TestFile1392_3", filePath,
                checkFilePath );
        BreakpointUtil.checkScmFile( ws, "TestFile1392_4", filePath,
                checkFilePath );
        runSuccess = true;
    }

    @AfterClass
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

    private class UploadBreakpointFileThread {
        private String name;

        public UploadBreakpointFileThread( String name ) {
            this.name = name;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .getInstance( ws, name );
            breakpointFile.upload( new File( filePath ) );
        }
    }

    private class CreateBreakpointFileThread {

        private String name;

        public CreateBreakpointFileThread( String name ) {
            this.name = name;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .createInstance( ws, name );
            breakpointFile.upload( new File( filePath ) );
        }
    }
}
