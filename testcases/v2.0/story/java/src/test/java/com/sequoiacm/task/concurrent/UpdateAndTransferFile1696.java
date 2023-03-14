/**
 *
 */
package com.sequoiacm.task.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1696:并发更新和迁移相同文件
 * @author luweikang
 * @createDate 2018.06.13
 * @updateUser ZhangYanan
 * @updateDate 2021.12.09
 * @updateRemark
 * @version v1.0
 */
public class UpdateAndTransferFile1696 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;
    private ScmId taskId = null;
    private String fileName = "fileVersion1696";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 1024 * 2 ];

    @BeforeClass
    private void setUp() throws ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = ScmSessionUtils.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = ScmSessionUtils.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = ScmFileUtils.createFileByStream( wsA, fileName, filedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new TransferFileThread() );
        es.addWorker( new UpdateFileThread() );
        es.run();
        int curFileVersion = rootCurVersion();
        SiteWrapper[] expHisSiteList = { rootSite, branSite };
        VersionUtils.checkSite( wsA, fileId, curFileVersion, expHisSiteList );
        if ( curFileVersion == 1 ) {
            VersionUtils.CheckFileContentByStream( wsM, fileName,
                    historyVersion, filedata );
        } else {
            VersionUtils.CheckFileContentByStream( wsM, fileName,
                    currentVersion, updatedata );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private int rootCurVersion() throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( wsM, fileId, 1, 0 );
        if ( file.getLocationList().size() < 2 ) {
            return 2;
        } else {
            return 1;
        }
    }

    private class UpdateFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( branSite );
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

    private class TransferFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_ID )
                        .is( fileId.toString() ).get();
                taskId = ScmSystem.Task.startTransferTask( ws, cond,
                        ScmType.ScopeType.SCOPE_CURRENT,
                        rootSite.getSiteName() );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
