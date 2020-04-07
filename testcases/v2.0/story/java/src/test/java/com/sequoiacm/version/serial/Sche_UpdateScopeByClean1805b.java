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
 * test content:update schedule Clean task,
 *              test b: set Scopetype from history to current
 *              test c: set Scopetype from current to all
 * testlink-case:SCM-1805b
 *
 * @author wuyan
 * @Date 2018.06.14
 * @version 1.00
 */

public class Sche_UpdateScopeByClean1805b extends TestScmBase {
    private final static String taskname = "versionfile_schetask1805b";
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
    private String fileName = "fileVersion1805b";
    private String authorName = "author1805b";
    private int fileSize1 = 1024 * 20;
    private int fileSize2 = 1024 * 5;
    private String filePath1 = null;
    private String filePath2 = null;
    private byte[] writedata = new byte[ 1024 * 10 ];
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
        SiteWrapper[] exphHisSiteList = { rootSite };
        VersionUtils.checkScheTaskFileSites( wsM, fileIdList, historyVersion,
                exphHisSiteList );

        //test b:update task to clean history version file
        ScopeType scope_b = ScopeType.SCOPE_CURRENT;
        updateScheTaskByScopeType( wsA, scope_b );
        //read history version file again at the branSite
        readFile( wsA, historyVersion );

        //test b: check siteinfo
        SiteWrapper[] expCurSiteList_b = { rootSite };
        VersionUtils.checkScheTaskFileSites( wsA, fileIdList, currentVersion,
                expCurSiteList_b );
        SiteWrapper[] exphHisSiteList_b = { rootSite, branSite };
        VersionUtils.checkScheTaskFileSites( wsM, fileIdList, historyVersion,
                exphHisSiteList_b );

        //test c:update task to clean all version file
        ScopeType scope_c = ScopeType.SCOPE_ALL;
        updateScheTaskByScopeType( wsA, scope_c );
        //read current version file again at the branSite
        readFile( wsA, currentVersion );

        //test b: check siteinfo
        SiteWrapper[] expCurSiteList_c = { rootSite };
        VersionUtils.checkScheTaskFileSites( wsA, fileIdList, currentVersion,
                expCurSiteList_c );
        SiteWrapper[] exphHisSiteList_c = { rootSite };
        VersionUtils.checkScheTaskFileSites( wsM, fileIdList, historyVersion,
                exphHisSiteList_c );
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
                maxStayTime, condition, ScopeType.SCOPE_HISTORY );
        cron = "* * * * * ?";

        ScmSchedule sche = ScmSystem.Schedule.create( session, wsp.getName(),
                ScheduleType.CLEAN_FILE, taskname, "", content, cron );
        scheduleId = sche.getId();
        Assert.assertEquals( content.getScope(), ScopeType.SCOPE_HISTORY );
    }

    private void updateScheTaskByScopeType( ScmWorkspace ws, ScopeType scope )
            throws ScmException {
        ScmSchedule sche = ScmSystem.Schedule.get( sessionA, scheduleId );
        content.setScope( scope );
        Assert.assertEquals( content.getScope(), scope );
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