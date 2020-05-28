package com.sequoiacm.directory;

import java.io.IOException;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
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
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmDirUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:create a file under a dir,than update Content of the file, than
 * ayncTransfer the current version file testlink-case:SCM-2059
 *
 * @author wuyan
 * @Date 2018.07.12
 * @version 1.00
 */

public class UpdateAndAsyncTransferFile2059 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;
    private ScmDirectory scmDir1;
    private ScmDirectory scmDir2;
    private String dirBasePath = "/CreatefileWiteDir2059";
    private String fullPath1 = dirBasePath
            + "/2059_a/2059_b/2059_c/2059_e/2059_f/2059_g/2059_h/2059_i/2059_g/";
    private String fullPath2 = "/2059_updatedir/2059_b/2059_c/2059_update/2059_1/2059_2"
            + "/2059_update3/";
    private String authorName = "CreateFileWithDir2059";
    private String fileName = "filedir2059";
    private byte[] writeData = new byte[ 1024 * 10 ];
    private byte[] updateData = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
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
        ScmDirUtils.deleteDir( wsA, fullPath1 );
        ScmDirUtils.deleteDir( wsA, fullPath2 );
        scmDir1 = ScmDirUtils.createDir( wsA, fullPath1 );
        scmDir2 = ScmDirUtils.createDir( wsA, fullPath2 );

    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        fileId = ScmDirUtils.createFileWithDir( wsA, fileName, writeData,
                authorName, scmDir1 );
        ScmDirUtils.updateContentWithDir( wsA, fileId, updateData, scmDir2 );

        int currentVersion = 2;
        int historyVersion = 1;
        asyncTransferCurrentVersionFile( wsA, currentVersion );

        // check the currentVersion file data and siteinfo
        SiteWrapper[] expCurSiteList = { rootSite, branSite };
        VersionUtils.checkSite( wsM, fileId, currentVersion, expCurSiteList );
        VersionUtils.CheckFileContentByStream( wsA, fullPath2 + fileName,
                currentVersion, updateData );

        // check the historyVersion file ,only on the rootSite,dir change to
        // fullpath2
        SiteWrapper[] expHisSiteList = { branSite };
        VersionUtils.checkSite( wsA, fileId, historyVersion, expHisSiteList );
        VersionUtils.CheckFileContentByStream( wsA, fullPath2 + fileName,
                historyVersion, writeData );

        // check the file dir attribute
        checkFileDirAttr( wsA, scmDir1, scmDir2, currentVersion,
                historyVersion );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
                ScmDirUtils.deleteDir( wsM, fullPath1 );
                ScmDirUtils.deleteDir( wsM, fullPath2 );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void asyncTransferCurrentVersionFile( ScmWorkspace ws,
            int majorVersion ) throws Exception {
        ScmFactory.File.asyncTransfer( ws, fileId, majorVersion, 0 );

        // wait task finished
        int sitenums = 2;
        VersionUtils.waitAsyncTaskFinished( ws, fileId, majorVersion,
                sitenums );
    }

    private void checkFileDirAttr( ScmWorkspace ws, ScmDirectory oldDir,
            ScmDirectory newDir, int currentVersion, int historyVersion )
            throws ScmException {
        // check the current file
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, currentVersion,
                0 );
        Assert.assertEquals( file.getDirectory().toString(),
                newDir.toString() );

        // check the history file,move to the update file directory:fullPath2
        ScmFile hisFile = ScmFactory.File.getInstance( ws, fileId,
                historyVersion, 0 );
        Assert.assertEquals( hisFile.getDirectory().toString(),
                newDir.toString() );
        try {
            ScmFactory.File.getInstanceByPath( ws, fullPath1 + fileName );
            Assert.fail( "get  file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                Assert.fail( "expErrorCode:-262  actError:" + e.getError()
                        + e.getMessage() );
            }
        }
    }

}