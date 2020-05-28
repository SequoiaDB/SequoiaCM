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
 * test content:Transfer the current version file testlink-case:SCM-1660
 *
 * @author wuyan
 * @Date 2018.06.05
 * @modify Date 2018.07.26
 * @version 1.10
 */

public class TransferCurVersionFile1660 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private ScmSession sessionS = null;
    private ScmWorkspace wsS = null;
    private ScmSession sessionT = null;
    private ScmWorkspace wsT = null;
    private ScmId taskId = null;
    private List< String > fileIdList = new ArrayList< String >();
    private File localPath = null;
    private int fileNum = 10;
    private BSONObject condition = null;

    private String fileName = "fileVersion1660";
    private String authorName = "author1660";
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

        wsp = ScmInfo.getWs();
        // clean file
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( wsp );
        sourceSite = siteList.get( 0 );
        targetSite = siteList.get( 1 );

        sessionS = TestScmTools.createSession( sourceSite );
        wsS = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionS );
        sessionT = TestScmTools.createSession( targetSite );
        wsT = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionT );
        writeAndUpdateFile( wsS );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        ScopeType scopeType = ScopeType.SCOPE_CURRENT;
        startTransferTaskByCurrentVerFile( wsS, sessionS, scopeType );

        checkCurrentVerFileSiteAndDataInfo( wsS, currentVersion );
        checkHisVersionFileInfo( wsT, historyVersion );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                TestSdbTools.Task.deleteMeta( taskId );
                for ( String fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( wsT, new ScmId( fileId ),
                            true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( sessionS != null ) {
                sessionS.close();
            }
            if ( sessionT != null ) {
                sessionT.close();
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

    private void startTransferTaskByCurrentVerFile( ScmWorkspace ws,
            ScmSession session, ScopeType scopeType ) throws Exception {
        condition = ScmQueryBuilder.start().put( ScmAttributeName.File.SIZE )
                .greaterThanEquals( fileSize1 )
                .put( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        taskId = ScmSystem.Task.startTransferTask( ws, condition, scopeType,
                targetSite.getSiteName() );

        // wait task finish
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }

    private void checkCurrentVerFileSiteAndDataInfo( ScmWorkspace ws,
            int currentVersion ) throws Exception {
        // check the transfered file,check the sitelist and data
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_CURRENT, condition );
        int size = 0;
        ScmFileBasicInfo file;
        List< ScmId > transferfileIdList = new ArrayList< ScmId >();
        while ( cursor.hasNext() ) {
            file = cursor.getNext();
            ScmId fileId = file.getFileId();
            transferfileIdList.add( fileId );
            size++;
        }
        cursor.close();
        int expFileNum = 5;
        Assert.assertEquals( size, expFileNum );

        // check transfered file siteinfo and data
        SiteWrapper[] expCurSiteList = { targetSite, sourceSite };
        ScmFileUtils.checkMetaAndData( wsp, transferfileIdList, expCurSiteList,
                localPath, filePath1 );

        // check the no transfer file by current version
        BSONObject condition1 = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.SIZE ).lessThan( fileSize1 )
                .put( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        ScmCursor< ScmFileBasicInfo > cursor1 = ScmFactory.File
                .listInstance( ws, ScopeType.SCOPE_CURRENT, condition1 );
        int size1 = 0;
        SiteWrapper[] expCurSiteList1 = { sourceSite };
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
            throws ScmException {
        // all history version file only on the branSite
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_CURRENT, condition );
        SiteWrapper[] expHisSiteList = { sourceSite };
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            // check results
            ScmId fileId = file.getFileId();
            VersionUtils.checkSite( ws, fileId, version, expHisSiteList );
            size++;
        }
        cursor.close();
        int expFileNums = 10;
        Assert.assertEquals( size, expFileNums );
    }

}