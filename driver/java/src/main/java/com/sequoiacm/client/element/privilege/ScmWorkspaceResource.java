package com.sequoiacm.client.element.privilege;

/**
 * Scm workspace resource.
 */
public class ScmWorkspaceResource implements ScmResource {
    public static final String RESOURCE_TYPE = "workspace";

    private String workspaceName;

    /**
     * Create a workspace resource with specified workspace name.
     *
     * @param workspaceName
     *            workspace name.
     */
    ScmWorkspaceResource(String workspaceName) {
        this.workspaceName = workspaceName;
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
        return workspaceName;
    }
}
