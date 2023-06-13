package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import org.bson.BSONObject;

public class ScmConfProp {

    private String serviceName;
    private String instance;
    private String configKey;
    private String configValue;

    public ScmConfProp(BSONObject bsonObject) throws ScmException {
        this.serviceName = BsonUtils.getStringChecked(bsonObject, "service_name");
        this.instance = BsonUtils.getStringChecked(bsonObject, "instance");
        this.configKey = BsonUtils.getStringChecked(bsonObject, "config_key");
        this.configValue = BsonUtils.getString(bsonObject, "config_value");
    }

    /**
     * Gets the service name.
     *
     * @return service name.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Gets the instance url.
     * 
     * @return instance url.
     */
    public String getInstance() {
        return instance;
    }

    /**
     * Gets the config key.
     *
     * @return config key.
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * Gets the config value, return null if config key not exists.
     *
     * @return config value.
     */
    public String getConfigValue() {
        return configValue;
    }

    @Override
    public String toString() {
        return "ScmConfProp{" + "serviceName='" + serviceName + '\'' + ", instance='" + instance
                + '\'' + ", configKey='" + configKey + '\'' + ", configValue='" + configValue + '\''
                + '}';
    }
}
