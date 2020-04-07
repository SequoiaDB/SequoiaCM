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

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:Clean the current version file,than set Scopetype is history
 * testlink-case:SCM-1805
 *
 * @author wuyan
 * @Date 2018.06.13
 * @version 1.00
 */

public class Sche_UpdateScopeByCleanTask1805a extends TestScmBase {
    private final static String taskname = "versionfile_schetask1805a";
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private List< String > fileIdListStr = new ArrayList<>();
    private File localPath = null;
    private int fileNum = 10;
    private BSONObject condition = null;
    private ScmId scheduleId = null;
    private ScmScheduleCleanFileContent content = null;
    private String cron = null;
    private String fileName = "fileVersion1805a";
    private String authorName = "author1805a";
    private int fileSize1 = 1024 * 100;
    private int fileSize2 = 1024 * 5;
    private String filePath1 = null;
    private String filePath2 = null;
    private byte[] writedata = new byte[ 1024 * 20 ];
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
        readFile( wsM, currentVersion );
        readFile( wsM, historyVersion );

        //clean current version file
        createScheduleTask( sessionA );

        //check siteinfo
        SiteWrapper[] expCurSiteList1 = { rootSite };
        VersionUtils.checkScheTaskFileSites( wsA, fileIdList, currentVersion,
                expCurSiteList1 );
        SiteWrapper[] expHisSiteList1 = { rootSite, branSite };
        VersionUtils.checkScheTaskFileSites( wsM, fileIdList, historyVersion,
                expHisSiteList1 );

        //update task to clean history version file
        updateScheTaskToHisVersionFile();
        //check siteinfo
        SiteWrapper[] expHisSiteList2 = { rootSite };
        VersionUtils.checkScheTaskFileSites( wsM, fileIdList, historyVersion,
                expHisSiteList2 );
        //write current version file again at the branSite
        readFile( wsA, currentVersion );
        //check siteinfo
        SiteWrapper[] expCurSiteList2 = { rootSite, branSite };
        VersionUtils.checkScheTaskFileSites( wsA, fileIdList, currentVersion,
                expCurSiteList2 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmSystem.Schedule.delete( sessionA, scheduleId );
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( wsM, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
                ScmScheduleUtils.cleanTask( sessionA, scheduleId );
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
            fileIdList.add( fileId );
            fileIdListStr.add( fileId.get() );
        }
    }

    private void createScheduleTask( ScmSession session ) throws ScmException {
        String maxStayTime = "0d";
        condition = ScmQueryBuilder.start().put( ScmAttributeName.File.FILE_ID )
                .in( fileIdListStr ).get();
        // create schedule task
        content = new ScmScheduleCleanFileContent( branSite.getSiteName(),
                maxStayTime, condition, ScopeType.SCOPE_CURRENT );
        cron = "* * * * * ?";

        ScmSchedule sche = ScmSystem.Schedule.create( session, wsp.getName(),
                ScheduleType.CLEAN_FILE, taskname, "", content, cron );
        scheduleId = sche.getId();
    }

    private void updateScheTaskToHisVersionFile() throws ScmException {
        ScmSchedule sche = ScmSystem.Schedule.get( sessionA, scheduleId );
        content.setScope( ScopeType.SCOPE_HISTORY );
        Assert.assertEquals( content.getScope(), ScopeType.SCOPE_HISTORY );
        sche.updateContent( content );
    }

    private void readFile( ScmWorkspace ws, int version ) throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            String downloadPath = TestTools.LocalFile
                    .initDownloadPath( localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
            ScmFile file = ScmFactory.File
                    .getInstance( ws, fileId, version, 0 );
            file.getContent( downloadPath );
        }
    }

}