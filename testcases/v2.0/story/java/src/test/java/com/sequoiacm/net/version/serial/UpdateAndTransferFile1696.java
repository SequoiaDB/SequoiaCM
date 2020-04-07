/**
 *
 */
package com.sequoiacm.net.version.serial;

import java.io.IOException;
import java.util.List;

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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description 并发更新和迁移相同文件
 * @author luweikang
 * @date 2018年6月13日
 * @modify By wuyan
 * @modify Date 2018.07.26
 * @version 1.10
 */
public class UpdateAndTransferFile1696 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private ScmSession sessionS = null;
    private ScmSession sessionT = null;
    private ScmWorkspace wsS = null;
    private ScmWorkspace wsT = null;
    private ScmId fileId = null;
    private ScmId taskId = null;

    private String fileName = "fileVersion1696";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 1024 * 2 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        wsp = ScmInfo.getWs();
        // clean file
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( wsp );
        sourceSite = siteList.get( 0 );
        targetSite = siteList.get( 1 );

        sessionS = TestScmTools.createSession( sourceSite );
        wsS = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionS );
        sessionT = TestScmTools.createSession( targetSite );
        wsT = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionT );

        fileId = VersionUtils.createFileByStream( wsS, fileName, filedata );

    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {

        int currentVersion = 2;
        int historyVersion = 1;

        UpdateFileThread updateFileThread = new UpdateFileThread();
        updateFileThread.start();

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId.toString() ).get();
        taskId = ScmSystem.Task
                .startTransferTask( wsS, cond, ScopeType.SCOPE_CURRENT,
                        targetSite.getSiteName() );

        ScmTaskUtils.waitTaskFinish( sessionS, taskId );
        Assert.assertTrue( updateFileThread.isSuccess(),
                updateFileThread.getErrorMsg() );

        int curFileVersion = rootCurVersion();

        SiteWrapper[] expHisSiteList = { targetSite, sourceSite };
        VersionUtils.checkSite( wsS, fileId, curFileVersion, expHisSiteList );
        if ( curFileVersion == 1 ) {
            VersionUtils
                    .CheckFileContentByStream( wsT, fileName, historyVersion,
                            filedata );
        } else {
            VersionUtils
                    .CheckFileContentByStream( wsT, fileName, currentVersion,
                            updatedata );
        }

        runSuccess = true;

    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( wsT, fileId, true );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( sessionS != null ) {
                sessionS.close();
            }
            if ( sessionT != null ) {
                sessionT.close();
            }
        }
    }

    private int rootCurVersion() throws ScmException {

        ScmFile file = ScmFactory.File.getInstance( wsT, fileId, 1, 0 );
        if ( file.getLocationList().size() < 2 ) {
            return 2;
        } else {
            return 1;
        }
    }

    class UpdateFileThread extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            VersionUtils.updateContentByStream( wsS, fileId, updatedata );
        }

    }

}
