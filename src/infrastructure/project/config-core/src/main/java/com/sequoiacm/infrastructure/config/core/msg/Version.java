package com.sequoiacm.infrastructure.config.core.msg;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class Version {
    private String businessType;
    private String businessName;
    private int version;

    public Version(BSONObject bsonObj) {
        this.businessType = BsonUtils.getStringChecked(bsonObj,
                ScmRestArgDefine.CONF_VERSION_BUSINESS_TYPE);
        this.businessName = BsonUtils.getStringChecked(bsonObj,
                ScmRestArgDefine.CONF_VERSION_BUSINESS_NAME);
        this.version = BsonUtils.getIntegerChecked(bsonObj,
                ScmRestArgDefine.CONF_VERSION_BUSINESS_VERSION);
    }

    public Version(String businessType, String businessName, Integer version) {
        this.businessType = businessType;
        this.businessName = businessName;
        this.version = version == null ? -1 : version;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.CONF_VERSION_BUSINESS_NAME, businessName);
        obj.put(ScmRestArgDefine.CONF_VERSION_BUSINESS_TYPE, businessType);
        obj.put(ScmRestArgDefine.CONF_VERSION_BUSINESS_VERSION, version);
        return obj;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Version other = (Version) obj;
        if (businessName == null) {
            if (other.businessName != null) {
                return false;
            }
        }
        else if (!businessName.equals(other.businessName)) {
            return false;
        }
        if (businessType == null) {
            if (other.businessType != null) {
                return false;
            }
        }
        else if (!businessType.equals(other.businessType)) {
            return false;
        }
        if (version != other.version) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "DefaultVersion [businessType=" + businessType + ", businessName=" + businessName
                + ", version=" + version + "]";
    }

}
