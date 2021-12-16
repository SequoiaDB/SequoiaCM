package com.sequoiacm.version.serial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
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
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1667:清理当前版本文件
 * @author wuyan
 * @createDate 2018.06.08
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class CleanCurVersionFile1667 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId taskId = null;
    private List< String > fileIdList = new ArrayList< String >();
    private File localPath = null;
    private int fileNum = 10;
    private BSONObject condition = null;

    private String fileName = "fileVersion1667";
    private String authorName = "author1667";
    private int fileSize1 = 1024 * 100;
    private int fileSize2 = 1024 * 5;
    private String filePath1 = null;
    private String filePath2 = null;
    private byte[] writedata = new byte[ 1024 * 200 ];
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath1 = localPath + File.separator + "localFile_" + fileSize1
                + ".txt";
        filePath2 = localPath + File.separator + "localFile_" + fileSize2
                + ".txt";
        TestTools.LocalFile.createFile( filePath1, fileSize1 );
        TestTools.LocalFile.createFile( filePath2, fileSize2 );

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
        writeAndUpdateFile( wsA );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        readFileFromM( wsM, currentVersion );
        readFileFromM( wsM, historyVersion );

        // clean current version file
        ScopeType scopeType = ScopeType.SCOPE_CURRENT;
        startCleanTaskByCurrentVerFile( wsA, sessionA, scopeType );

        // check siteinfo
        checkCurrentVerFileSiteInfo( wsA, currentVersion );
        checkHisVersionFileInfo( wsM, historyVersion );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestSdbTools.Task.deleteMeta( taskId );
                for ( String fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( wsM, new ScmId( fileId ),
                            true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void writeAndUpdateFile( ScmWorkspace ws ) throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            String subfileName = fileName + "_" + i;
            ScmId fileId = VersionUtils.createFileByStream( ws, subfileName,
                    writedata, authorName );
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

    private void startCleanTaskByCurrentVerFile( ScmWorkspace ws,
            ScmSession session, ScopeType scopeType ) throws Exception {
        condition = ScmQueryBuilder.start().put( ScmAttributeName.File.SIZE )
                .greaterThanEquals( fileSize1 )
                .put( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        taskId = ScmSystem.Task.startCleanTask( ws, condition, scopeType );

        // wait task finish
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }

    private void checkCurrentVerFileSiteInfo( ScmWorkspace ws,
            int currentVersion ) throws Exception {
        // check the clean file,check the sitelist and data
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_CURRENT, condition );
        int size = 0;

        SiteWrapper[] expCurSiteList = { rootSite };
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            ScmId fileId = file.getFileId();
            VersionUtils.checkSite( ws, fileId, currentVersion,
                    expCurSiteList );
            size++;
        }
        cursor.close();
        int expFileNum = 5;
        Assert.assertEquals( size, expFileNum );

        // check the no clean file by current version
        BSONObject condition1 = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.SIZE ).lessThan( fileSize1 )
                .put( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        ScmCursor< ScmFileBasicInfo > cursor1 = ScmFactory.File
                .listInstance( ws, ScopeType.SCOPE_CURRENT, condition1 );
        int size1 = 0;
        SiteWrapper[] expCurSiteList1 = { rootSite, branSite };
        while ( cursor1.hasNext() ) {
            ScmFileBasicInfo file1 = cursor1.getNext();
            ScmId fileId1 = file1.getFileId();
            VersionUtils.checkSite( ws, fileId1, currentVersion,
                    expCurSiteList1 );
            size1++;
        }
        cursor1.close();
        Assert.assertEquals( size1, expFileNum );
    }

    private void checkHisVersionFileInfo( ScmWorkspace ws, int version )
            throws Exception {
        // all history version file only on the branSite
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_HISTORY, condition );
        SiteWrapper[] expHisSiteList = { rootSite, branSite };
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            // check results
            ScmId fileId = file.getFileId();

            VersionUtils.checkSite( ws, fileId, version, expHisSiteList );
            size++;
            ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionM );
            VersionUtils.checkSite( ws1, fileId, version, expHisSiteList );
        }
        cursor.close();
        int expFileNums = 10;
        Assert.assertEquals( size, expFileNums );
    }

    private void readFileFromM( ScmWorkspace ws, int version )
            throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = new ScmId( fileIdList.get( i ) );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            ScmFile file = ScmFactory.File.getInstance( wsM, fileId, version,
                    0 );
            file.getContent( downloadPath );
        }
    }
}