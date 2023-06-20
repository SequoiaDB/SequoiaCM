package com.sequoiacm.infrastructure.feign;

import org.bson.BSONObject;
import org.bson.util.JSON;

public class ScmFeignExceptionUtils {

    public static void handleException(BSONObject bsonObject) throws ScmFeignException {
        if (bsonObject == null || bsonObject.isEmpty()) {
            return;
        }
        if (bsonObject.containsField("status") && bsonObject.containsField("message")) {
            throw new ScmFeignException(bsonObject);
        }
    }

    public static void handleException(String res) throws ScmFeignException {
        if (res == null || res.trim().isEmpty()) {
            return;
        }
        BSONObject bsonRes = null;
        try {
            bsonRes = (BSONObject) JSON.parse(res);
        }
        catch (Exception exception) {
            throw new IllegalArgumentException("Invalid response: " + res, exception);
        }
        if (bsonRes != null) {
            handleException(bsonRes);
        }
    }
}
