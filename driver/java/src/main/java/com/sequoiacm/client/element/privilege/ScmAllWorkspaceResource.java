package com.sequoiacm.client.element.privilege;

public class ScmAllWorkspaceResource implements ScmResource {
    public static final String RESOURCE_TYPE = "workspace_all";

    /**
     * Create a workspace resource with specified workspace name.
     *
     * @param workspaceName
     *            workspace name.
     * @return
     */
    ScmAllWorkspaceResource() {
    }

    /**
     * Get the type of the resource
     *
     * @return type string.
     */
    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    /**
     * Format the resource to a string.
     *
     * @return string.
     */
    @Override
    public String toStringFormat() {
        return RESOURCE_TYPE;
    }
}
