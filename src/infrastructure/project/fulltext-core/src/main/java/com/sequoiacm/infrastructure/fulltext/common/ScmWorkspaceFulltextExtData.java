package com.sequoiacm.infrastructure.fulltext.common;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;

public class ScmWorkspaceFulltextExtData {
    public static final String FIELD_WS_EXT_DATA_ENABLED = "fulltext_enabled";
    public static final String FIELD_WS_EXT_DATA_STATUS = "fulltext_status";
    public static final String FIELD_WS_EXT_DATA_MATCHER = "fulltext_file_matcher";
    public static final String FIELD_WS_EXT_DATA_MODE = "fulltext_mode";
    public static final String FIELD_WS_EXT_DATA_LOCATION = "fulltext_index_data_location";
    public static final String FIELD_WS_EXT_DATA_SCHNAME = "fulltext_sch_name";

    private String wsName;
    private int wsId;

    private boolean enabled = false;
    private ScmFulltextStatus indexStatus = ScmFulltextStatus.NONE;
    private ScmFulltextMode mode = ScmFulltextMode.async;
    private BSONObject fileMatcher = null;
    private String fulltextJobName = null;
    private String indexDataLocation = null;

    public ScmWorkspaceFulltextExtData() {

    }

    public void setWsId(int wsId) {
        this.wsId = wsId;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public int getWsId() {
        return wsId;
    }

    public String getWsName() {
        return wsName;
    }

    public ScmWorkspaceFulltextExtData(String wsName, int wsId, BSONObject externalData) {
        this.wsName = wsName;
        this.wsId = wsId;
        if (externalData == null) {
            return;
        }
        enabled = BsonUtils.getBooleanOrElse(externalData, FIELD_WS_EXT_DATA_ENABLED, enabled);
        String indexStatusStr = BsonUtils.getStringOrElse(externalData, FIELD_WS_EXT_DATA_STATUS,
                indexStatus.name());
        indexStatus = ScmFulltextStatus.valueOf(indexStatusStr);
        String modeStr = BsonUtils.getStringOrElse(externalData, FIELD_WS_EXT_DATA_MODE,
                mode.name());
        mode = ScmFulltextMode.valueOf(modeStr);
        fileMatcher = BsonUtils.getBSON(externalData, FIELD_WS_EXT_DATA_MATCHER);
        indexDataLocation = BsonUtils.getString(externalData, FIELD_WS_EXT_DATA_LOCATION);
        fulltextJobName = BsonUtils.getString(externalData, FIELD_WS_EXT_DATA_SCHNAME);
    }

    public BSONObject getBson() {
        BasicBSONObject bson = new BasicBSONObject();
        bson.put(FIELD_WS_EXT_DATA_ENABLED, enabled);
        bson.put(FIELD_WS_EXT_DATA_MATCHER, fileMatcher);
        bson.put(FIELD_WS_EXT_DATA_MODE, mode.name());
        bson.put(FIELD_WS_EXT_DATA_STATUS, indexStatus.name());
        return bson;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ScmFulltextStatus getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(ScmFulltextStatus indexStatus) {
        this.indexStatus = indexStatus;
    }

    public ScmFulltextMode getMode() {
        return mode;
    }

    public void setMode(ScmFulltextMode mode) {
        this.mode = mode;
    }

    public BSONObject getFileMatcher() {
        return fileMatcher;
    }

    public void setFileMatcher(BSONObject fileMatcher) {
        this.fileMatcher = fileMatcher;
    }

    public String getFulltextJobName() {
        return fulltextJobName;
    }

    public String getIndexDataLocation() {
        return indexDataLocation;
    }

    public void setFulltextJobName(String fulltextJobName) {
        this.fulltextJobName = fulltextJobName;
    }

    public void setIndexDataLocation(String indexDataLocation) {
        this.indexDataLocation = indexDataLocation;
    }

    @Override
    public String toString() {
        return "WorkspaceFulltextExternalData [wsName=" + wsName + ", wsId=" + wsId + ", enabled="
                + enabled + ", indexStatus=" + indexStatus + ", mode=" + mode + ", fileMatcher="
                + fileMatcher + ", fulltextJobName=" + fulltextJobName + ", indexDataLocation="
                + indexDataLocation + "]";
    }

}
