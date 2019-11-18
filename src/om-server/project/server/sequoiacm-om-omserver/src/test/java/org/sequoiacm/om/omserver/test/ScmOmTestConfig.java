package org.sequoiacm.om.omserver.test;

import java.util.Arrays;
import java.util.List;

public class ScmOmTestConfig {
    private String workspaceName1 = "ws_default";
    private String workspaceName2 = "ws_none";
    private String workspaceName3 = "ws_month";

    private String rootSite = "rootsite";

    private List<String> gatewayAddr = Arrays.asList("192.168.31.32:8080");
    private String scmUser = "admin";
    private String scmPassword = "01F56BC56488BBE8D425";
    private String scmSrcPassword = "admin";
    private String myZone = "zone1";
    private String myRegion = "beijing";

    private String omserverAddr = "localhost:8800";

    public String getScmSrcPassword() {
        return scmSrcPassword;
    }

    public void setScmSrcPassword(String scmSrcPassword) {
        this.scmSrcPassword = scmSrcPassword;
    }

    public String getOmserverAddr() {
        return omserverAddr;
    }

    public void setOmserverAddr(String omserverAddr) {
        this.omserverAddr = omserverAddr;
    }

    public String getRootSite() {
        return rootSite;
    }

    public void setRootSite(String rootSite) {
        this.rootSite = rootSite;
    }

    public String getMyZone() {
        return myZone;
    }

    public void setMyZone(String myZone) {
        this.myZone = myZone;
    }

    public String getMyRegion() {
        return myRegion;
    }

    public void setMyRegion(String myRegion) {
        this.myRegion = myRegion;
    }

    public String getWorkspaceName1() {
        return workspaceName1;
    }

    public void setWorkspaceName1(String workspaceName1) {
        this.workspaceName1 = workspaceName1;
    }

    public String getWorkspaceName2() {
        return workspaceName2;
    }

    public void setWorkspaceName2(String workspaceName2) {
        this.workspaceName2 = workspaceName2;
    }

    public String getWorkspaceName3() {
        return workspaceName3;
    }

    public void setWorkspaceName3(String workspaceName3) {
        this.workspaceName3 = workspaceName3;
    }

    public List<String> getGatewayAddr() {
        return gatewayAddr;
    }

    public void setGatewayAddr(List<String> gatewayAddr) {
        this.gatewayAddr = gatewayAddr;
    }

    public String getScmUser() {
        return scmUser;
    }

    public void setScmUser(String scmUser) {
        this.scmUser = scmUser;
    }

    public String getScmPassword() {
        return scmPassword;
    }

    public void setScmPassword(String scmPassword) {
        this.scmPassword = scmPassword;
    }

}
