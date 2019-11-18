package com.sequoiacm.client.core;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

class ScmSessionInfoImpl implements ScmSessionInfo {
    private String sessionId;
    private String username;
    private long creationTime;
    private long lastAccessedTime;
    private int maxInactiveInterval;

    ScmSessionInfoImpl(BSONObject obj) throws ScmException {
        fromBSONObj(obj);
    }

    private void fromBSONObj(BSONObject obj) throws ScmException {
        sessionId = BsonUtils.getStringChecked(
                obj, FieldName.SessionInfo.FIELD_SESSION_ID);
        username = BsonUtils.getString(
                obj, FieldName.SessionInfo.FIELD_USERNAME);
        creationTime = BsonUtils.getNumber(
                obj, FieldName.SessionInfo.FIELD_CREATION_TIME).longValue();
        lastAccessedTime = BsonUtils.getNumber(
                obj, FieldName.SessionInfo.FIELD_LAST_ACCESSED_TIME).longValue();
        maxInactiveInterval = BsonUtils.getNumber(
                obj, FieldName.SessionInfo.FIELD_MAX_INACTIVE_INTERVAL).intValue();
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }
}
