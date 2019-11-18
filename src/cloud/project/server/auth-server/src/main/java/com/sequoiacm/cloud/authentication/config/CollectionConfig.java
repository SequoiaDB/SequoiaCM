package com.sequoiacm.cloud.authentication.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class CollectionConfig {
    private final String collectionSpaceName = "SCMSYSTEM";
    private final String sessionCollectionName = "SESSIONS";
    private final String userCollectionName = "USERS";
    private final String roleCollectionName = "ROLES";

    public String getCollectionSpaceName() {
        return collectionSpaceName;
    }

    public String getSessionCollectionName() {
        return sessionCollectionName;
    }

    public String getUserCollectionName() {
        return userCollectionName;
    }

    public String getRoleCollectionName() {
        return roleCollectionName;
    }
}
