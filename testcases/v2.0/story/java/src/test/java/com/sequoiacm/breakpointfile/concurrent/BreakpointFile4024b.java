/**
 *
 */
package com.sequoiacm.breakpointfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testresource.SkipTestException;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @description SCM-4024:并发创建同一文件
 * @author zhangYanan
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class BreakpointFile4024b extends TestScmBase {
    private static SiteWrapper site1 = null;
    private static SiteWrapper site2 = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "scmfile4024b";
    private int fileSize = 1024 * 1024 * 10;
    private File localPath = null;
    private String filePath = null;
    private String checkfilePath = null;
    private boolean runSuccess = false;
    private int threadSuccessCount = 0;

    @BeforeClass()
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils
                .checkDBAndCephS3DataSource();
        if ( sites.size() < 2 ) {
            throw new SkipTestException( "指定类型站点数量不足！" );
        }
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        checkfilePath = localPath + File.separator + "localFile_check"
                + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site1 = sites.get( 0 );
        site2 = sites.get( 1 );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site1 );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp.getName(), cond );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new UploadBreakpointFileThread( site1 ) );
        es.addWorker( new UploadBreakpointFileThread( site2 ) );
        es.run();
        Assert.assertEquals( threadSuccessCount, 1 );
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
        private SiteWrapper site;

        public UploadBreakpointFileThread( SiteWrapper site ) {
            this.site = site;
        }

        @ExecuteOrder(step = 1)
        private void uploadBreakpointFileThread()
                throws ScmException, IOException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .createInstance( ws, fileName );
                breakpointFile.upload( new File( filePath ) );
                // 检查上传文件MD5
                BreakpointUtil.checkScmFile( ws, fileName, filePath, checkfilePath );
                threadSuccessCount++;
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_EXIST.getErrorCode() ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
