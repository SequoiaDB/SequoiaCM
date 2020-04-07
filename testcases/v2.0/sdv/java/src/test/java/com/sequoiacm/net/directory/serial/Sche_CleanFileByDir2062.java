package com.sequoiacm.net.directory.serial;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
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
 * test content:create ScheduleCleanTask,match directory to clean up files in
 * the directory
 * testlink-case:SCM-2062
 *
 * @author wuyan
 * @Date 2018.07.13
 * @version 1.00
 * @modify Date 2018.07.30
 * @version 1.10
 */

public class Sche_CleanFileByDir2062 extends TestScmBase {
    private final static String taskname = "schetask2062";
    private static WsWrapper wsp = null;
    private SiteWrapper cleanSite = null;
    private SiteWrapper lastSite = null;
    private ScmSession sessionC = null;
    private ScmWorkspace wsC = null;
    private ScmSession sessionL = null;
    private ScmWorkspace wsL = null;
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private int fileNum = 10;
    private BSONObject condition = null;
    private ScmId scheduleId = null;
    private ScmScheduleCleanFileContent content = null;
    private String cron = null;
    private ScmDirectory scmDir1;
    private ScmDirectory scmDir2;
    private String fullPath1 =
            "/CreatefileWiteDir2062a/2062_a/2062_b/2062_c/2062_e/2062_f/";
    private String fullPath2 =
            "/CreatefileWiteDir2062b/2062_a/2062_b/2062_c/2062_e/2062_f/";
    private String authorName = "CreateFileWithDir2062";
    private String fileName = "filedir2060";
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

        cleanSite = ScmNetUtils.getNonLastSite( wsp );
        lastSite = ScmNetUtils.getLastSite( wsp );

        sessionC = TestScmTools.createSession( cleanSite );
        wsC = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionC );
        sessionL = TestScmTools.createSession( lastSite );
        wsL = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionL );

        ScmDirUtils.deleteDir( wsC, fullPath1 );
        ScmDirUtils.deleteDir( wsC, fullPath2 );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        scmDir1 = ScmDirUtils.createDir( wsC, fullPath1 );
        writeFileWithDir( wsC, scmDir1, fileIdList1, writeData1 );
        scmDir2 = ScmDirUtils.createDir( wsC, fullPath2 );
        writeFileWithDir( wsC, scmDir2, fileIdList2, writeData2 );

        readFileFromSouceSite( wsL, fileIdList1 );
        readFileFromSouceSite( wsL, fileIdList2 );

        //clean current version file
        createScheduleTask( sessionC );

        //check siteinfo
        int currentVersion = 1;
        SiteWrapper[] expCurSiteList1 = { lastSite };
        VersionUtils.checkScheTaskFileSites( wsC, fileIdList1, currentVersion,
                expCurSiteList1 );
        SiteWrapper[] expCurSiteList2 = { lastSite, cleanSite };
        VersionUtils.checkScheTaskFileSites( wsL, fileIdList2, currentVersion,
                expCurSiteList2 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmSystem.Schedule.delete( sessionC, scheduleId );
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( wsL, fileId, true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( wsL, fileId, true );
                }
                ScmScheduleUtils.cleanTask( sessionC, scheduleId );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( sessionC != null ) {
                sessionC.close();
            }
            if ( sessionL != null ) {
                sessionL.close();
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
            byte[] data, String authorName,
            ScmDirectory dir ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );

        file.setContent( new ByteArrayInputStream( data ) );
        file.setFileName( fileName );
        file.setAuthor( authorName );
        file.setTitle( "sequoiacm" );
        if ( dir != null ) {
            file.setDirectory( dir );
        }
        file.setMimeType( fileName + ".txt" );
        //add tags
        ScmTags tags = new ScmTags();
        tags.addTag(
                "我是一个标签2062                                                  " +
                        "                                                    " +
                        "                                                    " +
                        "                            "
                        + "                                " );
        tags.addTag( "THIS IS TAG 2062!" );
        tags.addTag( "tag *&^^^^^*90234@#$%!~asf" );
        file.setTags( tags );
        ScmId fileId = file.save();
        return fileId;
    }

    private void createScheduleTask( ScmSession session ) throws ScmException {
        String maxStayTime = "0d";
        condition = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.DIRECTORY_ID ).in( scmDir1.getId() )
                .get();
        // create schedule task
        content = new ScmScheduleCleanFileContent( cleanSite.getSiteName(),
                maxStayTime, condition );
        content.setScope( ScopeType.SCOPE_CURRENT );
        cron = "* * * * * ?";

        ScmSchedule sche = ScmSystem.Schedule.create( session, wsp.getName(),
                ScheduleType.CLEAN_FILE, taskname, "", content, cron );
        scheduleId = sche.getId();
    }

    private void readFileFromSouceSite( ScmWorkspace ws,
            List< ScmId > fileIdList ) throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            file.getContent( outputStream );
        }
    }

}