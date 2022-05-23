package com.sequoiacm.mappingutil.element;

import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import org.bson.BSONObject;

import java.util.Objects;

public class WorkConf {

    private String workspace;
    private String bucket;
    private ScmBucketAttachKeyType keyType;
    private String fileIdPath;
    private BSONObject fileMatcher;

    public WorkConf(MappingOption mappingOption) {
        this.workspace = mappingOption.getWorkspace();
        this.bucket = mappingOption.getBucket();
        this.keyType = mappingOption.getKeyType();
        this.fileIdPath = mappingOption.getIdFilePath();
        this.fileMatcher = mappingOption.getFileMatcher();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WorkConf workConf = (WorkConf) o;
        return Objects.equals(workspace, workConf.workspace)
                && Objects.equals(bucket, workConf.bucket)
                && Objects.equals(keyType, workConf.keyType)
                && Objects.equals(fileIdPath, workConf.fileIdPath)
                && Objects.equals(fileMatcher, workConf.fileMatcher);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspace, bucket, keyType, fileIdPath, fileMatcher);
    }

    @Override
    public String toString() {
        return "WorkConf{" + "workspace='" + workspace + '\'' + ", bucket='" + bucket + '\''
                + ", keyType=" + keyType + ", fileIdPath='" + fileIdPath + '\'' + ", fileMatcher="
                + fileMatcher + '}';
    }
}
