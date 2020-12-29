package com.sequoiacm.infrastructure.fulltext.common;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class FileFulltextOperation {
    public static final String KEY_OPTION_TYPE = "option_type";
    public static final String KEY_WS_NAME = "ws_name";
    public static final String KEY_IDX_LOCATION = "index_location";
    public static final String KEY_FILE_ID = "file_id";
    public static final String KEY_SYNC_SAVE_INDEX = "sync_save_index";
    public static final String KEY_REINDEX = "reindex";

    private OperationType operationType;
    private String wsName;
    private String indexLocation;
    private String fileId;

    // operationType == CREATE_IDX 有效
    private boolean syncSaveIndex;
    private boolean reindex;

    public FileFulltextOperation() {
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
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
        return "FulltextOperation [operationType=" + operationType + ", wsName=" + wsName
                + ", indexLocation=" + indexLocation + ", fileId=" + fileId + ", ]";
    }

    public boolean isSyncSaveIndex() {
        return syncSaveIndex;
    }

    public boolean isReindex() {
        return reindex;
    }

    public void setSyncSaveIndex(boolean syncSaveIndex) {
        this.syncSaveIndex = syncSaveIndex;
    }

    public void setReindex(boolean reindex) {
        this.reindex = reindex;
    }

    public BSONObject toBSON() {
        BasicBSONObject ret = new BasicBSONObject();
        ret.put(FileFulltextOperation.KEY_FILE_ID, this.getFileId());
        ret.put(FileFulltextOperation.KEY_IDX_LOCATION, this.getIndexLocation());
        ret.put(FileFulltextOperation.KEY_OPTION_TYPE, this.getOperationType().name());
        ret.put(FileFulltextOperation.KEY_WS_NAME, this.getWsName());
        ret.put(FileFulltextOperation.KEY_SYNC_SAVE_INDEX, this.isSyncSaveIndex());
        ret.put(FileFulltextOperation.KEY_REINDEX, this.isReindex());
        return ret;
    }

    public static enum OperationType {
        // 为文件建立索引, 更新索引状态
        CREATE_IDX,
        // 删除索引并更新文件的索引状态
        DROP_IDX_AND_UPDATE_FILE,
        // 删除索引，不用更新文件索引状态（文件删除）
        DROP_IDX_ONLY
    }

}
