package com.sequoiacm.net.version.serial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:Clean the histroy version file,specify the fields in condition
 * are not in the history table. testlink-case:SCM-1676
 *
 * @author wuyan
 * @Date 2018.06.13
 * @modify By wuyan
 * @modify Date 2018.07.26
 * @version 1.10
 */

public class Sche_CleanHisVersionFile1676 extends TestScmBase {
    private final static String taskname = "versionfile_schetask1676";
    private static WsWrapper wsp = null;
    private SiteWrapper cleanSite = null;
    private SiteWrapper lastSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionL = null;
    private ScmWorkspace wsL = null;
    private ScmId fileId = null;
    private String fileName = "fileVersion1676";
    private String authorName = "author1676";
    private byte[] writeData = new byte[ 1024 * 2 ];
    private byte[] updateData = new byte[ 1024 * 5 ];
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
        sessionA = TestScmTools.createSession( cleanSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionL = TestScmTools.createSession( lastSite );
        wsL = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionL );
        fileId = VersionUtils.createFileByStream( wsA, fileName, writeData,
                authorName );
        VersionUtils.updateContentByStream( wsA, fileId, updateData );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        readFileFromM( wsL, currentVersion );
        readFileFromM( wsL, historyVersion );

        // update clean history version file
        createScheduleTask( sessionA );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsL, fileId, true );
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

    private void createScheduleTask( ScmSession session ) throws ScmException {
        String maxStayTime = "0d";
        BSONObject condition = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        try {
            // create schedule task, specify author in condition are not in
            // the history table.
            ScmScheduleContent content = new ScmScheduleCleanFileContent(
                    cleanSite.getSiteName(), maxStayTime, condition,
                    ScopeType.SCOPE_HISTORY );
            String cron = "* * * * * ?";
            ScmSystem.Schedule.create( session, wsp.getName(),
                    ScheduleType.CLEAN_FILE, taskname, "", content, cron );
            Assert.fail( "create clean task must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                Assert.fail( "expErrorCode:-101  actError:" + e.getError()
                        + e.getMessage() );
            }
        }
    }

    private void readFileFromM( ScmWorkspace ws, int version )
            throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, version, 0 );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );
    }

}