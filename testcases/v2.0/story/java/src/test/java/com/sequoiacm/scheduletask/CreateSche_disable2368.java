package com.sequoiacm.scheduletask;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-2368:创建禁用的scheduler任务
 * @Author fanyu
 * @Date 2018-04-17
 * @Version 1.00
 */

public class CreateSche_disable2368 extends TestScmBase {
    private ScmSession ssA = null;
    private SiteWrapper branSite = null;
    private WsWrapper wsp = null;
    private ScmId scheduleId = null;
    private String scheduleName = "schedule2368";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        branSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        ssA = TestScmTools.createSession( branSite );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        this.createScheduleTask();
        this.checkScheduleTaskInfo();
        BSONObject bson = ScmQueryBuilder.start( ScmAttributeName.Schedule.ID )
                .is( scheduleId.get() ).get();
        // make sure the schedule is disabled
        for ( int i = 0; i < 60; i++ ) {
            ScmCursor< ScmTaskBasicInfo > cursor = ScmSystem.Task.listTask( ssA,
                    bson );
            Assert.assertFalse( cursor.hasNext() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            ScmSystem.Schedule.delete( ssA, scheduleId );
        } finally {
            if ( ssA != null ) {
                ssA.close();
            }
        }
    }

    private void createScheduleTask() throws ScmException {
        String maxStayTime = "0d";
        // test interface
        BSONObject bson = new BasicBSONObject();
        // create schedule task
        ScmScheduleContent content = new ScmScheduleCleanFileContent(
                branSite.getSiteName(), maxStayTime, bson );
        String cron = "* * * * * ?";
        ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsp.getName(),
                ScheduleType.CLEAN_FILE, scheduleName, "", content, cron,
                false );
        scheduleId = sche.getId();
    }

    private void checkScheduleTaskInfo() throws ScmException {
        ScmSchedule sche = ScmSystem.Schedule.get( ssA, scheduleId );
        Assert.assertEquals( sche.getId(), scheduleId );
        Assert.assertEquals( sche.getType(), ScheduleType.CLEAN_FILE );
        Assert.assertEquals( sche.getName(), scheduleName );
        Assert.assertEquals( sche.getDesc(), "" );
        Assert.assertFalse( sche.isEnable(), sche.toString() );
        Assert.assertEquals( sche.getWorkspace(), wsp.getName() );
    }
}