package com.sequoiacm.om.omserver.module;

public class OmResourceInfo {
    private String type;
    private String resource;
    private String privilege;

    public OmResourceInfo() {
    }

    public OmResourceInfo(String type, String resource, String privilege) {
        this.type = type;
        this.resource = resource;
        this.privilege = privilege;
    }

    public String getResource() {
        return resource;
    }

    public String getType() {
        return type;
    }

    public String getPrivilege() {
        return privilege;
    }

}
