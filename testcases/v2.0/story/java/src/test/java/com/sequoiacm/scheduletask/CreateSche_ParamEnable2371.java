package com.sequoiacm.scheduletask;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-2371:将删除的schedule任务，设置禁用或启用
 * @Author fanyu
 * @Date 2019-01-28
 * @Version 1.00
 */

public class CreateSche_ParamEnable2371 extends TestScmBase {
    private ScmSession ssA = null;
    private SiteWrapper branSite = null;
    private WsWrapper wsp = null;
    private String scheduleName = "schedule2371";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        branSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        ssA = TestScmTools.createSession( branSite );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmSchedule schedule = this.createScheduleTask();
        schedule.delete();
        try {
            schedule.disable();
            Assert.fail( "exp fail but act success,scheduleId = " +
                    schedule.getId().get() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }

        try {
            schedule.enable();
            Assert.fail( "exp fail but act success,scheduleId = " +
                    schedule.getId().get() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        if ( ssA != null ) {
            ssA.close();
        }
    }

    private ScmSchedule createScheduleTask() throws ScmException {
        String maxStayTime = "0d";
        // test interface
        BSONObject bson = new BasicBSONObject();
        // create schedule task
        ScmScheduleContent content = new ScmScheduleCleanFileContent(
                branSite.getSiteName(), maxStayTime, bson );
        String cron = "* * * * * ?";
        ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsp.getName(),
                ScheduleType.CLEAN_FILE, scheduleName, "", content, cron );
        return sche;
    }
}