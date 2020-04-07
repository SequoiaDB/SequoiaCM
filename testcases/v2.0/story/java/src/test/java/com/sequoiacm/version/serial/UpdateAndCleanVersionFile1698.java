/**
 *
 */
package com.sequoiacm.version.serial;

import java.io.IOException;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description UpdateAndCleanVersionFile1698.java
 * @author luweikang
 * @date 2018年6月15日
 */
public class UpdateAndCleanVersionFile1698 extends TestScmBase {
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

    private String fileName = "fileVersion1698";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {

        ScmFactory.File.asyncTransfer( wsA, fileId );
        VersionUtils.waitAsyncTaskFinished( wsM, fileId, 1, 2, 30 );

        UpdateFileThread updateFileThread = new UpdateFileThread();
        updateFileThread.start();

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId.toString() ).get();
        taskId = ScmSystem.Task
                .startCleanTask( wsA, cond, ScopeType.SCOPE_CURRENT );

        Assert.assertTrue( updateFileThread.isSuccess(),
                updateFileThread.getErrorMsg() );
        ScmTaskUtils.waitTaskFinish( sessionA, taskId );
        boolean branHasHisVersion = branHasHisVersion();

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
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
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

    class UpdateFileThread extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            VersionUtils.updateContentByStream( wsA, fileId, updatedata );
        }

    }

}
