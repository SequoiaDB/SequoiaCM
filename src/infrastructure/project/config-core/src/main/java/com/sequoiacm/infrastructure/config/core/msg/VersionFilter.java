package com.sequoiacm.infrastructure.config.core.msg;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class VersionFilter {

    private String businessType;
    private List<String> businessNames;

    public VersionFilter(BSONObject bsonObj) {
        String businessType = BsonUtils.getString(bsonObj,
                ScmRestArgDefine.CONF_VERSION_BUSINESS_TYPE);
        BasicBSONList businessNames = BsonUtils.getArray(bsonObj,
                ScmRestArgDefine.CONF_VERSION_BUSINESS_NAME);
        setBusinessType(businessType);
        if (businessNames != null) {
            for (Object businessName : businessNames) {
                addBusinessName((String) businessName);
            }
        }

    }

    public VersionFilter(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public List<String> getBusinessNames() {
        return businessNames;
    }

    public void addBusinessName(String businessName) {
        if (businessNames == null) {
            businessNames = new ArrayList<>();
        }
        if (businessNames.contains(businessName)) {
            return;
        }
        businessNames.add(businessName);
    }

    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        if (businessType != null) {
            obj.put(ScmRestArgDefine.CONF_VERSION_BUSINESS_TYPE, businessType);
        }

        if (businessNames != null) {
            obj.put(ScmRestArgDefine.CONF_VERSION_BUSINESS_NAME, businessNames);
        }

        return obj;
    }

    @Override
    public String toString() {
        return "DefaultVersionFilter [businessType=" + businessType + ", businessName="
                + businessNames + "]";
    }

}
