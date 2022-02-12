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
 * @description SCM-1688:并发更新相同文件
 * @author wuyan
 * @createDate 2018.06.13
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class UpdateContentBySameFile1688 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;
    private int updateThreadSuccessNum = 0;
    private boolean runSuccess = false;
    private String fileName = "versionfile1688";
    private String authorName = "author1688";
    private byte[] writeData = new byte[ 1024 * 20 ];

    @BeforeClass
    private void setUp() throws ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = VersionUtils.createFileByStream( wsM, fileName, writeData,
                authorName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int updateSize1 = 1024 * 1000;
        int updateSize2 = 1024 * 1024;
        byte[] updateData1 = new byte[ updateSize1 ];
        byte[] updateData2 = new byte[ updateSize2 ];
        UpdateFileThread updateFileThread1 = new UpdateFileThread(
                updateData1 );
        UpdateFileThread updateFileThread2 = new UpdateFileThread(
                updateData2 );
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( updateFileThread1 );
        es.addWorker( updateFileThread2 );
        es.run();
        if ( updateThreadSuccessNum == 2 ) {
            checkAllUpdateContentResult( wsM, updateData1, updateData2 );
        } else if ( updateFileThread1.getRetCode() == 0
                && updateFileThread2.getRetCode() != 0 ) {
            checkUpdateContentResult( wsM, updateData1 );
        } else if ( updateFileThread1.getRetCode() != 0
                && updateFileThread2.getRetCode() == 0 ) {
            checkUpdateContentResult( wsM, updateData2 );
        } else if ( updateThreadSuccessNum == 0 ) {
            Assert.fail( "Only One thread is expected to succeed" );
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

    private class UpdateFileThread extends ResultStore {
        byte[] fileData;

        public UpdateFileThread( byte[] fileData ) {
            this.fileData = fileData;
        }

        @ExecuteOrder(step = 1)
        private void exec() {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                VersionUtils.updateContentByStream( ws, fileId, fileData );
                updateThreadSuccessNum++;
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_VERSION_MISMATCHING
                        .getErrorCode() ) {
                    saveResult( e.getErrorCode(), e );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}