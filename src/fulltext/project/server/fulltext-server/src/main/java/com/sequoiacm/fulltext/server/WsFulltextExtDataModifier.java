package com.sequoiacm.fulltext.server;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;

public class WsFulltextExtDataModifier {
    private BSONObject modifier;
    private BSONObject matcher;
    private String ws;

    public WsFulltextExtDataModifier(String ws) {
        modifier = new BasicBSONObject();
        matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLWORKSPACE_NAME, ws);
        this.ws = ws;
    }

    public WsFulltextExtDataModifier(String ws, String schName) {
        this(ws);
        matcher.put(FieldName.FIELD_CLWORKSPACE_EXT_DATA + "."
                + ScmWorkspaceFulltextExtData.FIELD_WS_EXT_DATA_SCHNAME, schName);
    }

    public String getWs() {
        return ws;
    }

    public BSONObject getMatcher() {
        return matcher;
    }

    public void setEnabled(boolean enabled) {
        modifier.put(ScmWorkspaceFulltextExtData.FIELD_WS_EXT_DATA_ENABLED, enabled);
    }

    public void setIndexStatus(ScmFulltextStatus indexStatus) {
        modifier.put(ScmWorkspaceFulltextExtData.FIELD_WS_EXT_DATA_STATUS, indexStatus.name());
    }

    public void setMode(ScmFulltextMode mode) {
        modifier.put(ScmWorkspaceFulltextExtData.FIELD_WS_EXT_DATA_MODE,
                mode == null ? null : mode.name());
    }

    public void setFileMatcher(BSONObject fileMatcher) {
        modifier.put(ScmWorkspaceFulltextExtData.FIELD_WS_EXT_DATA_MATCHER, fileMatcher);
    }

    public void setFulltextJobName(String fulltextJobName) {
        modifier.put(ScmWorkspaceFulltextExtData.FIELD_WS_EXT_DATA_SCHNAME, fulltextJobName);
    }

    public void setIndexDataLocation(String indexDataLocation) {
        modifier.put(ScmWorkspaceFulltextExtData.FIELD_WS_EXT_DATA_LOCATION, indexDataLocation);
    }

    public BSONObject getModifier() {
        return modifier;
    }

    @Override
    public String toString() {
        return "WsFulltextExtDataModifier [modifier=" + modifier + ", matcher=" + matcher + "]";
    }

}
