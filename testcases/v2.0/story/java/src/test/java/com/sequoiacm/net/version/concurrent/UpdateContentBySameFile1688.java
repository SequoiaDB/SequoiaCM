package com.sequoiacm.net.version.concurrent;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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
 * test content: update Content of the same file concurrently
 * testlink-case:SCM-1688
 *
 * @author wuyan
 * @Date 2018.06.13
 * @version 1.00
 */

public class UpdateContentBySameFile1688 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;

    private String fileName = "versionfile1688";
    private String authorName = "author1688";
    private byte[] writeData = new byte[ 1024 * 20 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        fileId = VersionUtils.createFileByStream( wsM, fileName, writeData,
                authorName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int updateSize1 = 1024 * 1000;
        int updateSize2 = 1024 * 1024;
        byte[] updateData1 = new byte[ updateSize1 ];
        byte[] updateData2 = new byte[ updateSize2 ];
        UpdateContentThread updateContentThread1 = new UpdateContentThread(
                updateData1 );
        UpdateContentThread updateContentThread2 = new UpdateContentThread(
                updateData2 );
        updateContentThread1.start();
        updateContentThread2.start();

        if ( updateContentThread1.isSuccess() ) {
            if ( !updateContentThread2.isSuccess() ) {
                Assert.assertTrue( !updateContentThread2.isSuccess(),
                        updateContentThread2.getErrorMsg() );
                ScmException e = ( ScmException ) updateContentThread2
                        .getExceptions().get( 0 );
                Assert.assertEquals( e.getError(),
                        ScmError.FILE_VERSION_MISMATCHING,
                        "updateContent2 fail:"
                                + updateContentThread2.getErrorMsg() );
                checkUpdateContentResult( wsM, updateData1 );
            } else if ( updateContentThread2.isSuccess() ) {
                checkAllUpdateContentResult( wsM, updateData1, updateData2 );
            } else {
                Assert.fail(
                        "the results can only by updated successfully or one "
                                + "update succeeds" );
            }
        } else if ( !updateContentThread1.isSuccess() ) {
            Assert.assertTrue( updateContentThread2.isSuccess(),
                    updateContentThread2.getErrorMsg() );
            ScmException e = ( ScmException ) updateContentThread1
                    .getExceptions().get( 0 );
            Assert.assertEquals( e.getError(),
                    ScmError.FILE_VERSION_MISMATCHING, "updateContent1 fail:"
                            + updateContentThread1.getErrorMsg() );
            checkUpdateContentResult( wsM, updateData2 );
            ;
        }

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

    private void checkAllUpdateContentResult( ScmWorkspace ws,
            byte[] updatedata1, byte[] updatedata2 ) throws Exception {
        int historyVersion1 = 1;
        // first updateContent version
        int historyVersion2 = 2;
        // second updateContent version
        int currentVersion = 3;

        ScmFile file = ScmFactory.File.getInstance( ws, fileId, currentVersion,
                0 );
        long fileSize = file.getSize();

        // check the updateContent
        if ( fileSize == updatedata1.length ) {
            VersionUtils.CheckFileContentByStream( ws, fileName, currentVersion,
                    updatedata1 );
            VersionUtils.CheckFileContentByStream( ws, fileName,
                    historyVersion2, updatedata2 );
        } else if ( fileSize == updatedata2.length ) {
            VersionUtils.CheckFileContentByStream( ws, fileName, currentVersion,
                    updatedata2 );
            VersionUtils.CheckFileContentByStream( ws, fileName,
                    historyVersion2, updatedata1 );
        } else {
            Assert.fail( "update file content is error!" );
        }
        // check the write content
        VersionUtils.CheckFileContentByStream( ws, fileName, historyVersion1,
                writeData );
    }

    private void checkUpdateContentResult( ScmWorkspace ws, byte[] updatedata )
            throws Exception {
        int historyVersion = 1;
        int currentVersion = 2;
        VersionUtils.CheckFileContentByStream( ws, fileName, currentVersion,
                updatedata );
        VersionUtils.CheckFileContentByStream( ws, fileName, historyVersion,
                writeData );
    }

    public class UpdateContentThread extends TestThreadBase {
        byte[] fileData;

        public UpdateContentThread( byte[] fileData ) {
            this.fileData = fileData;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                VersionUtils.updateContentByStream( ws, fileId, fileData );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}