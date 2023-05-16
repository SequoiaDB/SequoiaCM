package com.sequoiacm.om.omserver.module.tag;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import org.bson.BSONObject;

public class OmTagFilter {

    private String tagMatcher;
    private String keyMatcher;
    private String valueMatcher;

    public OmTagFilter(BSONObject tagFilter) {
        this.tagMatcher = BsonUtils.getString(tagFilter, RestParamDefine.TAG_MATCHER);
        this.keyMatcher = BsonUtils.getString(tagFilter, RestParamDefine.KEY_MATCHER);
        this.valueMatcher = BsonUtils.getString(tagFilter, RestParamDefine.VALUE_MATCHER);
    }

    public String getTagMatcher() {
        return tagMatcher;
    }

    public String getKeyMatcher() {
        return keyMatcher;
    }

    public String getValueMatcher() {
        return valueMatcher;
    }
}
