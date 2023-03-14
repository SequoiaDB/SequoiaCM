/**
 *
 */
package com.sequoiacm.scheduletask;

import java.util.ArrayList;
import java.util.List;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description  SCM-1670:异步调度任务指定迁移当前版本文件
 * @author luweikang
 * @createDate 2018.06.13
 * @updateUser ZhangYanan
 * @updateDate 2021.12.09
 * @updateRemark
 * @version v1.0
 */
public class ScheduleAsyncTransferCurVersionFile1670 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId1 = null;
    private ScmId fileId2 = null;
    private ScmId scheduleId = null;
    private List< String > fileIdList = new ArrayList<>();
    private String authorName = "fileVersion1670";
    private String fileName1 = "fileVersion1670_1";
    private String fileName2 = "fileVersion1670_2";
    private String scheduleName = "schedule1670";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = ScmSessionUtils.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = ScmSessionUtils.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId1 = ScmFileUtils.createFileByStream( wsA, fileName1, filedata,
                authorName );
        fileId2 = ScmFileUtils.createFileByStream( wsA, fileName2, filedata,
                authorName );
        VersionUtils.updateContentByStream( wsA, fileId1, updatedata );
        VersionUtils.updateContentByStream( wsA, fileId2, updatedata );
        VersionUtils.updateContentByStream( wsA, fileId2, updatedata );
        fileIdList.add( fileId1.toString() );
        fileIdList.add( fileId2.toString() );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        createScheduleTask();

        ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId1, 2, 2 );

        SiteWrapper[] expSites1 = { branSite };
        VersionUtils.checkSite( wsA, fileId2, 2, expSites1 );

        SiteWrapper[] expSites2 = { rootSite, branSite };
        VersionUtils.checkSite( wsM, fileId1, 2, expSites2 );

        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() throws Exception {
        try {
            ScmSystem.Schedule.delete( sessionA, scheduleId );
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( wsM, fileId1, true );
                ScmFactory.File.deleteInstance( wsM, fileId2, true );
                ScmScheduleUtils.cleanTask( sessionA, scheduleId );
            }
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
                .start( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();
        ScmScheduleContent content = new ScmScheduleCopyFileContent(
                branSite.getSiteName(), rootSite.getSiteName(), "0d", queryCond,
                ScopeType.SCOPE_CURRENT );
        String cron = "* * * * * ?";
        ScmSchedule sche = ScmSystem.Schedule.create( sessionA, wsp.getName(),
                ScheduleType.COPY_FILE, scheduleName, "", content, cron );
        scheduleId = sche.getId();
    }
}
