/**
 *
 */
package com.sequoiacm.net.version.serial;

import java.io.IOException;
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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description 异步迁移任务，更新scopeType类型 test b:原任务scopeType为历史版本，更新为当前版本
 * @author luweikang
 * @date 2018年6月15日
 * @modify By wuyan
 * @modify Date 2018.07.26
 * @version 1.10
 */
public class ScheduleUpdateScopeType1807b extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private ScmSession sessionS = null;
    private ScmSession sessionT = null;
    private ScmWorkspace wsS = null;
    private ScmWorkspace wsT = null;
    private ScmId fileId = null;
    private ScmId scheduleId = null;

    private String fileName = "fileVersion1807b";
    private String scheduleName = "schedule1807b";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        wsp = ScmInfo.getWs();
        // clean file
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( wsp );
        sourceSite = siteList.get( 0 );
        targetSite = siteList.get( 1 );
        sessionS = TestScmTools.createSession( sourceSite );
        wsS = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionS );
        sessionT = TestScmTools.createSession( targetSite );
        wsT = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionT );

        fileId = VersionUtils.createFileByStream( wsS, fileName, filedata );
        VersionUtils.updateContentByStream( wsS, fileId, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {

        createScheduleTask();

        VersionUtils.waitAsyncTaskFinished( wsT, fileId, 1, 2 );

        changeScheduleType();

        VersionUtils.waitAsyncTaskFinished( wsT, fileId, 2, 2 );

        SiteWrapper[] expSites = { targetSite, sourceSite };
        VersionUtils.checkSite( wsT, fileId, 1, expSites );

        VersionUtils.checkSite( wsT, fileId, 2, expSites );

        runSuccess = true;

    }

    @AfterClass
    private void tearDown() {
        try {
            ScmSystem.Schedule.delete( sessionT, scheduleId );
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( wsT, fileId, true );
                ScmScheduleUtils.cleanTask( sessionS, scheduleId );
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

    private void createScheduleTask() throws ScmException {
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileId.toString() )
                .get();
        ScmScheduleContent content = new ScmScheduleCopyFileContent(
                sourceSite.getSiteName(), targetSite.getSiteName(), "0d",
                queryCond, ScopeType.SCOPE_HISTORY );
        String cron = "* * * * * ?";
        ScmSchedule sche = ScmSystem.Schedule.create( sessionS, wsp.getName(),
                ScheduleType.COPY_FILE, scheduleName, "", content, cron );
        scheduleId = sche.getId();
    }

    private void changeScheduleType() throws ScmException {
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileId.toString() )
                .get();
        ScmScheduleContent content = new ScmScheduleCopyFileContent(
                sourceSite.getSiteName(), targetSite.getSiteName(), "0d",
                queryCond, ScopeType.SCOPE_CURRENT );
        ScmSchedule sche = ScmSystem.Schedule.get( sessionS, scheduleId );
        sche.updateContent( content );
    }
}
