package com.sequoiacm.net.directory.serial;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmDirUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:create ScheduleCleanTask,match directory to transfer files in
 * the directory testlink-case:SCM-2061
 *
 * @author wuyan
 * @Date 2018.07.13
 * @version 1.00
 */

public class Sche_TransferFileByDir2061 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionT = null;
    private ScmWorkspace wsT = null;
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private int fileNum = 10;
    private ScmId scheduleId = null;

    private ScmDirectory scmDir1;
    private ScmDirectory scmDir2;
    private String fullPath1 = "/CreatefileWiteDir2061a/2061_a/2061_b/2061_c/2061_e/2061_f/";
    private String fullPath2 = "/CreatefileWiteDir2061b/2061_a/2061_b/2061_c/2061_e/2061_f/";
    private String authorName = "CreateFileWithDir2061";
    private String fileName = "filedir2061";
    private String scheduleName = "schedule2061";
    private byte[] writeData1 = new byte[ 1024 * 2 ];
    private byte[] writeData2 = new byte[ 1024 * 5 ];
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        wsp = ScmInfo.getWs();
        // clean file
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( wsp );
        sourceSite = siteList.get( 0 );
        targetSite = siteList.get( 1 );

        sessionA = TestScmTools.createSession( sourceSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionT = TestScmTools.createSession( targetSite );
        wsT = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionT );

        ScmDirUtils.deleteDir( wsA, fullPath1 );
        ScmDirUtils.deleteDir( wsA, fullPath2 );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        scmDir1 = ScmDirUtils.createDir( wsA, fullPath1 );
        writeFileWithDir( wsA, scmDir1, fileIdList1, writeData1 );
        scmDir2 = ScmDirUtils.createDir( wsA, fullPath2 );
        writeFileWithDir( wsA, scmDir2, fileIdList2, writeData2 );

        // transfer file
        createScheduleTask( sessionA, scmDir1 );

        // check siteinfo
        int currentVersion = 1;
        SiteWrapper[] expCurSiteList1 = { targetSite, sourceSite };
        VersionUtils.checkScheTaskFileSites( wsA, fileIdList1, currentVersion,
                expCurSiteList1 );
        SiteWrapper[] expCurSiteList2 = { sourceSite };
        VersionUtils.checkScheTaskFileSites( wsT, fileIdList2, currentVersion,
                expCurSiteList2 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmSystem.Schedule.delete( sessionA, scheduleId );
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( wsT, fileId, true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( wsT, fileId, true );
                }
                ScmScheduleUtils.cleanTask( sessionA, scheduleId );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionT != null ) {
                sessionT.close();
            }
        }
    }

    private void writeFileWithDir( ScmWorkspace ws, ScmDirectory scmDir,
            List< ScmId > fileIdList, byte[] writeData ) throws ScmException {
        new Random().nextBytes( writeData );

        for ( int i = 0; i < fileNum; i++ ) {
            String subfileName = fileName + "_" + i;
            ScmId fileId = createFileWithDir( ws, subfileName, writeData,
                    authorName, scmDir );

            fileIdList.add( fileId );
        }
    }

    private ScmId createFileWithDir( ScmWorkspace ws, String fileName,
            byte[] data, String authorName, ScmDirectory dir )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );

        file.setContent( new ByteArrayInputStream( data ) );
        file.setFileName( fileName );
        file.setAuthor( authorName );
        file.setTitle( "sequoiacm" );
        if ( dir != null ) {
            file.setDirectory( dir );
        }
        file.setMimeType( fileName + ".txt" );
        // add tags
        ScmTags tags = new ScmTags();
        tags.addTag(
                "我是一个标签2061                                                  "
                        + "                                                    "
                        + "                                                    "
                        + "                            "
                        + "                                " );
        tags.addTag( "THIS IS TAG 2061!" );
        tags.addTag( "tag *&^^^^^*90234@#$%!~asf" );
        file.setTags( tags );
        ScmId fileId = file.save();
        return fileId;
    }

    private void createScheduleTask( ScmSession session, ScmDirectory scmDir )
            throws ScmException {
        BSONObject condition = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.DIRECTORY_ID ).in( scmDir.getId() )
                .get();
        ScmScheduleContent content = new ScmScheduleCopyFileContent(
                sourceSite.getSiteName(), targetSite.getSiteName(), "0d",
                condition, ScopeType.SCOPE_CURRENT );
        // create schedule task
        String cron = "* * * * * ?";
        ScmSchedule sche = ScmSystem.Schedule.create( sessionA, wsp.getName(),
                ScheduleType.COPY_FILE, scheduleName, "", content, cron );
        scheduleId = sche.getId();
    }

}