package com.sequoiacm.infrastructure.config.core.msg.bucket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import org.bson.BSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.BUCKET)
public class BucketConfigFilter implements ConfigFilter {

    @JsonProperty(ScmRestArgDefine.BUCKET_CONF_FILTER_TYPE)
    private BucketConfigFilterType type;

    @JsonProperty(ScmRestArgDefine.BUCKET_CONF_FILTER_NAME)
    // type=EXACT_MATCH
    private String bucketName;

    @JsonProperty(ScmRestArgDefine.BUCKET_CONF_FILTER_MATCHER)
    // type=FUZZY_MATCH
    private BSONObject matcher;

    @JsonProperty(ScmRestArgDefine.BUCKET_CONF_FILTER_ORDERBY)
    private BSONObject orderBy;

    @JsonProperty(ScmRestArgDefine.BUCKET_CONF_FILTER_LIMIT)
    private long limit = -1;

    @JsonProperty(ScmRestArgDefine.BUCKET_CONF_FILTER_SKIP)
    private long skip = 0;

    public BucketConfigFilter(BSONObject matcher, BSONObject orderBy, long limit, long skip) {
        this.matcher = matcher;
        this.orderBy = orderBy;
        this.limit = limit;
        this.skip = skip;
        this.type = BucketConfigFilterType.FUZZY_MATCH;
    }

    public BucketConfigFilter(BSONObject matcher) {
        this.matcher = matcher;
        this.type = BucketConfigFilterType.FUZZY_MATCH;
    }

    public BucketConfigFilter(String bucketName) {
        this.bucketName = bucketName;
        this.type = BucketConfigFilterType.EXACT_MATCH;
    }

    public BucketConfigFilter() {
    }

    @Override
    public String toString() {
        return "BucketConfigFilter{" + "matcher=" + matcher + ", orderBy=" + orderBy + ", limit="
                + limit + ", skip=" + skip + '}';
    }

    public BucketConfigFilterType getType() {
        return type;
    }

    public String getBucketName() {
        return bucketName;
    }

    public BSONObject getMatcher() {
        return matcher;
    }

    public BSONObject getOrderBy() {
        return orderBy;
    }

    public long getLimit() {
        return limit;
    }

    public long getSkip() {
        return skip;
    }

    public void setType(BucketConfigFilterType type) {
        this.type = type;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public void setMatcher(BSONObject matcher) {
        this.matcher = matcher;
    }

    public void setOrderBy(BSONObject orderBy) {
        this.orderBy = orderBy;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public void setSkip(long skip) {
        this.skip = skip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BucketConfigFilter that = (BucketConfigFilter) o;
        return limit == that.limit && skip == that.skip && type == that.type && Objects.equals(bucketName, that.bucketName) && Objects.equals(matcher, that.matcher) && Objects.equals(orderBy, that.orderBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, bucketName, matcher, orderBy, limit, skip);
    }
}
