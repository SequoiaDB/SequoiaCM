package com.sequoiacm.tools.element;

public class LocationMsg {
    private int siteID;
    private String domainName;

    public LocationMsg() {

    }

    public LocationMsg(int siteID, String domainName) {
        this.siteID = siteID;
        this.domainName = domainName;
    }

    public int getSiteID() {
        return siteID;
    }

    public void setSiteID(int siteID) {
        this.siteID = siteID;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

}