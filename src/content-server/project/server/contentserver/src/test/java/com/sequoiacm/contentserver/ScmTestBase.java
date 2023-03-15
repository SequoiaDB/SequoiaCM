package com.sequoiacm.contentserver;

import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

public class ScmTestBase extends PowerMockTestCase {

    public static String SDB_URL;
    public static String SDB_USER;
    public static String SDB_PASSWD;

    @Parameters({ "MAINSDBURL", "SDBUSER", "SDBPASSWD" })

    @BeforeSuite
    public void initSuite(String MAINSDBURL, String SDBUSER, String SDBPASSWD) {
        SDB_URL = MAINSDBURL;
        SDB_USER = SDBUSER;
        SDB_PASSWD = SDBPASSWD;
    }
}
