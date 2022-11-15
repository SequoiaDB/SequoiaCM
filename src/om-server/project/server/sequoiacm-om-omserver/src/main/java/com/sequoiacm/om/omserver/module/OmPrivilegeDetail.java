package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmPrivilegeDetail extends OmPrivilegeBasic {

    @JsonProperty("resource_type")
    private String resourceType;

    @JsonProperty("resource_name")
    private String resourceName;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}
