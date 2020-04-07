package com.sequoiacm.version.concurrent;

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
 * test content: delete the same file concurrently
 * testlink-case:SCM-1702
 *
 * @author wuyan
 * @Date 2018.06.19
 * @version 1.00
 */

public class DeleteSameFile1702 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;

    private String fileName = "versionfile1702";
    private String authorName = "author1702";
    private byte[] writeData = new byte[ 1024 * 10 ];
    private byte[] updateData = new byte[ 1024 * 1024 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        fileId = VersionUtils
                .createFileByStream( wsM, fileName, writeData, authorName );
        VersionUtils.updateContentByStream( wsM, fileId, updateData );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        DeleteFile deleteFile = new DeleteFile();
        deleteFile.start( 3 );
        Assert.assertTrue( deleteFile.isSuccess(), deleteFile.getErrorMsg() );
        //check the delete result
        checkDeleteFileResult( wsM );
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( wsM, fileId, true );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void checkDeleteFileResult( ScmWorkspace ws ) throws Exception {
        //check the FILE is not exist
        try {
            ScmFactory.File.getInstanceByPath( ws, fileName );
            Assert.fail( "get file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                Assert.fail( "expErrorCode:-262  actError:" + e.getError() +
                        e.getMessage() );
            }
        }

        //count histroy and current version file are not exist
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        long count = ScmFactory.File
                .countInstance( ws, ScopeType.SCOPE_ALL, condition );
        long expFileConut = 0;
        Assert.assertEquals( count, expFileConut );
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