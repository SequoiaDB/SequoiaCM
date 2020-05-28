package com.sequoiacm.version.serial;

import java.io.ByteArrayOutputStream;
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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:Clean the current version file,than set Scopetype is history or
 * all version, specify the filed in condition are not in the history table.
 * testlink-case:SCM-1806
 *
 * @author wuyan
 * @Date 2018.06.13
 * @version 1.00
 */

public class Sche_UpdateScopeByClean1806 extends TestScmBase {
    private final static String taskname = "versionfile_schetask1806";
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;
    private BSONObject condition = null;
    private ScmId scheduleId = null;
    private ScmScheduleCleanFileContent content = null;
    private String cron = null;
    private String fileName = "fileVersion1806";
    private String authorName = "author1806";
    private byte[] writeData = new byte[ 1024 * 10 ];
    private byte[] updateData = new byte[ 1024 * 20 ];
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        fileId = VersionUtils.createFileByStream( wsA, fileName, writeData,
                authorName );
        VersionUtils.updateContentByStream( wsA, fileId, updateData );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        readFile( wsM, currentVersion );
        readFile( wsM, historyVersion );

        // clean current version file
        createScheduleTask( sessionA );

        // update task to clean history version file
        updateScheTaskToHisVersionFile();
        // write current version file again at the branSite
        updateScheTaskToAllVersionFile();

        // check clean current version file result
        SiteWrapper[] expCurSiteList = { rootSite };
        List< ScmId > fileIdList = new ArrayList<>();
        fileIdList.add( fileId );
        VersionUtils.checkScheTaskFileSites( wsA, fileIdList, currentVersion,
                expCurSiteList );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmSystem.Schedule.delete( sessionA, scheduleId );
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
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

    private void createScheduleTask( ScmSession session ) throws ScmException {
        String maxStayTime = "0d";
        condition = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        // create schedule task
        content = new ScmScheduleCleanFileContent( branSite.getSiteName(),
                maxStayTime, condition, ScopeType.SCOPE_CURRENT );
        cron = "* * * * * ?";

        ScmSchedule sche = ScmSystem.Schedule.create( session, wsp.getName(),
                ScheduleType.CLEAN_FILE, taskname, "", content, cron );
        scheduleId = sche.getId();
        Assert.assertEquals( content.getScope(), ScopeType.SCOPE_CURRENT );
    }

    private void updateScheTaskToHisVersionFile() throws ScmException {
        try {
            ScmSchedule sche = ScmSystem.Schedule.get( sessionA, scheduleId );
            content.setScope( ScopeType.SCOPE_HISTORY );
            sche.updateContent( content );
            Assert.fail( "update scopeType to SCOPE_HISTORY must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.HTTP_BAD_REQUEST != e.getError() ) {
                Assert.fail( "expErrorCode:400  actError:" + e.getError()
                        + e.getMessage() );
            }
        }
    }

    private void updateScheTaskToAllVersionFile() throws ScmException {

        try {
            ScmSchedule sche = ScmSystem.Schedule.get( sessionA, scheduleId );
            content.setScope( ScopeType.SCOPE_ALL );
            sche.updateContent( content );
            Assert.fail( "update scopeType to SCOPE_ALL must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.HTTP_BAD_REQUEST != e.getError() ) {
                Assert.fail( "expErrorCode:400  actError:" + e.getError()
                        + e.getMessage() );
            }
        }
    }

    private void readFile( ScmWorkspace ws, int version ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, version, 0 );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );
    }

}