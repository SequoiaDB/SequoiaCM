package com.sequoiacm.schedule;

import com.sequoiacm.client.core.*;
import org.apache.log4j.Logger;
import org.bson.BasicBSONObject;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

import java.util.UUID;

public class TestListSchedule extends ScmTestMultiCenterBase {
    private final static Logger logger = Logger.getLogger(TestListSchedule.class);

    private ScmSession ss;
    private ScmId scheduleId;
    private ScmId scheduleId2;

    @BeforeClass
    public void setUp() throws Exception {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
    }

    @Test
    public void listSchedule() throws ScmException {
        ScmCursor<ScmScheduleBasicInfo> cursor = null;
        try {
            cursor = ScmSystem.Schedule.list(ss, new BasicBSONObject(
                    ScmAttributeName.Schedule.NAME, "not exist"));
            Assert.assertFalse(cursor.hasNext());
            cursor.close();

            String name = UUID.randomUUID().toString();
            ScmSiteInfo rootsite = ScmTestTools.getSiteInfo(ss, getSiteId1());
            ScmSiteInfo site2 = ScmTestTools.getSiteInfo(ss, getSiteId2());
            ScmScheduleContent content = new ScmScheduleCopyFileContent(site2.getName(),
                    rootsite.getName(), "0d", null);
            String cron = "* * 21 * * ?";
            ScmSchedule sch = ScmSystem.Schedule.create(ss, getWorkspaceName(),
                    ScheduleType.COPY_FILE, name, "desc", content, cron);
            scheduleId = sch.getId();

            String cron2 = "* * 11 * * ?";
            ScmSchedule sch2 = ScmSystem.Schedule.create(ss, getWorkspaceName(),
                    ScheduleType.COPY_FILE, name, "desc", content, cron2);
            scheduleId2 = sch2.getId();

            cursor = ScmSystem.Schedule.list(ss, new BasicBSONObject(
                    ScmAttributeName.Schedule.NAME, name));
            ScmScheduleBasicInfo basicInfo = cursor.getNext();
            logger.info("info=" + basicInfo);

            ScmScheduleBasicInfo basicInfo2 = cursor.getNext();
            logger.info("info2=" + basicInfo2);

            Assert.assertFalse(cursor.hasNext());
            Assert.assertNotEquals(basicInfo.getCron(), basicInfo2.getCron());
        }
        catch (Exception e) {
            logger.error("createSchedule failed", e);
            Assert.fail(e.getMessage());
        }
        finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        TestScheduleCommon.deleteScheduleSilence(ss, scheduleId);
        TestScheduleCommon.deleteScheduleSilence(ss, scheduleId2);
        ss.close();
    }
}