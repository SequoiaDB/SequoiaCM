package com.sequoiacm.testcommon;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.exception.ScmException;
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

    private static String s3AccessKeyID;
    private static String s3SecretKey;
    private static String s3WorkSpaces;
    private static List<String> gatewayList;

    @Parameters({
            "MAINSDBURL",
            "SDBUSER",
            "SDBPASSWD",

            "SCMUSER",
            "SCMPASSWD",
            "GATEWAYS",

            "DATADIR", "S3ACCESSKEYID", "S3SECRETKEY", "S3WOKERSPACES",
    })


    @BeforeSuite
    public void initSuite(String MAINSDBURL, String SDBUSER, String SDBPASSWD, String SCMUSER,
            String SCMPASSWD, String GATEWAYS, String DATADIR, String S3ACCESSKEYID,
            String S3SECRETKEY, String S3WOKERSPACES) throws ScmException {

        // 解析配置参数
        String sdbUrl = MAINSDBURL;
        sdbUser = SDBUSER;
        sdbPasswd = SDBPASSWD;

        scmUser = SCMUSER;
        scmPasswd = SCMPASSWD;
        gatewayList = Arrays.asList(GATEWAYS.split(","));

        s3AccessKeyID = S3ACCESSKEYID;
        s3SecretKey = S3SECRETKEY;
        s3WorkSpaces = S3WOKERSPACES;

        ScmSession session = ScmFactory.Session
                .createSession(new ScmConfigOption(gatewayList.get(0), scmUser, scmPasswd));
        List<String> siteList = ScmSystem.ServiceCenter.getSiteList(session);

        session = ScmFactory.Session
                .createSession(new ScmConfigOption(gatewayList.get(0) + "/" + siteList.get(0), scmUser, scmPasswd));
        ScmCursor<ScmSiteInfo> siteCursor = ScmFactory.Site.listSite(session);

        boolean hasRootSite = false;
        int siteCount = 0;

        ScmFactory.Workspace.setKeepAliveTime(0);
        ScmSiteInfo currentSite = null;
        while ((currentSite = siteCursor.getNext()) != null) {
            if (++siteCount > 3 && hasRootSite) {
                break;
            }
            if (currentSite.isRootSite()) {
                siteId1 = currentSite.getId();
                server1 = new ScmTestServerInfo(gatewayList.get(0) + "/" + currentSite.getName());
                sdb1 = new ScmTestServerInfo(currentSite.getDataUrl().get(0));
                hasRootSite = true;
            }
            else {
                if (server2 == null) {
                    siteId2 = currentSite.getId();
                    server2 = new ScmTestServerInfo(gatewayList.get(0) + "/" + currentSite.getName());
                    sdb2 = new ScmTestServerInfo(currentSite.getDataUrl().get(0));
                }
                else {
                    siteId3 = currentSite.getId();
                    server3 = new ScmTestServerInfo(gatewayList.get(0) + "/" + currentSite.getName());
                    sdb3 = new ScmTestServerInfo(currentSite.getDataUrl().get(0));
                }
            }
            if (currentSite.getDataType() != ScmType.DatasourceType.SEQUOIADB) {
                System.err.print("current environment is exist non-SDB data source.");
                System.exit(-2);
            }
        }

        if (!hasRootSite) {
            System.err.print("current environment is no root site.");
            System.exit(-1);
        }

        if (siteCount < 3) {
            System.err.print("current environment is less than 3 sites.");
            System.exit(-1);
        }

        // 获取当前已经创建的工作区，自动使用一个3站点以上组成的工作区作为测试工作区，若不存在则报错退出
        ScmCursor<ScmWorkspaceInfo> scmWorkspaceInfoScmCursor = ScmFactory.Workspace.listWorkspace(session);
        ScmWorkspaceInfo currentWorkSpace = null;
        while ((currentWorkSpace = scmWorkspaceInfoScmCursor.getNext()) != null) {
            if (currentWorkSpace.getDataLocation().size() >= 3) {
                workspaceName = currentWorkSpace.getName();
                break;
            }
        }
        if (workspaceName == null) {
            System.err.print("there is no workspace with more than 3 sites in the current environment.");
            System.exit(-3);
        }

        dataDirectory = DATADIR;

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

    public static String getS3AccessKeyID() {
        return s3AccessKeyID;
    }

    public static String getS3SecretKey() {
        return s3SecretKey;
    }

    public static String getS3WorkSpaces() {
        return s3WorkSpaces;
    }

    public static List<String> getGatewayList() {
        return gatewayList;
    }
}
