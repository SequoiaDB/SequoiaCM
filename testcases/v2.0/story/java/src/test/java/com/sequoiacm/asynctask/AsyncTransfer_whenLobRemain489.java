package com.sequoiacm.asynctask;

import java.io.File;
import java.io.IOException;

import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Testcase: SCM-489:分中心存在文件，主中心存在残留LOB，且大小不一致
 * @author huangxiaoni init
 * @date 2017.6.26
 */

public class AsyncTransfer_whenLobRemain489 extends TestScmBase {
    private static String fileName = "asyncTransfer489";
    private static int fileSize = 100;
    private static String contentBase = "a";
    private static int lobSize = 99;
    private boolean runSuccess = false;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmId fileId = null;
    private File localPath = null;
    private String filePath = null;
    private String lobPath = null;

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        lobPath = localPath + File.separator + "localFile_" + lobSize + ".txt";
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, contentBase, fileSize );
        TestTools.LocalFile.createFile( lobPath, contentBase, lobSize );

        rootSite = ScmInfo.getRootSite();
        branceSite = ScmInfo.getBranchSite();
        ws_T = ScmInfo.getWs();

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( ws_T, cond );

        sessionA = TestScmTools.createSession( branceSite );
        wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        // analog lob remain in mainSite's sdb
        TestSdbTools.Lob.putLob( rootSite, ws_T, fileId, lobPath );
        // transfer siteA's file to mainSite, expect not transfer
        ScmFactory.File.asyncTransfer( wsA, fileId );
        SiteWrapper[] expSiteList = { rootSite, branceSite };
        ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId, expSiteList.length );
        ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null )
                sessionA.close();
            if ( sessionM != null )
                sessionM.close();
        }
    }
}