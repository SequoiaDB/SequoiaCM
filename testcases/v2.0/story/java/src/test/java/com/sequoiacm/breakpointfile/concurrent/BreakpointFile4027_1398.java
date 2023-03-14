package com.sequoiacm.breakpointfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @descreption SCM-4027:并发删除断点文件 SCM-1398:并发删除断点文件
 * @author YiPan
 * @date 2021/10/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BreakpointFile4027_1398 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "file4027";
    private int fileSize = 1024 * 1024 * 10;
    private File localPath = null;
    private String filePath = null;
    private AtomicInteger sameDeleteOKCount = new AtomicInteger( 0 );
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws ScmException, IOException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        createBreakPointFile();
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    private void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new DeleteBreakpointFileThread() );
        t.run();
        // delete the same breakpointfile only one success
        int expSuccessNum = 1;
        Assert.assertEquals( sameDeleteOKCount.get(), expSuccessNum );
        checkResult();
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

    private void createBreakPointFile() throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, ScmChecksumType.ADLER32 );
        breakpointFile.upload( new File( filePath ) );
    }

    private void checkResult() throws Exception {
        // the breakpointfile is not exist
        try {
            ScmFactory.BreakpointFile.getInstance( ws, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                throw e;
            }
        }
    }

    private class DeleteBreakpointFileThread {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            try (ScmSession session = ScmSessionUtils.createSession( site );) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName );

                // recorded the numbers of upload file successful
                sameDeleteOKCount.getAndIncrement();
            } catch ( ScmException e ) {
                if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                    throw e;
                }
            }
        }
    }

}