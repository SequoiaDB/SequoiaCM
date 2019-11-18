package com.sequoiacm.test.schedule;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.test.schedule.common.RestTools;
import com.sequoiacm.test.schedule.common.httpclient.RestToolsHttpClient;

public class TestPrivilege {
    static RestTools rt;

    @BeforeClass
    public static void setUp() {
        rt = new RestToolsHttpClient("http://192.168.20.92:8080/auth-server");
    }

    @Test
    public void testGetVersion() {
        System.out.println(rt.getVersion());
    }

    @Test
    public void listPrivileges() {
        rt.listPrivileges();
    }

    @Test
    public void listUsers() {
        rt.listUsers();
    }

    @AfterClass
    public static void tearDown() {

    }
}
