package com.sequoiacm.client.element.quota;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.infrastructure.common.ScmQuotaUtils;

public class ScmEnableBucketQuotaConfig {
    private String bucketName;
    private long maxObjects = -1;
    private long maxSizeBytes = -1;

    private Long usedObjects;
    private Long usedSizeBytes;

    private ScmEnableBucketQuotaConfig(Builder builder) {
        this.bucketName = builder.bucketName;
        if (builder.maxSizeBytes != null) {
            this.maxSizeBytes = builder.maxSizeBytes;
        }
        if (builder.maxObjects != null) {
            this.maxObjects = builder.maxObjects;
        }
        this.usedSizeBytes = builder.usedSizeBytes;
        this.usedObjects = builder.usedObjects;
    }

    /**
     * Get the bucket name.
     * 
     * @return the bucket name.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Get the maximum object count limit for the bucket.
     * 
     * @return the maximum object count limit for the bucket.
     */
    public long getMaxObjects() {
        return maxObjects;
    }

    /**
     * Get the maximum size limit with bytes for the bucket.
     * 
     * @return the maximum size limit for the bucket.
     */
    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    /**
     * Get the used object count for the bucket.
     * 
     * @return the used object count for the bucket.
     */
    public Long getUsedObjects() {
        return usedObjects;
    }

    /**
     * Get the used size for the bucket.
     * 
     * @return the used size for the bucket.
     */
    public Long getUsedSizeBytes() {
        return usedSizeBytes;
    }

    /**
     * Create ScmBucketQuotaConfig with Builder.
     * 
     * @param bucketName
     *            the bucket name.
     * @return ScmBucketQuotaConfig.Builder
     */
    public static Builder newBuilder(String bucketName) {
        return new Builder(bucketName);
    }

    public static class Builder {
        private String bucketName;
        private Long maxObjects;
        private Long maxSizeBytes;

        private Long usedObjects;
        private Long usedSizeBytes;

        private Builder(String bucketName) {
            this.bucketName = bucketName;
        }

        /**
         * Set the maximum object count limit for the bucket. If the value is less than
         * 0, it means no limit.
         * 
         * @param maxObjects
         *            the maximum object count limit for the bucket.
         * @return ScmBucketQuotaConfig.Builder
         */
        public Builder setMaxObjects(long maxObjects) {
            this.maxObjects = maxObjects;
            return this;
        }

        /**
         * Set the maximum size limit for the bucket. If the value is less than 0 or
         * null, it means no limit.
         * 
         * @param maxSize
         *            the maximum size limit for the bucket. The unit is MB or GB.
         *            example: 100G、100g、1000M、1000m、-1(it means no limit).
         * @return ScmBucketQuotaConfig.Builder
         */
        public Builder setMaxSize(String maxSize) {
            this.maxSizeBytes = ScmQuotaUtils.parseMaxSize(maxSize);
            return this;
        }

        /**
         * Set the maximum size limit with bytes for the bucket. If the value is less
         * than 0, it means no limit.
         * 
         * @param maxSizeBytes
         *            the maximum size limit for the bucket. The unit is bytes.
         * @return ScmBucketQuotaConfig.Builder
         */
        public Builder setMaxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
            return this;
        }

        /**
         * Set the used size and used objects for the bucket. This is optional. If not
         * set, the used quota information will be automatically calculated.
         *
         * @param usedSize
         *            the used size for the bucket. The unit is MB or GB. example:
         *            100G、100g、1000M、0m.
         * @param usedObjects
         *            the used objects for the bucket.
         * @return ScmBucketQuotaConfig.Builder
         */
        public Builder setUsedQuota(long usedObjects, String usedSize) {
            this.usedObjects = usedObjects;
            this.usedSizeBytes = ScmQuotaUtils.convertToBytes(usedSize);
            return this;
        }

        /**
         * Set the used size and used objects for the bucket. This is optional. If not
         * set, the used quota information will be automatically calculated.
         *
         * @param usedSizeBytes
         *            the used size for the bucket. The unit is bytes.
         * @param usedObjects
         *            the used objects for the bucket.
         * @return ScmBucketQuotaConfig.Builder
         */
        public Builder setUsedQuota(long usedObjects, long usedSizeBytes) {
            this.usedObjects = usedObjects;
            this.usedSizeBytes = usedSizeBytes;
            return this;
        }

        /**
         * Build ScmBucketQuotaConfig.
         * 
         * @return ScmBucketQuotaConfig
         * @throws ScmInvalidArgumentException
         *             if bucketName is null or empty, or maxObjects and maxSize are
         *             both null.
         */
        public ScmEnableBucketQuotaConfig build() throws ScmInvalidArgumentException {
            if (bucketName == null || bucketName.isEmpty()) {
                throw new ScmInvalidArgumentException("bucketName is null or empty");
            }
            if (maxObjects == null && maxSizeBytes == null) {
                throw new ScmInvalidArgumentException(
                        "maxObjects and maxSize must specify at least one");
            }
            return new ScmEnableBucketQuotaConfig(this);
        }
    }

}
