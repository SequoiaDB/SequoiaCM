package org.springframework.session.data.sequoiadb;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.session.ExpiringSession;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SequoiadbSession implements ExpiringSession, Serializable {

    /**
     * The default time period in seconds in which a session will expire (30 minutes).
     */
    public static final int DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS = 1800;

    /**
     * SequoiaDB doesn't support {@literal dot} in field names. We replace it with very rarely used character
     */
    private static final char DOT_COVER_CHAR = '\uF607';

    private final String id;
    private long creationTime = System.currentTimeMillis();
    private long lastAccessedTime;
    private int maxInactiveInterval;
    private Map<String, Object> attributes = new HashMap<String, Object>();

    @JsonIgnore
    private boolean isNew = true;

    public SequoiadbSession(String id, int maxInactiveIntervalInSeconds) {
        this.id = id;
        this.maxInactiveInterval = maxInactiveIntervalInSeconds;
        setLastAccessedTime(this.creationTime);
    }

    public SequoiadbSession(int maxInactiveIntervalInSeconds) {
        this(UUID.randomUUID().toString(), maxInactiveIntervalInSeconds);
    }

    public SequoiadbSession() {
        this(DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS);
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public void setCreationTime(long created) {
        this.creationTime = created;
    }

    @Override
    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public void setMaxInactiveIntervalInSeconds(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveIntervalInSeconds() {
        return this.maxInactiveInterval;
    }

    @Override
    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    private boolean isExpired(long now) {
        if (this.maxInactiveInterval < 0) {
            return false;
        }
        return now - TimeUnit.SECONDS.toMillis(this.maxInactiveInterval) >= this.lastAccessedTime;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getPrincipal() {
        return AbstractSequoiadbSessionConverter.extractPrincipal(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String attributeName) {
        return (T) this.attributes.get(coverDot(attributeName));
    }

    @Override
    public Set<String> getAttributeNames() {
        HashSet<String> result = new HashSet<String>();
        for (String key : this.attributes.keySet()) {
            result.add(uncoverDot(key));
        }
        return result;
    }

    @Override
    public void setAttribute(String attributeName, Object attributeValue) {
        if (attributeValue == null) {
            removeAttribute(coverDot(attributeName));
        }
        else {
            this.attributes.put(coverDot(attributeName), attributeValue);
        }
    }

    @Override
    public void removeAttribute(String attributeName) {
        this.attributes.remove(coverDot(attributeName));
    }

    static String coverDot(String attributeName) {
        return attributeName.replace('.', DOT_COVER_CHAR);
    }

    static String uncoverDot(String attributeName) {
        return attributeName.replace(DOT_COVER_CHAR, '.');
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SequoiadbSession that = (SequoiadbSession) o;

        return this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
