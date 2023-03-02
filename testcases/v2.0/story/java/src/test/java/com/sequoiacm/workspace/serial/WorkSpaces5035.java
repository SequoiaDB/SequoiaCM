package com.sequoiacm.workspace.serial;

import java.io.File;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5035:删除工作区后创建同名工作区
 * @author ZhangYanan
 * @date 2022/8/1
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5035 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmSession ssRootSite = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private ScmSession ssBranSite = null;
    private String wsName = "ws5035";
    private ScmWorkspace rootSiteWs = null;
    private ScmWorkspace branSiteWs = null;
    private int fileSize = 1024;
    private String fileName = "file5035";
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        // ready local file
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath1 = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        filePath2 = localPath + File.separator + "localFile_" + fileSize * 2
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize );
        TestTools.LocalFile.createFile( filePath2, fileSize * 2 );

        rootSite = ScmInfo.getRootSite();
        branSite = ScmInfo.getBranchSite();
        ssRootSite = TestScmTools.createSession( rootSite );
        ssBranSite = TestScmTools.createSession( branSite );

        ScmWorkspaceUtil.deleteWs( wsName, ssRootSite );
        ScmWorkspaceUtil.createWS( ssRootSite, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( ssRootSite, wsName );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsName, ssRootSite );
        branSiteWs = ScmFactory.Workspace.getWorkspace( wsName, ssBranSite );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test1() throws Exception {
        ScmId fileId = ScmFileUtils.create( rootSiteWs, fileName, filePath1 );

        BSONObject query = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmId taskId = ScmSystem.Task.startTransferTask( rootSiteWs, query,
                ScmType.ScopeType.SCOPE_CURRENT, branSite.getSiteName() );
        ScmTaskUtils.waitTaskStop( ssRootSite, taskId );
        SiteWrapper[] expSite = new SiteWrapper[] { rootSite, branSite };
        ScmFileUtils.checkMeta( rootSiteWs, fileId, expSite );
        ScmFileUtils.checkData( rootSiteWs, fileId, localPath, filePath1 );

        // deletews
        ScmWorkspaceUtil.deleteWs( wsName, ssRootSite );

        // create ws
        int time = 0;
        while ( true ) {
            Thread.sleep( 1000 );
            try {
                ScmWorkspaceUtil.createWS( ssRootSite, wsName,
                        ScmInfo.getSiteNum() );
                break;
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.CONFIG_SERVER_ERROR ) {
                    throw e;
                }
            }
            if ( time < 60 ) {
                time++;
            } else {
                throw new Exception( "createWs timeout !" );
            }
        }

        ScmWorkspaceUtil.wsSetPriority( ssRootSite, wsName );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsName, ssRootSite );

        fileId = ScmFileUtils.create( rootSiteWs, fileName, filePath2 );

        taskId = ScmSystem.Task.startTransferTask( rootSiteWs, query,
                ScmType.ScopeType.SCOPE_CURRENT, branSite.getSiteName() );
        ScmTaskUtils.waitTaskStop( ssRootSite, taskId );

        ScmFileUtils.checkMeta( rootSiteWs, fileId, expSite );
        ScmFileUtils.checkData( rootSiteWs, fileId, localPath, filePath2 );
        runSuccess = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test2() throws Exception {
        ScmId fileId = ScmFileUtils.create( branSiteWs, fileName + "_1",
                filePath1 );

        SiteWrapper[] expSite = new SiteWrapper[] { branSite };
        ScmFileUtils.checkMeta( branSiteWs, fileId, expSite );
        ScmFileUtils.checkData( branSiteWs, fileId, localPath, filePath1 );
        // deletews
        ScmWorkspaceUtil.deleteWs( wsName, ssBranSite );

        // create ws
        int time = 0;
        while ( true ) {
            Thread.sleep( 1000 );
            try {
                ScmWorkspaceUtil.createWS( ssBranSite, wsName,
                        ScmInfo.getSiteNum() );
                break;
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.CONFIG_SERVER_ERROR ) {
                    throw e;
                }
            }
            if ( time < 60 ) {
                time++;
            } else {
                throw new Exception( "createWs timeout !" );
            }
        }

        ScmWorkspaceUtil.wsSetPriority( ssBranSite, wsName );
        branSiteWs = ScmFactory.Workspace.getWorkspace( wsName, ssBranSite );

        fileId = ScmFileUtils.create( branSiteWs, fileName + "_1", filePath2 );
        ScmFileUtils.checkMeta( branSiteWs, fileId, expSite );
        ScmFileUtils.checkData( branSiteWs, fileId, localPath, filePath2 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, ssRootSite );
            if ( ssRootSite != null ) {
                ssRootSite.close();
            }
            if ( ssBranSite != null ) {
                ssBranSite.close();
            }
        }
    }

}