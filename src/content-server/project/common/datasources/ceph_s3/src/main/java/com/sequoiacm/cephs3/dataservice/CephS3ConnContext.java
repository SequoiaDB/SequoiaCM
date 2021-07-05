package com.sequoiacm.cephs3.dataservice;

public class CephS3ConnContext {
    private volatile CephS3Conn conn;
    private final CephS3UrlInfo urlInfo;
    private volatile boolean isDown;

    public CephS3ConnContext(CephS3UrlInfo urlInfo) {
        this.urlInfo = urlInfo;
    }

    public CephS3Conn getConn() {
        return conn;
    }

    public CephS3UrlInfo getUrlInfo() {
        return urlInfo;
    }

    public boolean isDown() {
        return isDown;
    }

    public void setConn(CephS3Conn conn) {
        this.conn = conn;
    }

    public void setDown(boolean down) {
        isDown = down;
    }

    @Override
    public String toString() {
        return "CephS3ConnContext{" + "url=" + urlInfo + ", isDown=" + isDown + '}';
    }
}
