package com.sequoiacm.client.element.privilege;

/**
 * Scm directory resource.
 */
public class ScmDirectoryResource implements ScmResource {
    public static final String RESOURCE_TYPE = "directory";

    private String workspaceName;
    private String directory;

    /**
     * Create a instance with specified args.
     *
     * @param workspaceName
     *            workspace name.
     * @param directory
     *            path.
     */
    ScmDirectoryResource(String workspaceName, String directory) {
        this.workspaceName = workspaceName;
        this.directory = directory;
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
        return workspaceName + ":" + directory;
    }
}