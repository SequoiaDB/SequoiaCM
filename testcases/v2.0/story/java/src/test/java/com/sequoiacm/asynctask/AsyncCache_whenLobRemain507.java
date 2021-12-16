package com.sequoiacm.asynctask;

import java.io.File;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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
 * @Testcase: SCM-507:分中心有残留LOB，且跟主中心LOB大小一致
 * @author huangxiaoni init
 * @date 2017.6.26
 */

public class AsyncCache_whenLobRemain507 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmId fileId = null;
    private String fileName = "asyncCache507";
    private int fileSize = 100;
    private File localPath = null;
    private String filePath = null;
    private String content = null;

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        // ready local file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        content = TestTools.getRandomString( fileSize );
        TestTools.LocalFile.createFile( filePath, content, fileSize );
        rootSite = ScmInfo.getRootSite();
        branceSite = ScmInfo.getBranchSite();
        ws_T = ScmInfo.getWs();
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( ws_T, cond );
        // login
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );
        sessionA = TestScmTools.createSession( branceSite );
        wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
        // ready scm file
        writeFileFromM();
        // lobRemainFromA();
        TestSdbTools.Lob.putLob( branceSite, ws_T, fileId, filePath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmFactory.File.asyncCache( wsA, fileId );
        SiteWrapper[] expSiteList = { rootSite, branceSite };
        ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId, expSiteList.length );
        ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                filePath );
        readFileFromA();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void writeFileFromM() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( wsM );
        file.setContent( filePath );
        file.setFileName( fileName + "_" + UUID.randomUUID() );
        file.setAuthor( fileName );
        fileId = file.save();
    }

    private void readFileFromA() throws Exception {
        TestSdbTools.Lob.removeLob( rootSite, ws_T, fileId );
        // read siteA's local cache
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        ScmFile file = ScmFactory.File.getInstance( wsA, fileId );
        file.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
    }
}
