package com.sequoiacm.infrastructure.config.core.msg;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class DefaultVersion implements Version {
    private String bussinessType;
    private String bussinessName;
    private int version;

    public DefaultVersion(BSONObject bsonObj) {
        this.bussinessType = BsonUtils.getStringChecked(bsonObj,
                ScmRestArgDefine.CONF_VERSION_BUSINESS_TYPE);
        this.bussinessName = BsonUtils.getStringChecked(bsonObj,
                ScmRestArgDefine.CONF_VERSION_BUSINESS_NAME);
        this.version = BsonUtils.getIntegerChecked(bsonObj,
                ScmRestArgDefine.CONF_VERSION_BUSINESS_VERSION);
    }

    public DefaultVersion(String bussinessType, String bussinessName, int version) {
        this.bussinessType = bussinessType;
        this.bussinessName = bussinessName;
        this.version = version;
    }

    public String getBussinessType() {
        return bussinessType;
    }

    public void setBussinessType(String bussinessType) {
        this.bussinessType = bussinessType;
    }

    @Override
    public String getBussinessName() {
        return bussinessName;
    }

    public void setBussinessName(String bussinessName) {
        this.bussinessName = bussinessName;
    }

    @Override
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.CONF_VERSION_BUSINESS_NAME, bussinessName);
        obj.put(ScmRestArgDefine.CONF_VERSION_BUSINESS_TYPE, bussinessType);
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
        DefaultVersion other = (DefaultVersion) obj;
        if (bussinessName == null) {
            if (other.bussinessName != null) {
                return false;
            }
        }
        else if (!bussinessName.equals(other.bussinessName)) {
            return false;
        }
        if (bussinessType == null) {
            if (other.bussinessType != null) {
                return false;
            }
        }
        else if (!bussinessType.equals(other.bussinessType)) {
            return false;
        }
        if (version != other.version) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "DefaultVersion [bussinessType=" + bussinessType + ", bussinessName=" + bussinessName
                + ", version=" + version + "]";
    }

}
