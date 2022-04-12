package com.sequoiacm.client.element.privilege;

/**
 * Scm bucket resource.
 */
public class ScmBucketResource implements ScmResource {
    public static final String RESOURCE_TYPE = "bucket";
    private String bucket;
    private String workspace;

    ScmBucketResource(String bucket, String workspace) {
        this.bucket = bucket;
        this.workspace = workspace;
    }

    /**
     * Returns resource type.
     * 
     * @return type.
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
        return workspace + ":" + bucket;
    }
}
