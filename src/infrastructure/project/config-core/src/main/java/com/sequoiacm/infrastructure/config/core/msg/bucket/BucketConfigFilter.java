package com.sequoiacm.infrastructure.config.core.msg.bucket;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

public class BucketConfigFilter implements ConfigFilter {
    private BucketConfigFilterType type;

    // type=EXACT_MATCH
    private String bucketName;

    // type=FUZZY_MATCH
    private BSONObject matcher;
    private BSONObject orderBy;
    private long limit = -1;
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

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject ret = new BasicBSONObject();
        ret.put(ScmRestArgDefine.BUCKET_CONF_FILTER_TYPE, type.name());
        ret.put(ScmRestArgDefine.BUCKET_CONF_FILTER_NAME, bucketName);
        ret.put(ScmRestArgDefine.BUCKET_CONF_FILTER_MATCHER, matcher);
        ret.put(ScmRestArgDefine.BUCKET_CONF_FILTER_ORDERBY, orderBy);
        ret.put(ScmRestArgDefine.BUCKET_CONF_FILTER_SKIP, skip);
        ret.put(ScmRestArgDefine.BUCKET_CONF_FILTER_LIMIT, limit);
        return ret;
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
}
