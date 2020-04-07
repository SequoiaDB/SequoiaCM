package com.sequoiacm.directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
 * test content:create a file under a dir,than update Content of the file,
 * than ayncCache the current version file
 * testlink-case:SCM-2058
 *
 * @author wuyan
 * @Date 2018.07.12
 * @version 1.00
 */

public class UpdateAndAsyncCacheFile2058 extends TestScmBase {
    private static WsWrapper wsp = null;
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private List< SiteWrapper > branSites = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;

    private ScmDirectory scmDir;
    private String dirBasePath = "/CreatefileWiteDir2058";
    private String fullPath =
            dirBasePath + "/2058_a/2058_b/2058_c/2058_e/2058_f/";
    private String author = "CreateFileWithDir2058";
    private String fileName = "filedir2058";
    private byte[] fileData = new byte[ 1024 * 100 ];
    private byte[] updateData = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSites = ScmInfo.getBranchSites( branSitesNum );

        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = TestScmTools.createSession( branSites.get( 1 ) );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( author ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        deleteDir( wsA, fullPath );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        scmDir = createDir( wsA, fullPath );
        fileId = ScmDirUtils
                .createFileWithDir( wsA, fileName, fileData, author, scmDir );
        VersionUtils.updateContentByStream( wsM, fileId, updateData );

        int currentVersion = 2;
        int historyVersion = 1;
        asyncCacheCurrentVersionFile( wsB, currentVersion );

        //check the currentVersion file data and siteinfo,dirinfo
        SiteWrapper[] expCurSiteList = { rootSite, branSites.get( 1 ) };
        VersionUtils.checkSite( wsM, fileId, currentVersion, expCurSiteList );
        VersionUtils.CheckFileContentByStream( wsA, fullPath + fileName,
                currentVersion, updateData );
        checkFileDir( wsA, scmDir );

        //check the historyVersion file only on the rootSite
        SiteWrapper[] expHisSiteList = { branSites.get( 0 ) };
        VersionUtils.checkSite( wsA, fileId, historyVersion, expHisSiteList );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
                deleteDir( wsB, fullPath );
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
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    private void asyncCacheCurrentVersionFile( ScmWorkspace ws,
            int majorVersion ) throws Exception {
        // cache
        ScmFactory.File.asyncCache( ws, fileId, majorVersion, 0 );

        int sitenums = 2;
        VersionUtils
                .waitAsyncTaskFinished( ws, fileId, majorVersion, sitenums );

    }

    private ScmDirectory createDir( ScmWorkspace ws, String dirPath )
            throws ScmException {
        List< String > pathList = getSubPaths( dirPath );
        for ( String path : pathList ) {
            try {
                ScmFactory.Directory.createInstance( ws, path );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        return ScmFactory.Directory
                .getInstance( ws, pathList.get( pathList.size() - 1 ) );
    }

    private void checkFileDir( ScmWorkspace ws, ScmDirectory scmDir )
            throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getDirectory().toString(),
                scmDir.toString() );
    }

    private void deleteDir( ScmWorkspace ws, String dirPath ) {
        List< String > pathList = getSubPaths( dirPath );
        for ( int i = pathList.size() - 1; i >= 0; i-- ) {
            try {
                ScmFactory.Directory.deleteInstance( ws, pathList.get( i ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND &&
                        e.getError() != ScmError.DIR_NOT_EMPTY ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private List< String > getSubPaths( String path ) {
        String ele = "/";
        String[] arry = path.split( "/" );
        List< String > pathList = new ArrayList< String >();
        for ( int i = 1; i < arry.length; i++ ) {
            ele = ele + arry[ i ];
            pathList.add( ele );
            ele = ele + "/";
        }
        return pathList;
    }

}