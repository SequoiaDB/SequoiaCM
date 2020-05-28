package com.sequoiacm.net.directory;

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
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:create a file under a dir,than update Content of the file, than
 * ayncCache the current version file testlink-case:SCM-2058
 *
 * @author wuyan
 * @Date 2018.07.12
 * @modify Date 2018.07.27
 * @version 1.10
 */

public class UpdateAndAsyncCacheFile2058 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper sourceSite = null;
    private SiteWrapper cacheSite = null;
    private SiteWrapper updateSite = null;
    private ScmSession sessionS = null;
    private ScmWorkspace wsS = null;
    private ScmSession sessionC = null;
    private ScmWorkspace wsC = null;
    private ScmSession sessionU = null;
    private ScmWorkspace wsU = null;
    private ScmId fileId = null;

    private ScmDirectory scmDir;
    private String dirBasePath = "/CreatefileWiteDir2058";
    private String fullPath = dirBasePath
            + "/2058_a/2058_b/2058_c/2058_e/2058_f/";
    private String author = "CreateFileWithDir2058";
    private String fileName = "filedir2058";
    private byte[] fileData = new byte[ 1024 * 100 ];
    private byte[] updateData = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        wsp = ScmInfo.getWs();
        // clean file
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        List< SiteWrapper > siteList = ScmNetUtils.getAllSite( wsp );
        updateSite = siteList.get( 0 );
        cacheSite = siteList.get( 1 );
        sourceSite = siteList.get( 2 );
        sessionS = TestScmTools.createSession( sourceSite );
        wsS = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionS );
        sessionC = TestScmTools.createSession( cacheSite );
        wsC = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionC );
        sessionU = TestScmTools.createSession( updateSite );
        wsU = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionU );
        deleteDir( wsS, fullPath );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        scmDir = createDir( wsS, fullPath );
        fileId = ScmDirUtils.createFileWithDir( wsS, fileName, fileData, author,
                scmDir );
        VersionUtils.updateContentByStream( wsU, fileId, updateData );

        int currentVersion = 2;
        int historyVersion = 1;
        asyncCacheCurrentVersionFile( wsC, currentVersion );

        // check the currentVersion file data and siteinfo,dirinfo
        SiteWrapper[] expCurSiteList = { cacheSite, updateSite };
        VersionUtils.checkSite( wsU, fileId, currentVersion, expCurSiteList );
        VersionUtils.CheckFileContentByStream( wsS, fullPath + fileName,
                currentVersion, updateData );
        checkFileDir( wsS, scmDir );

        // check the historyVersion file only on the sourcesite
        SiteWrapper[] expHisSiteList = { sourceSite };
        VersionUtils.checkSite( wsS, fileId, historyVersion, expHisSiteList );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( wsS, fileId, true );
                deleteDir( wsC, fullPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionS != null ) {
                sessionS.close();
            }
            if ( sessionU != null ) {
                sessionU.close();
            }
            if ( sessionC != null ) {
                sessionC.close();
            }
        }
    }

    private void asyncCacheCurrentVersionFile( ScmWorkspace ws,
            int majorVersion ) throws Exception {
        // cache
        ScmFactory.File.asyncCache( ws, fileId, majorVersion, 0 );

        int sitenums = 2;
        VersionUtils.waitAsyncTaskFinished( ws, fileId, majorVersion,
                sitenums );

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
        return ScmFactory.Directory.getInstance( ws,
                pathList.get( pathList.size() - 1 ) );
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
                if ( e.getError() != ScmError.DIR_NOT_FOUND
                        && e.getError() != ScmError.DIR_NOT_EMPTY ) {
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