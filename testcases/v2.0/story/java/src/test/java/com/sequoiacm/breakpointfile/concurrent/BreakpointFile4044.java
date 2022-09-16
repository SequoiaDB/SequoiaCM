/**
 *
 */
package com.sequoiacm.breakpointfile.concurrent;

import java.io.*;
import java.util.List;
import java.util.Random;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.element.ScmBreakpointFileOption;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;

/**
 * @Description SCM-4044:并发续传同一个文件（缓存区内有数据）
 * @Author zhangyanan
 * @Date 2021.11.5
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.11.5
 * @version 1.00
 */
public class BreakpointFile4044 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "scmfile4044";
    private int fileSize = 1024 * 1024 * 9;
    private int fristUploadSize = 1024 * 1024 * 3;
    private File localPath = null;
    private String filePath = null;
    private String checkfilePath = null;
    private boolean runSuccess = false;
    private int threadSuccessCount = 0;

    @BeforeClass()
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils
                .checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        checkfilePath = localPath + File.separator + "localFile_check"
                + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        createBreakpointFile();
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite, GroupTags.fourSite })
    private void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new ContinuesBreakpointFileThread() );
        es.addWorker( new ContinuesBreakpointFileThread() );
        es.run();
        Assert.assertEquals( threadSuccessCount, 1 );
        // 检查上传文件MD5
        BreakpointUtil.checkScmFile( ws, fileName, filePath, checkfilePath );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess || TestScmBase.forceClear ) {
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
                breakpointFile.incrementalUpload( fStream, true );
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

    public void createBreakpointFile() throws ScmException, IOException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, new ScmBreakpointFileOption(),
                        ScmType.BreakpointFileType.BUFFERED );
        InputStream inputStream = new BreakpointStream( filePath,
                fristUploadSize );
        breakpointFile.incrementalUpload( inputStream, false );
        inputStream.close();
    }

}
