package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmDirectoryInfoWithSubDir extends OmDirectoryBasic {

    @JsonProperty("sub_dir_count")
    private Long subDirCount;

    public Long getSubDirCount() {
        return subDirCount;
    }

    public void setSubDirCount(Long subDirCount) {
        this.subDirCount = subDirCount;
    }
}
