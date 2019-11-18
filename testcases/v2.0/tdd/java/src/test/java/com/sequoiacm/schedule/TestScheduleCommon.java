package com.sequoiacm.schedule;

import org.apache.log4j.Logger;

import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmId;

public class TestScheduleCommon {
    private final static Logger logger = Logger.getLogger(TestScheduleCommon.class);

    public static boolean isScheduleEquals(ScmSchedule sch, ScmSchedule sch1) {
        if (!sch.getCreaateUser().equals(sch1.getCreaateUser())) {
            return false;
        }

        if (!sch.getId().get().equals(sch1.getId().get())) {
            return false;
        }

        if (!sch.getCreateDate().equals(sch1.getCreateDate())) {
            return false;
        }

        if (!sch.getCron().equals(sch1.getCron())) {
            return false;
        }

        if (!sch.getName().equals(sch1.getName())) {
            return false;
        }

        if (!sch.getType().equals(sch1.getType())) {
            return false;
        }

        if (!sch.getWorkspace().equals(sch1.getWorkspace())) {
            return false;
        }

        if (!sch.getContent().equals(sch1.getContent())) {
            return false;
        }

        if (null == sch.getDesc() && null != sch1.getDesc()) {
            return false;
        }

        if (null != sch.getDesc()) {
            if (!sch.getDesc().equals(sch1.getDesc())) {
                return false;
            }
        }

        return true;
    }

    public static void deleteScheduleSilence(ScmSession ss, ScmId scheduleId) {
        try {
            if (null != scheduleId) {
                logger.info("delete scheudle:id=" + scheduleId);
                ScmSystem.Schedule.delete(ss, scheduleId);
            }
        }
        catch (Exception e) {
            logger.warn("delete schedule failed:scheduleId=" + scheduleId, e);
        }
    }
}
