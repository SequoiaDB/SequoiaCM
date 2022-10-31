/**
 *
 */
package com.sequoiacm.breakpointfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;

/**
 * @description SCM-4024:并发创建同一文件
 * @author zhangYanan
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class BreakpointFile4024a extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "scmfile4024a";
    private int fileSize = 1024 * 1024 * 10;
    private File localPath = null;
    private String filePath = null;
    private String checkfilePath = null;
    private boolean runSuccess = false;
    private int threadSuccessCount = 0;

    @BeforeClass()
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
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

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new UploadBreakpointFileThread() );
        es.addWorker( new UploadBreakpointFileThread() );
        es.run();
        Assert.assertEquals(threadSuccessCount,1);
        // 检查上传文件MD5
        BreakpointUtil.checkScmFile( ws, fileName, filePath, checkfilePath );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
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

    private class UploadBreakpointFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void uploadBreakpointFileThread() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .createInstance( ws, fileName );
                breakpointFile.upload(new File(filePath));
                threadSuccessCount++;
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_EXIST.getErrorCode() ) {
                    throw e;
                }
            }finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
