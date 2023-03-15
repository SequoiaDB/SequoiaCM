package com.sequoiacm.infrastructure.common;

import org.testng.annotations.Test;

import java.net.UnknownHostException;

public class NetUtilTest {


    @Test
    public void test() throws UnknownHostException {
        boolean sameHost = NetUtil.isSameHost("localhost", "127.0.0.1");
        System.out.println("great!");
    }
}
