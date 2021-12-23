package com.sequoiacm.version.concurrent;

import java.io.ByteArrayInputStream;
import java.util.Random;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1691:并发使用断点文件更新和删除相同文件
 * @author wuyan
 * @createDate 2018.06.15
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class UpdateAndDeleteFile1691 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionA = null;
    private ScmId fileId = null;
    private int deleteThreadStatus = 0;
    private int updateThreadStatus = 0;
    private String fileName = "versionfile1691";
    private String authorName = "author1691";
    private byte[] writeData = new byte[ 1024 * 20 ];

    @BeforeClass
    private void setUp() throws ScmException {
        BreakpointUtil.checkDBDataSource();
        branSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = VersionUtils.createFileByStream( wsA, fileName, writeData,
                authorName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int updateSize = 1024 * 1024;
        byte[] updateData = new byte[ updateSize ];
        createBreakPointFile( wsA, updateData );
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new DeleteFileThread() );
        es.addWorker( new UpdateFileThread() );
        es.run();
        if ( updateThreadStatus == 1 && deleteThreadStatus == 0 ) {
            checkUpdateFileResult( wsA, updateData );
        } else if ( updateThreadStatus == 0 && deleteThreadStatus == 1 ) {
            checkDeleteFileResult( wsA );
        } else if ( updateThreadStatus == 1 && deleteThreadStatus == 1 ) {
            checkAllResult( wsA );
        } else {
            Assert.fail( "Only One thread is expected to succeed" );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( ( runSuccess || TestScmBase.forceClear )
                    && deleteThreadStatus == 0 ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        } finally {
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

    private void checkDeleteFileResult( ScmWorkspace ws ) throws Exception {
        // check the FILE is not exist
        try {
            ScmFactory.File.getInstanceByPath( ws, fileName );
            Assert.fail( "get file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                throw e;
            }
        }

        // count histroy and current version file are not exist
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        long count = ScmFactory.File.countInstance( ws, ScopeType.SCOPE_ALL,
                condition );
        long expFileConut = 0;
        Assert.assertEquals( count, expFileConut );
        ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
    }

    private void checkUpdateFileResult( ScmWorkspace ws, byte[] expUpdateData )
            throws Exception {
        int historyVersion = 1;
        int currentVersion = 2;
        VersionUtils.CheckFileContentByStream( ws, fileName, currentVersion,
                expUpdateData );
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

    private void checkAllResult( ScmWorkspace ws ) throws ScmException {
        try {
            ScmFactory.File.getInstance( ws, fileId );
            Assert.fail( "get file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                throw e;
            }
        }
        try {
            ScmFactory.BreakpointFile.getInstance( ws, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                throw e;
            }
        }
    }

    private class DeleteFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.File.deleteInstance( ws, fileId, true );
                deleteThreadStatus++;
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

    private class UpdateFileThread extends ResultStore {
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
                updateThreadStatus++;
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
}