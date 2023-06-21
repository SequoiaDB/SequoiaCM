package com.sequoiacm.client.common;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;

public class ScmExceptionUtils {

    public static void handleException(BSONObject bsonObject) throws ScmException {
        if (bsonObject == null || bsonObject.isEmpty()) {
            return;
        }
        if (!(bsonObject instanceof BasicBSONObject)) {
            // ignore BasicBSONList
            return;
        }
        Number errCode = BsonUtils.getNumber(bsonObject, "status");
        String errMsg = BsonUtils.getString(bsonObject, "message");
        if (errCode != null && errMsg != null) {
            throw new ScmException(errCode.intValue(), errMsg);
        }
    }

    public static void handleException(String res) throws ScmException {
        if (res == null || res.trim().isEmpty()) {
            return;
        }
        BSONObject bsonObject = (BSONObject) JSON.parse(res);
        handleException(bsonObject);
    }
}
