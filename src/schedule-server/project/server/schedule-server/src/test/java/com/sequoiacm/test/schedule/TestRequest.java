package com.sequoiacm.test.schedule;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.test.schedule.common.RestTestDefine;
import com.sequoiacm.test.schedule.common.RestTools;
import com.sequoiacm.test.schedule.common.httpclient.RestToolsHttpClient;

public class TestRequest {
    static RestTools rt;

    @BeforeClass
    public static void setUp() {
        rt = new RestToolsHttpClient(RestTestDefine.SCHEDULE_SERVICE_URL);
    }

    public void testLogin() {
        System.out.println(rt.login("user", "passwd"));
    }

    public void getName() {
        System.out.println(rt.getName("nihao"));
    }

    public void createSchedule() {
        BSONObject extra = new BasicBSONObject();
        BSONObject content = new BasicBSONObject();
        content.put(FieldName.Schedule.FIELD_CLEAN_SITE, "branchSite1");
        content.put(FieldName.Schedule.FIELD_MAX_STAY_TIME, "3d");
        rt.createSchedule("name", "desc", ScheduleDefine.ScheduleType.CLEAN_FILE, content,
                "ws_default", "* * * * * ?");
    }

    public void listSchedule() {
        rt.listSchedule();
    }

    public void getSchedule() {
        rt.getSchedule("5ac1e10f0000650000260001");
    }

    @Test
    public void deleteSchedule() {
        rt.deleteSchedule("5ac9b9b60000650000250001");
    }

    @AfterClass
    public static void tearDown() {

    }
}
