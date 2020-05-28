package com.sequoiacm.scmfile;

import java.io.File;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
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
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-282:所有中心均存在文件，在分中心A删除文件
 * @author huangxiaoni init
 * @date 2017.5.23
 */

public class Delete_fileExistAllSite282 extends TestScmBase {
    private static WsWrapper wsp = null;
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;

    private String fileName = "delete282";
    private String author = fileName;
    private ScmId fileId = null;
    private int fileSize = 100;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();

            sessionM = TestScmTools.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

            sessionA = TestScmTools.createSession( branSites.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

            sessionB = TestScmTools.createSession( branSites.get( 1 ) );
            wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );

            fileId = ScmFileUtils.create( wsA, fileName, filePath );
            this.readScmFile( wsB );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        try {
            // delete
            ScmFactory.File.getInstance( wsA, fileId ).delete( true );

            // check results
            this.checkResults();

            runSuccess = true;
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionM != null )
                sessionM.close();
            if ( sessionA != null )
                sessionA.close();
            if ( sessionB != null )
                sessionB.close();

        }
    }

    private void readScmFile( ScmWorkspace ws ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        String downloadPath = localPath + File.separator + "download.txt";
        file.getContent( downloadPath );
    }

    private void checkResults() throws Exception {
        // check meta
        BSONObject cond = new BasicBSONObject( "id", fileId.get() );
        long cnt = ScmFactory.File.countInstance( wsA, ScopeType.SCOPE_CURRENT,
                cond );
        Assert.assertEquals( cnt, 0 );

        // check data
        ScmWorkspace[] wsArr = { wsM, wsA, wsB };
        for ( int i = 0; i < wsArr.length; i++ ) {
            try {
                ScmFileUtils.checkData( wsArr[ i ], fileId, localPath,
                        filePath );
                Assert.assertFalse( true,
                        "File is unExisted, except throw e, but success." );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND,
                        e.getMessage() );
            }
        }
    }
}