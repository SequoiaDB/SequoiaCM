package com.sequoiacm.scheduletask;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @FileName SCM-1256:更新不存在的任务 SCM-1263:删除不存在的任务 SCM-1259:获取不存在的任务详细信息
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class UDQSche_taskNotExist1256 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( UDQSche_taskNotExist1256.class );
    private final static String name = "schetask1256";
    private ScmSession ssA = null;
    private SiteWrapper branSite = null;
    private WsWrapper wsp = null;
    private BSONObject queryCond = null;
    private ScmId scheduleId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            branSite = ScmInfo.getBranchSite();
            wsp = ScmInfo.getWs();
            ssA = ScmSessionUtils.createSession( branSite );
            queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( name ).get();

            this.createScheduleTask();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        test_deleteTaskNotExist();
        test_updateTaskNotExist();
        test_getTaskNotExist();
    }

    private void test_deleteTaskNotExist() throws Exception {
        ScmSystem.Schedule.delete( ssA, scheduleId );
        try {
            ScmSystem.Schedule.delete( ssA, scheduleId );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "delete not exist schedule, errorMsg = ["
                    + e.getError() + "]" );
        }
    }

    private void test_updateTaskNotExist() throws Exception {
        try {
            ScmSchedule sche = ScmSystem.Schedule.get( ssA, scheduleId );
            sche.updateName( "test" );
            Assert.fail( "expect fail but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "update not exist schedule, errorMsg = ["
                    + e.getError() + "]" );
        }
    }

    private void test_getTaskNotExist() throws Exception {
        try {
            ScmSystem.Schedule.get( ssA, scheduleId );
            Assert.fail( "expect fail but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "Get not exist schedule, errorMsg = [" + e.getError()
                    + "]" );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            ScmScheduleUtils.cleanTask( ssA, scheduleId );
        } finally {
            if ( ssA != null ) {
                ssA.close();
            }
        }
    }

    private void createScheduleTask() {
        try {
            String cron = "* * * * * ?";
            String maxStayTime = "0d";
            ScheduleType taskType = ScheduleType.CLEAN_FILE;
            ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
                    branSite.getSiteName(), maxStayTime, queryCond );
            ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsp.getName(),
                    taskType, name, "desc", content, cron );
            scheduleId = sche.getId();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}