package com.sequoiacm.testcommon;

public class SiteAccessInfo {
    int site_id;
    long last_access_time;

    public int getSite_id() {
        return site_id;
    }

    public void setSite_id(int site_id) {
        this.site_id = site_id;
    }

    public long getLast_access_time() {
        return last_access_time;
    }

    public void setLast_access_time(long last_access_time) {
        this.last_access_time = last_access_time;
    }
}
