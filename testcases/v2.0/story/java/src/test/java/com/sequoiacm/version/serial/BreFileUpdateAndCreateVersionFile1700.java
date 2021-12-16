/**
 *
 */
package com.sequoiacm.version.serial;

import java.io.File;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1700:并发使用断点文件更新和创建新文件
 * @author luweikang
 * @createDate 2018.06.15
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class BreFileUpdateAndCreateVersionFile1700 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private ScmSession sessionA = null;
    private int updateThreadStatus = 0;
    private int createThreadStatus = 0;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId1 = null;
    private ScmId fileId2 = null;
    private ScmBreakpointFile sbFile = null;
    private String fileName1 = "fileVersion1700_1";
    private String fileName2 = "fileVersion1700_2";
    private String fileAuthor = "fileAuthorVersion1700_1";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass()
    private void setUp() throws ScmException {
        BreakpointUtil.checkDBDataSource();
        branSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId1 = VersionUtils.createFileByStream( wsA, fileName1, filedata,
                fileAuthor );
        sbFile = VersionUtils.createBreakpointFileByStream( wsA, fileName1,
                updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new CreateFileThread() );
        es.addWorker( new UpdateFileThread() );
        es.run();
        if ( updateThreadStatus == 1 && createThreadStatus == 0 ) {
            VersionUtils.CheckFileContentByStream( wsA, fileName1,
                    currentVersion, updatedata );
        } else if ( updateThreadStatus == 0 && createThreadStatus == 1 ) {
            checkFile();
            VersionUtils.CheckFileContentByStream( wsA, fileName1,
                    historyVersion, filedata );
        } else {
            Assert.fail( "Only One thread is expected to succeed" );
        }
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId1, true );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private class UpdateFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId1 );
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .getInstance( ws, fileName1 );
                file.updateContent( breakpointFile );
                updateThreadStatus++;
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode()
                        && e.getErrorCode() != ScmError.INVALID_ARGUMENT
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

    private class CreateFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( sbFile );
                file.setFileName( fileName2 );
                file.setTitle( fileName2 );
                fileId2 = file.save();
                createThreadStatus++;
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

    public void checkFile() throws Exception {
        // down file
        File filePath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        String downloadPath = TestTools.LocalFile.initDownloadPath( filePath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        ScmFile file = ScmFactory.File.getInstance( wsA, fileId2 );
        file.getContent( downloadPath );

        // check results
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( updatedata ) );
        TestTools.LocalFile.removeFile( downloadPath );
        ScmFactory.File.deleteInstance( wsA, fileId2, true );
    }
}
