package com.sequoiacm.version.concurrent;

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
import com.sequoiacm.client.common.ScmType.ScopeType;
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
 * @description SCM-1690:并发更新和删除相同文件
 * @author wuyan
 * @createDate 2018.06.15
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class UpdateAndDeleteFile1690 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;
    private int deleteThreadStatus = 0;
    private int updateThreadStatus = 0;
    private String fileName = "versionfile1690";
    private String authorName = "author1690";
    private byte[] writeData = new byte[ 1024 * 20 ];

    @BeforeClass
    private void setUp() throws ScmException {
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
        int updateSize = 1024 * 100;
        byte[] updateData = new byte[ updateSize ];
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new DeleteFileThread() );
        es.addWorker( new UpdateFileThread( updateData ) );
        es.run();
        if ( updateThreadStatus == 1 && deleteThreadStatus == 0 ) {
            checkUpdateFileResult( wsM, updateData );
        } else if ( updateThreadStatus == 0 && deleteThreadStatus == 1 ) {
            checkDeleteFileResult( wsA );
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
    }

    private void checkUpdateFileResult( ScmWorkspace ws, byte[] expUpdateData )
            throws Exception {
        int historyVersion = 1;
        int currentVersion = 2;
        VersionUtils.CheckFileContentByStream( ws, fileName, currentVersion,
                expUpdateData );
        VersionUtils.CheckFileContentByStream( ws, fileName, historyVersion,
                writeData );
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
        private byte[] updateData;

        public UpdateFileThread( byte[] updateData ) {
            this.updateData = updateData;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                VersionUtils.updateContentByStream( ws, fileId, updateData );
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