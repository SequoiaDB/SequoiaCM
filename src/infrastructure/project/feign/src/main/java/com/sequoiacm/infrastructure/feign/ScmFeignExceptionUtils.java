package com.sequoiacm.infrastructure.feign;

import org.bson.BSONObject;

public class ScmFeignExceptionUtils {

    public static void handleException(BSONObject bsonObject) throws ScmFeignException {
        if (bsonObject == null || bsonObject.isEmpty()) {
            return;
        }
        if (bsonObject.containsField("status") && bsonObject.containsField("message")) {
            throw new ScmFeignException(bsonObject);
        }
    }
}
