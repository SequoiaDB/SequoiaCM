package com.sequoiacm.infrastructure.config.core.msg.quota;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class QuotaNotifyOption implements NotifyOption {
    private String type;
    private String name;
    private Integer version;
    private EventType eventType;

    private String businessName;

    public QuotaNotifyOption(String type, String name, Integer version, EventType eventType) {
        this.type = type;
        this.name = name;
        this.version = version;
        this.eventType = eventType;
        this.businessName = QuotaConfig.toBusinessName(type, name);
    }

    public QuotaNotifyOption(String businessName, Integer version, EventType eventType) {
        this.type = QuotaConfig.getTypeFromBusinessName(businessName);
        this.name = QuotaConfig.getNameFromBusinessName(businessName);
        this.version = version;
        this.eventType = eventType;
        this.businessName = businessName;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getBusinessName() {
        return businessName;
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }

    @Override
    public Version getVersion() {
        if (eventType == EventType.DELTE) {
            return new DefaultVersion(ScmConfigNameDefine.QUOTA, businessName, -1);
        }
        return new DefaultVersion(ScmConfigNameDefine.QUOTA, businessName, version);
    }

    @Override
    public BSONObject toBSONObject() {
        BSONObject bsonObject = new BasicBSONObject();
        bsonObject.put(ScmRestArgDefine.QUOTA_CONF_VERSION, version);
        bsonObject.put(FieldName.Quota.NAME, name);
        bsonObject.put(FieldName.Quota.TYPE, type);
        return bsonObject;
    }

    @Override
    public String toString() {
        return "QuotaNotifyOption{" + "type='" + type + '\'' + ", name='" + name + '\''
                + ", version=" + version + ", eventType=" + eventType + ", businessName='"
                + businessName + '\'' + '}';
    }
}
