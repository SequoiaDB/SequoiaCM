package com.sequoiacm.om.omserver.module;

import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;

public class OmUserFilter {

    private String nameMatcher;

    private String hasRole;

    private Boolean enabled;

    public OmUserFilter(BSONObject userFilter) {
        this.nameMatcher = BsonUtils.getString(userFilter, "name_matcher");
        this.hasRole = BsonUtils.getString(userFilter, "has_role");
        this.enabled = BsonUtils.getBoolean(userFilter, "enabled");
    }

    public String getNameMatcher() {
        return nameMatcher;
    }

    public String getHasRole() {
        return hasRole;
    }

    public Boolean getEnabled() {
        return enabled;
    }
}
