package com.sequoiacm.net.version.serial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
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
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content: Clean the all version file, matcheing partial current
 * version file clean
 * testlink-case:SCM-1669
 *
 * @author wuyan
 * @Date 2018.06.11
 * @modify By wuyan
 * @modify Date: 2018.07.26
 * @version 1.10
 */

public class CleanAllVersionFile1669 extends TestScmBase {
    private static WsWrapper wsp = null;
    private static SiteWrapper cleanSite = null;
    private static SiteWrapper lastSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionL = null;
    private ScmWorkspace wsL = null;
    private ScmId taskId = null;
    private List< String > fileIdList = new ArrayList< String >();
    private File localPath = null;
    private int fileNum = 10;
    private BSONObject condition = null;

    private String fileName = "fileVersion1669";
    private String authorName = "author1669";
    private int fileSize1 = 1024 * 50;
    private int fileSize2 = 1024 * 100;
    private String filePath1 = null;
    private String filePath2 = null;
    private byte[] writedata = new byte[ 1024 * 15 ];
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath1 =
                localPath + File.separator + "localFile_" + fileSize1 + ".txt";
        filePath2 =
                localPath + File.separator + "localFile_" + fileSize2 + ".txt";
        TestTools.LocalFile.createFile( filePath1, fileSize1 );
        TestTools.LocalFile.createFile( filePath2, fileSize2 );

        wsp = ScmInfo.getWs();
        // clean file
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        cleanSite = ScmNetUtils.getNonLastSite( wsp );
        lastSite = ScmNetUtils.getLastSite( wsp );
        sessionA = TestScmTools.createSession( cleanSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionL = TestScmTools.createSession( lastSite );
        wsL = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionL );
        writeAndUpdateFile( wsA );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        readFileFromM( wsL, currentVersion );
        readFileFromM( wsL, historyVersion );

        // clean history version file
        ScopeType scopeType = ScopeType.SCOPE_ALL;
        startCleanTaskByAllVerFile( wsA, sessionA, scopeType );

        // check siteinfo
        checkClearedFileSiteInfo( wsA, currentVersion, historyVersion );
        checkNoClearedFileInfo( wsL, currentVersion );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                TestSdbTools.Task.deleteMeta( taskId );
                for ( String fileId : fileIdList ) {
                    ScmFactory.File
                            .deleteInstance( wsL, new ScmId( fileId ), true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionL != null ) {
                sessionL.close();
            }
        }
    }

    private void writeAndUpdateFile( ScmWorkspace ws ) throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            String subfileName = fileName + "_" + i;
            ScmId fileId = VersionUtils
                    .createFileByStream( ws, subfileName, writedata,
                            authorName );
            if ( i % 2 == 0 ) {
                VersionUtils.updateContentByFile( ws, subfileName, fileId,
                        filePath1 );
            } else {
                VersionUtils.updateContentByFile( ws, subfileName, fileId,
                        filePath2 );
            }
            fileIdList.add( fileId.get() );
        }
    }

    private void startCleanTaskByAllVerFile( ScmWorkspace ws,
            ScmSession session, ScopeType scopeType ) throws Exception {
        condition = ScmQueryBuilder.start( ScmAttributeName.File.SIZE )
                .lessThanEquals( fileSize1 )
                .put( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();
        taskId = ScmSystem.Task.startCleanTask( ws, condition, scopeType );

        // wait task finish
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }

    private void checkClearedFileSiteInfo( ScmWorkspace ws, int currentVersion,
            int historyVersion ) throws Exception {
        // check the cleared current and history version file sitelist ,only on
        // the rootSite
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File
                .listInstance( ws, ScopeType.SCOPE_ALL, condition );
        int size = 0;
        SiteWrapper[] expSiteList = { lastSite };
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            ScmId fileId = file.getFileId();
            int version = file.getMajorVersion();
            if ( version == currentVersion ) {
                // current version file cleared nums is 5
                VersionUtils
                        .checkSite( ws, fileId, currentVersion, expSiteList );
            } else {
                // history version file cleared nums is 10
                VersionUtils
                        .checkSite( ws, fileId, historyVersion, expSiteList );
            }
            size++;
        }
        cursor.close();
        int expFileNum = 15;
        Assert.assertEquals( size, expFileNum );
    }

    private void checkNoClearedFileInfo( ScmWorkspace ws, int version )
            throws ScmException {
        // no cleared current version file
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.SIZE ).greaterThan( fileSize1 )
                .put( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File
                .listInstance( ws, ScopeType.SCOPE_ALL, condition );
        SiteWrapper[] expHisSiteList = { lastSite, cleanSite };
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            // check results
            ScmId fileId = file.getFileId();
            VersionUtils.checkSite( ws, fileId, version, expHisSiteList );
            size++;
        }
        cursor.close();
        int expFileNums = 5;
        Assert.assertEquals( size, expFileNums );
    }

    private void readFileFromM( ScmWorkspace ws, int version )
            throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = new ScmId( fileIdList.get( i ) );
            String downloadPath = TestTools.LocalFile
                    .initDownloadPath( localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
            ScmFile file = ScmFactory.File
                    .getInstance( ws, fileId, version, 0 );
            file.getContent( downloadPath );
        }
    }

}