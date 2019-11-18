package com.sequoiacm.tools.element;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.common.ScmMetaMgr;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;

public class ReloadResInfo {
    private List<String> nodeList = new ArrayList<>();
    private List<String> serverNameList = new ArrayList<>();
    private List<String> siteNameList = new ArrayList<>();
    private List<String> errorMsgList = new ArrayList<>();
    private List<String> siteIdList = new ArrayList<>();
    private List<String> serverIdList = new ArrayList<>();
    private final String NODE = "Node";
    private final String SERVERNAME = "ServerName";
    private final String SERVERID = "ServerId";
    private final String SITENAME = "SiteName";
    private final String SITEID = "SiteId";
    private final String RESULT = "Result";
    private int maxNodeLength = NODE.length() + 1;
    private int maxSiteNameLength = SITENAME.length() + 1;
    private int maxServerNameLength = SERVERNAME.length() + 1;
    private int maxSiteIdLength = SITEID.length() + 1;
    private int maxServerIdLength = SERVERID.length() + 1;
    private ScmMetaMgr mg;
    private boolean haveFailedHost = false;

    public ReloadResInfo(ScmMetaMgr mg) throws ScmToolsException {
        this.mg = mg;
    }

    public boolean haveFailedHost() {
        return haveFailedHost;
    }

    public void addErrorRow(String Node, String serverName, String serverId, String siteName,
            String siteID, String errorMsg) {
        nodeList.add(Node);
        serverNameList.add(serverName);
        serverIdList.add(serverId);
        siteNameList.add(siteName);
        siteIdList.add(siteID);
        errorMsgList.add(errorMsg);
    }

    public void addReloadResBson(BSONObject obj) throws ScmToolsException {
        String host = (String) getValueWhithChecked(obj, "hostname");
        int port = (int) getValueWhithChecked(obj, "port");
        nodeList.add(host + ":" + port);
        if ((host + ":" + port).length() >= maxNodeLength) {
            maxNodeLength = (host + ":" + port).length() + 1;
        }
        int siteId = (int) getValueWhithChecked(obj, "site_id");
        if ((siteId + "").length() >= maxSiteIdLength) {
            maxSiteIdLength = (siteId + "").length() + 1;
        }
        siteIdList.add(siteId + "");
        String siteName = mg.getSiteNameById(siteId);
        if (siteName.length() >= maxSiteNameLength) {
            maxSiteNameLength = siteName.length() + 1;
        }
        siteNameList.add(siteName);
        int serverId = (int) getValueWhithChecked(obj, "server_id");
        serverIdList.add(serverId + "");
        if ((serverId + "").length() >= maxServerIdLength) {
            maxServerIdLength = (serverId + "").length() + 1;
        }
        String serverName = mg.getContenserverNameById(serverId);
        if (serverName.length() >= maxServerNameLength) {
            maxServerNameLength = serverName.length() + 1;
        }
        serverNameList.add(serverName);
        int flag = (int) getValueWhithChecked(obj, "flag");
        if (flag == 0) {
            errorMsgList.add("Success");
        }
        else {
            haveFailedHost = true;
            String errorMsg = (String) getValueWhithChecked(obj, "errormsg");
            errorMsgList.add(flag + "," + errorMsg);
        }
    }

    public void addReloadResList(List<BSONObject> list) throws ScmToolsException {
        for (BSONObject obj : list) {
            addReloadResBson(obj);
        }
    }

    public void printRes() {
        // print head
        System.out.print(NODE);
        ScmCommon.printSpace(maxNodeLength - NODE.length());
        System.out.print(SERVERNAME);
        ScmCommon.printSpace(maxServerNameLength - SERVERNAME.length());
        System.out.print(SERVERID);
        ScmCommon.printSpace(maxServerIdLength - SERVERID.length());
        System.out.print(SITENAME);
        ScmCommon.printSpace(maxSiteNameLength - SITENAME.length());
        System.out.print(SITEID);
        ScmCommon.printSpace(maxSiteIdLength - SITEID.length());
        System.out.println(RESULT);

        for (int i = 0; i < nodeList.size(); i++) {
            System.out.print(nodeList.get(i));
            ScmCommon.printSpace(maxNodeLength - nodeList.get(i).length());
            System.out.print(serverNameList.get(i));
            ScmCommon.printSpace(maxServerNameLength - serverNameList.get(i).length());
            System.out.print(serverIdList.get(i));
            ScmCommon.printSpace(maxServerIdLength - serverIdList.get(i).toString().length());
            System.out.print(siteNameList.get(i));
            ScmCommon.printSpace(maxSiteNameLength - siteNameList.get(i).length());
            System.out.print(siteIdList.get(i));
            ScmCommon.printSpace(maxSiteIdLength - siteIdList.get(i).toString().length());
            System.out.println(errorMsgList.get(i));
        }

        System.out.println("Total:" + nodeList.size());
    }

    private Object getValueWhithChecked(BSONObject obj, String key) throws ScmToolsException {
        Object robj = obj.get(key);
        if (robj == null) {
            throw new ScmToolsException("Failed to analyze reload response,missing key:" + key,
                    ScmExitCode.COMMON_UNKNOW_ERROR);
        }
        return robj;
    }
}
