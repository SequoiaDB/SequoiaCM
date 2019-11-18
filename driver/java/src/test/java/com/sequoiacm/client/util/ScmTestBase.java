package com.sequoiacm.client.util;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

public class ScmTestBase {

    protected static String url;
    protected static String user;
    protected static String password;
    protected static String workspaceName;
    protected static String dataDirectory;
    
    protected static String sdbUrl;
    protected static String sdbUser;
    protected static String sdbPasswd;

    @Parameters({ "URL", "USER", "PASSWORD", "WORKSPACENAME", "DATADIRECTORY",
        "SDBURL", "SDBUSER", "SDBPASSWD"})
    @BeforeSuite
    public static void initSuite(String URL, String USER, String PASSWORD,
            String WORKSPACENAME, String DATADIRECTORY, String SDBURL, String SDBUSER, 
            String SDBPASSWD) {
        url = URL;
        user = USER;
        password = PASSWORD;
        workspaceName = WORKSPACENAME;
        dataDirectory = DATADIRECTORY;
        sdbUrl = SDBURL;
        sdbUser = SDBUSER;
        sdbPasswd = SDBPASSWD;
    }
}
