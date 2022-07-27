package com.sequoiacm.contentserver.model;

import com.sequoiacm.datasource.dataoperation.ScmDataInfo;

public class ScmDataInfoDetail {

    private String md5;
    private String etag;
    private int siteId;
    private long size;
    private ScmDataInfo dataInfo;

    public ScmDataInfoDetail(ScmDataInfo dataInfo) {
        this.dataInfo = dataInfo;
    }

    public ScmDataInfo getDataInfo() {
        return dataInfo;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getEtag() {
        return etag;
    }

    @Override
    public String toString() {
        return "ScmDataInfoDetail{" + "md5='" + md5 + '\'' + ", siteId=" + siteId + ", size=" + size
                + ", dataInfo=" + dataInfo + '}';
    }
}
