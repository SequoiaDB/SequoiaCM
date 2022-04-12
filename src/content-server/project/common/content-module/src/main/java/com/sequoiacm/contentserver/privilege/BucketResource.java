package com.sequoiacm.contentserver.privilege;

import com.sequoiacm.infrastructrue.security.privilege.IResource;

public class BucketResource implements IResource {
    static final String TYPE = "bucket";
    private String wsName;
    private String bucketName;

    public BucketResource(String wsName, String bucketName) {
        this.wsName = wsName;
        this.bucketName = bucketName;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getBucketName() {
        return bucketName;
    }

    @Override
    public String getWorkspace() {
        return wsName;
    }

    @Override
    public String toStringFormat() {
        return wsName + ":" + bucketName;
    }

    @Override
    public String toString() {
        return "BucketResource{" + "wsName='" + wsName + '\'' + ", bucketName='" + bucketName + '\''
                + '}';
    }
}
