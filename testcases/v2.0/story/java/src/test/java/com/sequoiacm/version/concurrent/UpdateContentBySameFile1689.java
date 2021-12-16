package com.sequoiacm.version.concurrent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1689:并发使用文件和断点文件方式更新相同文件
 * @author wuyan
 * @createDate 2018.06.13
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class UpdateContentBySameFile1689 extends TestScmBase {
    private static WsWrapper wsp = null;
    private final int FILE_SIZE = 1024 * 800;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsM = null;
    private int updateFileThreadStatus = 0;
    private int updateBreakPointFileThreadStatus = 0;
    private ScmId fileId = null;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "versionfile1689";
    private String authorName = "author1689";
    private byte[] writeData = new byte[ 1024 * 20 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        VersionUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + FILE_SIZE
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, FILE_SIZE );

        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = VersionUtils.createFileByStream( wsM, fileName, writeData,
                authorName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int updateSize = 1024 * 900;
        byte[] updateData = new byte[ updateSize ];
        createBreakPointFile( wsA, updateData );
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new UpdateContentByBreakpointFileThread() );
        es.addWorker( new UpdateContentByFileThread() );
        es.run();
        if ( updateFileThreadStatus == 1
                && updateBreakPointFileThreadStatus == 0 ) {
            checkUpdateByFileResult( wsM );
        } else if ( updateFileThreadStatus == 0
                && updateBreakPointFileThreadStatus == 1 ) {
            checkUpdateByBreakpointfileResult( wsM, updateData );
        } else if ( updateFileThreadStatus == 1
                && updateBreakPointFileThreadStatus == 1 ) {
            checkAllUpdateContentResult( wsM, updateData );
        } else {
            Assert.fail( "expected is least one thread to succeed" );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
            }
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void createBreakPointFile( ScmWorkspace ws, byte[] updateData )
            throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        new Random().nextBytes( updateData );
        breakpointFile.upload( new ByteArrayInputStream( updateData ) );
    }

    private void checkAllUpdateContentResult( ScmWorkspace ws,
            byte[] updatedata ) throws Exception {
        int historyVersion1 = 1;
        // first updateContent version
        int historyVersion2 = 2;
        // second updateContent version
        int currentVersion = 3;

        ScmFile file = ScmFactory.File.getInstance( ws, fileId, currentVersion,
                0 );
        long fileSize = file.getSize();

        // check the updateContent
        if ( fileSize == updatedata.length ) {
            VersionUtils.CheckFileContentByStream( ws, fileName, currentVersion,
                    updatedata );
            VersionUtils.CheckFileContentByFile( ws, fileId, historyVersion2,
                    filePath, localPath );
        } else if ( fileSize == FILE_SIZE ) {
            VersionUtils.CheckFileContentByFile( ws, fileId, currentVersion,
                    filePath, localPath );
            VersionUtils.CheckFileContentByStream( ws, fileName,
                    historyVersion2, updatedata );
        } else {
            Assert.fail( "update file content is error!" );
        }
        // check the write content
        VersionUtils.CheckFileContentByStream( ws, fileName, historyVersion1,
                writeData );
    }

    private void checkUpdateByBreakpointfileResult( ScmWorkspace ws,
            byte[] updatedata ) throws Exception {
        int historyVersion = 1;
        int currentVersion = 2;
        VersionUtils.CheckFileContentByStream( ws, fileName, currentVersion,
                updatedata );
        VersionUtils.CheckFileContentByStream( ws, fileName, historyVersion,
                writeData );

        // check the breakpoint is not exist
        try {
            ScmFactory.BreakpointFile.getInstance( ws, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                throw e;
            }
        }
    }

    private void checkUpdateByFileResult( ScmWorkspace ws ) throws Exception {
        int historyVersion = 1;
        int currentVersion = 2;
        VersionUtils.CheckFileContentByFile( ws, fileId, currentVersion,
                filePath, localPath );
        VersionUtils.CheckFileContentByStream( ws, fileName, historyVersion,
                writeData );
    }

    private class UpdateContentByBreakpointFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .getInstance( ws, fileName );
                file.updateContent( breakpointFile );
                updateBreakPointFileThreadStatus++;
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND
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

    private class UpdateContentByFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                VersionUtils.updateContentByFile( ws, fileName, fileId,
                        filePath );
                updateFileThreadStatus++;
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_VERSION_MISMATCHING
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
}