package com.sequoiacm.testcommon;

import java.io.File;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

public class ScmTestMultiCenterBase {

    private static int siteId1;
    private static int siteId2;
    private static int siteId3;
    private static ScmTestServerInfo server1;
    private static ScmTestServerInfo server2;
    private static ScmTestServerInfo server3;
    private static String scmUser;
    private static String scmPasswd;

    private static ScmTestServerInfo sdb1;
    private static ScmTestServerInfo sdb2;
    private static ScmTestServerInfo sdb3;
    private static String sdbUser;
    private static String sdbPasswd;

    private static String workspaceName;
    private static String dataDirectory;

    private static String srcFile;

    @Parameters({
        "SITE1",
        "SERVER1",
        "SDB1",

        "SITE2",
        "SERVER2",
        "SDB2",

        "SITE3",
        "SERVER3",
        "SDB3",

        "USERNAME",
        "PASSWORD",

        "SDBUSERNAME",
        "SDBPASSWORD",

        "WORKSPACENAME",
        "DATADIRECTORY"
    })

    @BeforeSuite
    public void initSuite(String SITE1, String SERVER1, String SDB1,
            String SITE2, String SERVER2, String SDB2,
            String SITE3, String SERVER3, String SDB3,
            String USERNAME, String PASSWORD,
            String SDBUSERNAME, String SDBPASSWORD, String WORKSPACENAME, String DATADIRECTORY) {

        siteId1 = Integer.parseInt(SITE1);
        siteId2 = Integer.parseInt(SITE2);
        siteId3 = Integer.parseInt(SITE3);

        server1 = new ScmTestServerInfo(SERVER1);
        server2 = new ScmTestServerInfo(SERVER2);
        server3 = new ScmTestServerInfo(SERVER3);
        scmUser = USERNAME;
        scmPasswd = PASSWORD;

        sdb1 = new ScmTestServerInfo(SDB1);
        sdb2 = new ScmTestServerInfo(SDB2);
        sdb3 = new ScmTestServerInfo(SDB3);
        sdbUser = SDBUSERNAME;
        sdbPasswd = SDBPASSWORD;

        workspaceName = WORKSPACENAME;
        dataDirectory = DATADIRECTORY;

        srcFile = getDataDirectory() + File.separator + "test.txt";
        String randomStr = ScmTestTools.randomString(6);
        ScmTestTools.createFile(srcFile, randomStr, 3*1024*1024);
    }

    public int getSiteId1() {
        return siteId1;
    }

    public int getSiteId2() {
        return siteId2;
    }

    public int getSiteId3() {
        return siteId3;
    }

    public ScmTestServerInfo getServer1() {
        return server1;
    }

    public ScmTestServerInfo getServer2() {
        return server2;
    }

    public ScmTestServerInfo getServer3() {
        return server3;
    }

    public String getScmUser() {
        return scmUser;
    }

    public String getScmPasswd() {
        return scmPasswd;
    }

    public ScmTestServerInfo getSdb1() {
        return sdb1;
    }

    public ScmTestServerInfo getSdb2() {
        return sdb2;
    }

    public ScmTestServerInfo getSdb3() {
        return sdb3;
    }

    public String getSdbUser() {
        return sdbUser;
    }

    public String getSdbPasswd() {
        return sdbPasswd;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public String getSrcFile() {
        return srcFile;
    }
}
