package com.sequoiacm.version.serial;

import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
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
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
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
 * @description SCM-1665:指定文件版本不存在，执行迁移任务
 * @author wuyan
 * @createDate 2018.06.05
 * @updateUser ZhangYanan
 * @updateDate 2021.12.09
 * @updateRemark
 * @version v1.0
 */
public class TransferHisVersionFile1665 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId taskId = null;
    private ScmId fileId = null;
    private String fileName = "fileVersion1661";
    private String authorName = "transfer1661";
    private byte[] writeData = new byte[ 1024 * 2 ];
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = VersionUtils.createFileByStream( wsA, fileName, writeData,
                authorName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 1;
        startTransferTaskByHistoryVerFile( wsA, sessionA );

        // check task info, the task successCount is 0
        ScmTask taskInfo = ScmSystem.Task.getTask( sessionA, taskId );
        Assert.assertEquals( taskInfo.getSuccessCount(), 0 );
        Assert.assertEquals( taskInfo.getActualCount(), 0 );

        // check the currentVersion file data and siteinfo
        SiteWrapper[] expSiteList = { branSite };
        VersionUtils.checkSite( wsA, fileId, currentVersion, expSiteList );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestSdbTools.Task.deleteMeta( taskId );
                ScmFactory.File.deleteInstance( wsM, fileId, true );
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

    private void startTransferTaskByHistoryVerFile( ScmWorkspace ws,
            ScmSession session ) throws Exception {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        taskId = ScmSystem.Task.startTransferTask( ws, condition,
                ScopeType.SCOPE_HISTORY, rootSite.getSiteName() );

        // wait task finish
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }
}