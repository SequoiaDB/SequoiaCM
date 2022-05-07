package com.sequoiacm.s3import.module;

import java.util.Objects;

public class WorkEnv {

    private String srcS3Url;
    private String destS3Url;

    public WorkEnv(String srcS3Url, String destS3Url) {
        this.srcS3Url = srcS3Url;
        this.destS3Url = destS3Url;
    }

    @Override
    public String toString() {
        return "srcS3Url=" + srcS3Url + ", destS3Url=" + destS3Url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WorkEnv workEnv = (WorkEnv) o;
        return Objects.equals(srcS3Url, workEnv.srcS3Url)
                && Objects.equals(destS3Url, workEnv.destS3Url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcS3Url, destS3Url);
    }
}
