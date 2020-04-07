package com.sequoiacm.net.asynctask;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
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
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @author huangxiaoni init
 * @Testcase: SCM-508:分中心有残留LOB，且跟主中心LOB大小不一致
 * @date 2017.6.26
 */

public class AsyncCache_whenLobRemain508 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmId fileId = null;
    private String fileName = "asyncCache508";
    private int fileSize = 100;
    private File localPath = null;
    private String filePath = null;

    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        // ready local file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        ws_T = ScmInfo.getWs();
        List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( ws_T );

        sourceSite = siteList.get( 0 );
        targetSite = siteList.get( 1 );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( ws_T, cond );

        // login
        sessionM = TestScmTools.createSession( sourceSite );
        wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );

        sessionA = TestScmTools.createSession( targetSite );
        wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

        // ready scm file
        writeFileFromM();
        //lobRemainFromA();
        String remainfilePath =
                localPath + File.separator + "localFile_" + fileSize / 2 +
                        ".txt";
        TestTools.LocalFile.createFile( remainfilePath, fileSize / 2 );
        TestSdbTools.Lob.putLob( targetSite, ws_T, fileId, remainfilePath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmFactory.File.asyncCache( wsA, fileId );
        //check result
        SiteWrapper[] expSiteList = { sourceSite, targetSite };
        ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId, expSiteList.length );
        ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
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
}