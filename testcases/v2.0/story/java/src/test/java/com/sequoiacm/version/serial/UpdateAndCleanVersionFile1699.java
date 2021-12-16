/**
 *
 */
package com.sequoiacm.version.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1699:并发使用断点文件更新和清理相同文件
 * @author luweikang
 * @createDate 2018.06.15
 * @updateUser ZhangYanan
 * @updateDate 2021.12.09
 * @updateRemark
 * @version v1.0
 */
public class UpdateAndCleanVersionFile1699 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;
    private ScmBreakpointFile sbFile = null;
    private ScmId taskId = null;
    private String fileName = "fileVersion1699";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws ScmException {
        BreakpointUtil.checkDBDataSource();
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );
        sbFile = VersionUtils.createBreakpointFileByStream( wsA, fileName,
                updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmFactory.File.asyncTransfer( wsA, fileId, rootSite.getSiteName() );
        VersionUtils.waitAsyncTaskFinished( wsM, fileId, 1, 2, 30 );
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new CleanFileThread() );
        es.addWorker( new UpdateFileThread() );
        es.run();
        boolean branHasHisVersion = branHasHisVersion();
        System.out.println("----b="+branHasHisVersion);
        if ( branHasHisVersion ) {
            SiteWrapper[] expSites = { rootSite, branSite };
            VersionUtils.checkSite( wsM, fileId, 1, expSites );
            VersionUtils.CheckFileContentByStream( wsA, fileName, 1, filedata );
        } else {
            SiteWrapper[] expSites = { rootSite };
            VersionUtils.checkSite( wsM, fileId, 1, expSites );
        }
        VersionUtils.CheckFileContentByStream( wsA, fileName, 2, updatedata );
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

    private boolean branHasHisVersion() throws ScmException {

        ScmFile file = ScmFactory.File.getInstance( wsA, fileId, 1, 0 );
        int siteNum = file.getLocationList().size();

        boolean branHasHisVersion = false;
        if ( siteNum > 1 ) {
            branHasHisVersion = true;
        }
        return branHasHisVersion;
    }

    private class UpdateFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws ScmException, InterruptedException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile scmFile = ScmFactory.File.getInstance( ws, fileId );
                scmFile.updateContent( sbFile );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class CleanFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_ID )
                        .is( fileId.toString() ).get();
                taskId = ScmSystem.Task.startCleanTask( ws, cond,
                        ScopeType.SCOPE_CURRENT );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
