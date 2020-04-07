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

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBreakpointFile;
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
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description BreFileUpdateAndCreateVersionFile1700.java
 * @author luweikang
 * @date 2018年6月15日
 */
public class BreFileUpdateAndCreateVersionFile1700 extends TestScmBase {
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

    private String fileName = "fileVersion1700";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass(enabled = false)
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );
        sbFile = VersionUtils
                .createBreakpointFileByStream( wsA, fileName, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" }, enabled = false)
    private void test() throws Exception {

        ScmFactory.File.asyncTransfer( wsA, fileId );
        VersionUtils.waitAsyncTaskFinished( wsM, fileId, 1, 2, 30 );

        UpdateFileThread updateFileThread = new UpdateFileThread();
        updateFileThread.start();

        CleanFileThread cleanFileThread = new CleanFileThread();
        cleanFileThread.start();

        Assert.assertTrue( updateFileThread.isSuccess(),
                updateFileThread.getErrorMsg() );
        Assert.assertTrue( cleanFileThread.isSuccess(),
                cleanFileThread.getErrorMsg() );

        SiteWrapper[] expSites = { rootSite };
        VersionUtils.checkSite( wsM, fileId, 1, expSites );
        VersionUtils.CheckFileContentByStream( wsA, fileName, 2, updatedata );

        runSuccess = true;
    }

    @AfterClass(enabled = false)
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

    class UpdateFileThread extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            Thread.sleep( 210 );
            ScmFile scmFile = ScmFactory.File.getInstance( wsA, fileId );
            scmFile.updateContent( sbFile );
        }

    }

    class CleanFileThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.FILE_ID )
                    .is( fileId.toString() ).get();
            taskId = ScmSystem.Task
                    .startCleanTask( wsA, cond, ScopeType.SCOPE_CURRENT );
        }
    }
}
