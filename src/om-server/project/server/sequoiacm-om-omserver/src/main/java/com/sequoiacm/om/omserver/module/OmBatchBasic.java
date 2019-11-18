package com.sequoiacm.om.omserver.module;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.core.ScmBatchInfo;

public class OmBatchBasic {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("create_time")
    private Date createTime;

    @JsonProperty("file_count")
    private int fileCount;

    public OmBatchBasic() {
    }

    public OmBatchBasic(ScmBatchInfo batch) {
        this.setCreateTime(batch.getCreateTime());
        this.setFileCount(batch.getFilesCount());
        this.setId(batch.getId().get());
        this.setName(batch.getName());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

}
