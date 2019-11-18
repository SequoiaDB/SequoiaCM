package com.sequoiacm.schedule;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestUpdateSchedule extends ScmTestMultiCenterBase {
    private final static Logger logger = Logger.getLogger(TestUpdateSchedule.class);

    private ScmSession ss;
    private ScmId scheduleId;

    @BeforeClass
    public void setUp() throws Exception {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
    }

    @Test
    public void updateSchedule() throws ScmException {

        try {
            ScmSiteInfo rootsite = ScmTestTools.getSiteInfo(ss, getSiteId1());
            ScmSiteInfo site2 = ScmTestTools.getSiteInfo(ss, getSiteId2());
            ScmScheduleContent content = new ScmScheduleCopyFileContent(site2.getName(),
                    rootsite.getName(), "0d", null);
            String cron = "* * 21 * * ?";
            ScmSchedule sch = ScmSystem.Schedule.create(ss, getWorkspaceName(),
                    ScheduleType.COPY_FILE, "s1", "desc", content, cron);
            scheduleId = sch.getId();

            ScmSchedule sch1 = ScmSystem.Schedule.get(ss, scheduleId);
            Assert.assertTrue("sch=" + sch.toString() + ",sch1=" + sch1.toString(),
                    TestScheduleCommon.isScheduleEquals(sch, sch1));

            // *************update name*****************************************
            String newName = "newName";
            logger.info("updating name=" + newName);
            sch1.updateName(newName);

            ScmSchedule sch2 = ScmSystem.Schedule.get(ss, scheduleId);
            Assert.assertTrue("name=" + sch2.getName(), newName.equals(sch2.getName()));
            Assert.assertTrue("sch1=" + sch1.toString() + ",sch2=" + sch2.toString(),
                    TestScheduleCommon.isScheduleEquals(sch1, sch2));
            // *****************************************************************

            // *************update cron*****************************************
            String newCron = "* * 11 * * ?";
            logger.info("updating cron=" + newCron);
            sch1.updateCron(newCron);
            sch2 = ScmSystem.Schedule.get(ss, scheduleId);
            Assert.assertTrue("cron=" + sch2.getCron(), newCron.equals(sch2.getCron()));
            Assert.assertTrue("sch1=" + sch1.toString() + ",sch2=" + sch2.toString(),
                    TestScheduleCommon.isScheduleEquals(sch1, sch2));
            // *****************************************************************

            ScmScheduleContent newContent = new ScmScheduleCleanFileContent(site2.getName(), "1d",
                    null);
            logger.info("updating content=" + newContent.toBSONObject().toString() + ",type="
                    + ScheduleType.CLEAN_FILE);
            sch1.updateSchedule(ScheduleType.CLEAN_FILE, newContent);
            sch2 = ScmSystem.Schedule.get(ss, scheduleId);
            Assert.assertTrue("type=" + sch2.getType(),
                    ScheduleType.CLEAN_FILE.equals(sch2.getType()));
            Assert.assertTrue("content=" + sch2.getContent().toBSONObject().toString(), newContent
                    .toBSONObject().toString().equals(sch2.getContent().toBSONObject().toString()));
            Assert.assertTrue("sch1=" + sch1.toString() + ",sch2=" + sch2.toString(),
                    TestScheduleCommon.isScheduleEquals(sch1, sch2));
        }
        catch (Exception e) {
            logger.error("createSchedule failed", e);
            Assert.fail(e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        TestScheduleCommon.deleteScheduleSilence(ss, scheduleId);
        ss.close();
    }
}
