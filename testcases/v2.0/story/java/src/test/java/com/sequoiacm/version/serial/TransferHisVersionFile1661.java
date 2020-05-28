package com.sequoiacm.version.serial;

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
 * test content:Transfer the history version file testlink-case:SCM-1661
 *
 * @author wuyan
 * @Date 2018.06.05
 * @version 1.00
 */

public class TransferHisVersionFile1661 extends TestScmBase {
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

    private String fileName = "fileVersion1661";
    private String authorName = "transfer1661";
    private int fileSize1 = 1024 * 50;
    private int fileSize2 = 1024 * 10;
    private String filePath1 = null;
    private String filePath2 = null;
    private byte[] updateData = new byte[ 1024 * 80 ];
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
        writeAndUpdateFile( wsA );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        ScopeType scopeType = ScopeType.SCOPE_HISTORY;
        startTransferTaskByHistoryVerFile( wsA, sessionA, scopeType );
        checkHistoryVerFileSiteAndDataInfo( wsM, historyVersion );
        checkCurVersionFileInfo( wsA, currentVersion );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                TestSdbTools.Task.deleteMeta( taskId );
                for ( String fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( wsM, new ScmId( fileId ),
                            true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
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
        ScmId fileId = null;
        for ( int i = 0; i < fileNum; i++ ) {
            String subfileName = fileName + "_" + i;

            if ( i % 2 == 0 ) {
                fileId = VersionUtils.createFileByFile( ws, subfileName,
                        filePath1, authorName );
            } else {
                fileId = VersionUtils.createFileByFile( ws, subfileName,
                        filePath2, authorName );

            }
            fileIdList.add( fileId.get() );
            VersionUtils.updateContentByStream( ws, fileId, updateData );

        }
    }

    private void startTransferTaskByHistoryVerFile( ScmWorkspace ws,
            ScmSession session, ScopeType scopeType ) throws Exception {
        BSONObject condition = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.SIZE )
                .greaterThanEquals( fileSize1 )
                .put( ScmAttributeName.File.MAJOR_VERSION ).greaterThan( 0 )
                .get();
        taskId = ScmSystem.Task.startTransferTask( ws, condition, scopeType );

        // wait task finish
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }

    private void checkHistoryVerFileSiteAndDataInfo( ScmWorkspace ws,
            int historyVersion ) throws Exception {
        // check the transfered file,check the sitelist and data
        BSONObject condition = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.SIZE )
                .greaterThanEquals( fileSize1 )
                .put( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_HISTORY, condition );
        int size = 0;
        // check transfered file siteinfo and data
        SiteWrapper[] expHisSiteList = { rootSite, branSite };
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            ScmId fileId = file.getFileId();
            VersionUtils.checkSite( ws, fileId, historyVersion,
                    expHisSiteList );
            VersionUtils.CheckFileContentByFile( ws, fileId, historyVersion,
                    filePath1, localPath );
            size++;
        }
        cursor.close();
        int expFileNum = 5;
        Assert.assertEquals( size, expFileNum );

        // check the no transfer file by history version
        BSONObject condition1 = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.SIZE ).lessThan( fileSize1 )
                .put( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();

        ScmCursor< ScmFileBasicInfo > cursor1 = ScmFactory.File
                .listInstance( ws, ScopeType.SCOPE_HISTORY, condition1 );
        int size1 = 0;
        SiteWrapper[] expCurSiteList1 = { branSite };
        while ( cursor1.hasNext() ) {
            ScmFileBasicInfo file1 = cursor1.getNext();
            ScmId fileId1 = file1.getFileId();
            VersionUtils.checkSite( ws, fileId1, historyVersion,
                    expCurSiteList1 );
            size1++;
        }
        cursor1.close();
        Assert.assertEquals( size1, expFileNum );
    }

    private void checkCurVersionFileInfo( ScmWorkspace ws, int version )
            throws ScmException {
        // all current version file only on the branSite
        BSONObject condition = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_CURRENT, condition );
        SiteWrapper[] expCurSiteList = { branSite };
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            // check results
            ScmId fileId = file.getFileId();
            VersionUtils.checkSite( ws, fileId, version, expCurSiteList );
            size++;
        }
        cursor.close();
        int expFileNums = 10;
        Assert.assertEquals( size, expFileNums );
    }

}