/**
 *
 */
package com.sequoiacm.version.concurrent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1701:并发更新和下载相同文件
 * @author luweikang
 * @createDate 2018.06.19
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class UpdateAndDownloadVersionFile1701 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSiteA = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsB = null;
    private ScmId fileId = null;
    private String fileName = "fileVersion1701";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];
    private byte[] downloadData = null;

    @BeforeClass
    private void setUp() throws ScmException {
        branSiteA = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSiteA );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = TestScmTools.createSession( rootSite );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new DownloadFileThread() );
        es.addWorker( new UpdateFileThread() );
        es.run();
        VersionUtils.CheckFileContentByStream( wsA, fileName, 2, updatedata );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    private class DownloadFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws ScmException, IOException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                file.getContent( outputStream );
                downloadData = outputStream.toByteArray();
                int majorVersion = file.getMajorVersion();
                if ( majorVersion == 1 ) {
                    VersionUtils.assertByteArrayEqual( filedata, downloadData );
                }
                outputStream.close();
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
                session = TestScmTools.createSession( branSiteA );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                VersionUtils.updateContentByStream( ws, fileId, updatedata );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
