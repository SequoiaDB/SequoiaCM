package com.sequoiacm.config.framework.config.workspace.entity;

import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public class SiteEntity {
    private int siteId;
    private String siteName;

    public SiteEntity(BSONObject siteObj) throws ScmConfigException {
        siteId = BsonUtils.getIntegerChecked(siteObj, FieldName.FIELD_CLSITE_ID);
        siteName = BsonUtils.getStringChecked(siteObj, FieldName.FIELD_CLSITE_NAME);
    }

    public int getSiteId() {
        return siteId;
    }

    public String getSiteName() {
        return siteName;
    }

}
