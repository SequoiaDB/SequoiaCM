/**
 *
 */
package com.sequoiacm.version.serial;

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
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description ScheduleUpdateScopeType1807a.java
 * @author luweikang
 * @date 2018年6月15日
 */
public class ScheduleUpdateScopeType1807c extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;
    private ScmId scheduleId = null;

    private String fileName = "fileVersion1807";
    private String scheduleName = "schedule1807c";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );
        VersionUtils.updateContentByStream( wsA, fileId, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {

        createScheduleTask();

        VersionUtils.waitAsyncTaskFinished( wsM, fileId, 2, 2 );

        changeScheduleType();

        VersionUtils.waitAsyncTaskFinished( wsM, fileId, 1, 2 );

        SiteWrapper[] expSites = { rootSite, branSite };
        VersionUtils.checkSite( wsM, fileId, 1, expSites );

        VersionUtils.checkSite( wsM, fileId, 2, expSites );

        runSuccess = true;

    }

    @AfterClass
    private void tearDown() {
        try {
            ScmSystem.Schedule.delete( sessionM, scheduleId );
            if ( runSuccess ) {
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

    private void createScheduleTask() throws ScmException {
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileId.toString() )
                .get();
        ScmScheduleContent content = new ScmScheduleCopyFileContent(
                branSite.getSiteName(), rootSite.getSiteName(), "0d", queryCond,
                ScopeType.SCOPE_CURRENT );
        String cron = "* * * * * ?";
        ScmSchedule sche = ScmSystem.Schedule.create( sessionA, wsp.getName(),
                ScheduleType.COPY_FILE, scheduleName, "", content, cron );
        scheduleId = sche.getId();
    }

    private void changeScheduleType() throws ScmException {
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileId.toString() )
                .get();
        ScmScheduleContent content = new ScmScheduleCopyFileContent(
                branSite.getSiteName(), rootSite.getSiteName(), "0d", queryCond,
                ScopeType.SCOPE_ALL );
        ScmSchedule sche = ScmSystem.Schedule.get( sessionA, scheduleId );
        sche.updateContent( content );
    }
}
