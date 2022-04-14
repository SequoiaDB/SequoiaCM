package com.sequoiacm.s3.core;

public class PreparedData {

    private DataInfo destDataInfo = null;
    private Part oldPart = null;
    private Boolean createNewData = false;

    public void setCreateNewData(Boolean createNewData) {
        this.createNewData = createNewData;
    }

    public Boolean getCreateNewData() {
        return createNewData;
    }

    public void setDestDataInfo(DataInfo destDataInfo) {
        this.destDataInfo = destDataInfo;
    }

    public DataInfo getDestDataInfo() {
        return destDataInfo;
    }

    public void setOldPart(Part oldPart) {
        this.oldPart = oldPart;
    }

    public Part getOldPart() {
        return oldPart;
    }
}
