package com.sequoiacm.client.element;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;

/**
 * The result of set configuration in service instance.
 */
public class ScmUpdateConfResult {
    private String serviceName;
    private String instance;
    private String errorMessage;

    public ScmUpdateConfResult(BSONObject info) throws ScmException {
        serviceName = BsonUtils.getStringChecked(info, "service");
        instance = BsonUtils.getStringChecked(info, "instance");
        errorMessage = BsonUtils.getString(info, "message");
    }

    /**
     * Gets service name.
     * 
     * @return Service name.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Gets instance url.
     * 
     * @return Instances url.
     */
    public String getInstance() {
        return instance;
    }

    /**
     * Gets error message.
     * 
     * @return Error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "ScmUpdateConfResult [serviceName=" + serviceName + ", instance=" + instance
                + ", errorMessage=" + errorMessage + "]";
    }
}
