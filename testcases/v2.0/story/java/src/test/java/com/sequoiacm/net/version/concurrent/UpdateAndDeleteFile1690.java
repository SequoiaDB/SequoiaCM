package com.sequoiacm.net.version.concurrent;

import java.io.IOException;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
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
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content: update Content and delete the same file concurrently: a.update
 * content b.delete the file testlink-case:SCM-1690
 *
 * @author wuyan
 * @Date 2018.06.15
 * @version 1.00
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

    private String fileName = "versionfile1690";
    private String authorName = "author1690";
    private byte[] writeData = new byte[ 1024 * 20 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        wsp = ScmInfo.getWs();
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();

        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

        fileId = VersionUtils.createFileByStream( wsM, fileName, writeData,
                authorName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int updateSize = 1024 * 100;
        byte[] updateData = new byte[ updateSize ];

        DeleteFile deleteFile = new DeleteFile();
        UpdateFile updateFile = new UpdateFile( updateData );
        deleteFile.start();
        updateFile.start();

        if ( deleteFile.isSuccess() ) {
            if ( !updateFile.isSuccess() ) {
                Assert.assertTrue( !updateFile.isSuccess(),
                        updateFile.getErrorMsg() );
                ScmException e = ( ScmException ) updateFile.getExceptions()
                        .get( 0 );
                Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND,
                        "updateContent by file fail:"
                                + updateFile.getErrorMsg() );
                checkDeleteFileResult( wsA );
            } else if ( updateFile.isSuccess() ) {
                checkDeleteFileResult( wsA );
            } else {
                Assert.fail(
                        "the results can only by updated successfully or one "
                                + "update succeeds" );
            }
        } else if ( !deleteFile.isSuccess() ) {
            Assert.assertTrue( updateFile.isSuccess(),
                    updateFile.getErrorMsg() );
            ScmException e = ( ScmException ) deleteFile.getExceptions()
                    .get( 0 );
            Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND,
                    "delete file fail:" + deleteFile.getErrorMsg() );

            checkUpdateFileResult( wsM, updateData );
        }

        runSuccess = true;

    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                try {
                    ScmFactory.File.deleteInstance( wsM, fileId, true );
                } catch ( ScmException e ) {
                    Assert.assertEquals( e.getError(),
                            ScmError.FILE_NOT_FOUND );
                }
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
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
                Assert.fail( "expErrorCode:-262  actError:" + e.getError()
                        + e.getMessage() );
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

    public class UpdateFile extends TestThreadBase {
        private byte[] updateData;

        public UpdateFile( byte[] updateData ) {
            this.updateData = updateData;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                VersionUtils.updateContentByStream( ws, fileId, updateData );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    public class DeleteFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.File.deleteInstance( ws, fileId, true );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}