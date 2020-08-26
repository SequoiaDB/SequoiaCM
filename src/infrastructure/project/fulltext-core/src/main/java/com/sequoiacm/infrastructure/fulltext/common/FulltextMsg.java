package com.sequoiacm.infrastructure.fulltext.common;

public class FulltextMsg {
    public static final String KEY_OPTION_TYPE = "option_type";
    public static final String KEY_WS_NAME = "ws_name";
    public static final String KEY_IDX_LOCATION = "index_location";
    public static final String KEY_FILE_ID = "file_id";

    private OptionType optionType;
    private String wsName;
    private String indexLocation;
    private String fileId;

    public FulltextMsg() {
    }

    public OptionType getOptionType() {
        return optionType;
    }

    public void setOptionType(OptionType optionType) {
        this.optionType = optionType;
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public String getIndexLocation() {
        return indexLocation;
    }

    public void setIndexLocation(String indexLocation) {
        this.indexLocation = indexLocation;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    @Override
    public String toString() {
        return "FulltextMsg [optionType=" + optionType + ", wsName=" + wsName + ", indexLocation="
                + indexLocation + ", fileId=" + fileId + "]";
    }

    public static enum OptionType {
        // 为文件建立索引, 更新索引状态
        CREATE_IDX,
        // 删除索引并更新文件的索引状态
        DROP_IDX_AND_UPDATE_FILE,
        // 删除索引，不用更新文件索引状态（文件删除）
        DROP_IDX_ONLY
    }

}
