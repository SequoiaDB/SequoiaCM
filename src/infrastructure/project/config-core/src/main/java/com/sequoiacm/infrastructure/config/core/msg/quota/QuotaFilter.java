package com.sequoiacm.infrastructure.config.core.msg.quota;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class QuotaFilter implements ConfigFilter {

    private String type;
    private String name;

    public QuotaFilter(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public QuotaFilter() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public BSONObject toBSONObject() {
        BSONObject bsonObject = new BasicBSONObject();
        if (type != null) {
            bsonObject.put(FieldName.Quota.TYPE, type);
        }
        if (name != null) {
            bsonObject.put(FieldName.Quota.NAME, name);
        }
        return bsonObject;
    }
}
