package com.sequoiacm.mappingutil.common;

import com.google.gson.*;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.mappingutil.exception.ScmExitCode;
import org.bson.BSONObject;
import org.bson.util.JSON;

import java.io.File;
import java.lang.reflect.Type;

public class CommonUtils {

    private static Gson gson = new Gson().newBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(BSONObject.class, new JsonDeserializer<BSONObject>() {
                @Override
                public BSONObject deserialize(JsonElement jsonElement, Type type,
                        JsonDeserializationContext jsonDeserializationContext)
                        throws JsonParseException {
                    return (BSONObject) JSON.parse(jsonElement.toString());
                }
            }).create();

    public static <T> T parseJsonStr(String jsonStr, Class<T> clazz) {
        return gson.fromJson(jsonStr, clazz);
    }

    public static String toJsonString(Object o) {
        return gson.toJson(o);
    }

    public static void assertTrue(boolean f, String message) throws ScmToolsException {
        if (!f) {
            throw new ScmToolsException(message, ScmExitCode.INVALID_ARG);
        }
    }

    public static void checkFailCount(long currentFailCount, long maxFailCount)
            throws ScmToolsException {
        if (currentFailCount > maxFailCount) {
            throw new ScmToolsException(
                    "Process have been interrupted because of too many failed tasks: failCount="
                            + currentFailCount + ", maxFailCount=" + maxFailCount,
                    ScmExitCode.TOO_MANY_FAILURES);
        }
    }

    public static String getStandardDirPath(String filePath) {
        return filePath.endsWith(File.separator) ? filePath : filePath + File.separator;
    }
}
