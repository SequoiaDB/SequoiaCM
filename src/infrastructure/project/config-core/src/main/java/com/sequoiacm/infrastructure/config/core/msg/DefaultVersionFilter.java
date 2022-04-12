package com.sequoiacm.infrastructure.config.core.msg;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class DefaultVersionFilter implements VersionFilter {

    private String bussinessType;
    private List<String> bussinessNames;

    public DefaultVersionFilter(BSONObject bsonObj) {
        String bussinessType = BsonUtils.getString(bsonObj,
                ScmRestArgDefine.CONF_VERSION_BUSINESS_TYPE);
        BasicBSONList bussinessNames = BsonUtils.getArray(bsonObj,
                ScmRestArgDefine.CONF_VERSION_BUSINESS_NAME);
        setBussinessType(bussinessType);
        if (bussinessNames != null) {
            for (Object bussinessName : bussinessNames) {
                addBussinessName((String) bussinessName);
            }
        }

    }

    public DefaultVersionFilter(String bussinessType) {
        this.bussinessType = bussinessType;
    }

    public String getBussinessType() {
        return bussinessType;
    }

    public void setBussinessType(String bussinessType) {
        this.bussinessType = bussinessType;
    }

    public List<String> getBussinessNames() {
        return bussinessNames;
    }

    public void addBussinessName(String bussinessName) {
        if (bussinessNames == null) {
            bussinessNames = new ArrayList<>();
        }
        if (bussinessNames.contains(bussinessName)) {
            return;
        }
        bussinessNames.add(bussinessName);
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        if (bussinessType != null) {
            obj.put(ScmRestArgDefine.CONF_VERSION_BUSINESS_TYPE, bussinessType);
        }

        if (bussinessNames != null) {
            obj.put(ScmRestArgDefine.CONF_VERSION_BUSINESS_NAME, bussinessNames);
        }

        return obj;
    }

    @Override
    public String toString() {
        return "DefaultVersionFilter [bussinessType=" + bussinessType + ", bussinessName="
                + bussinessNames + "]";
    }

}
