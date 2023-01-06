package com.sequoiacm.client.util;

import java.io.Closeable;
import java.util.Map;
import java.util.TreeMap;

import com.sequoiacm.client.dispatcher.MessageDispatcher;
import com.sequoiacm.client.dispatcher.RestDispatcher;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmHelper {
    private static final Logger logger = LoggerFactory.getLogger(ScmHelper.class);

    private static final String HEALTH_PATH = "/internal/v1/health";
    private static final String OLD_HEALTH_PATH = "/health";

    /**
     * A method to close opened stream resources.
     */
    public static void closeStream(Closeable stream) {
        try {
            if (null != stream) {
                stream.close();
            }
        }
        catch (Exception e) {
            // don't handle IOException
            logger.warn("close resource failed", e);
        }
    }

    public static void closeStreamNoLogging(Closeable stream) {
        try {
            if (null != stream) {
                stream.close();
            }
        }
        catch (Exception e) {
        }
    }

    public static boolean checkGatewayHealth(String url, MessageDispatcher restDispatcher) {
        boolean isHealth = true;
        String status = null;
        try {
            status = restDispatcher.getHealthStatus(url, HEALTH_PATH);
            if (!"UP".equals(status)) {
                isHealth = false;
            }
        }
        catch (ScmException e) {
            if (e.getError() == ScmError.HTTP_FORBIDDEN
                    || e.getError() == ScmError.HTTP_NOT_FOUND) {
                try {
                    status = restDispatcher.getHealthStatus(url, OLD_HEALTH_PATH);
                    if (!"UP".equals(status)) {
                        isHealth = false;
                    }
                }
                catch (ScmException scmException) {
                    isHealth = false;
                }

            }
            else {
                isHealth = false;
            }
        }
        return isHealth;
    }

    public static Map<String, String> parseCustomTag(BSONObject customTagObj) {
        Map<String, String> customTag = new TreeMap<String, String>();
        if (null != customTagObj && !customTagObj.isEmpty()) {
            BasicBSONObject obj = (BasicBSONObject) customTagObj;
            for (Map.Entry<String, Object> entry : obj.entrySet()) {
                customTag.put(entry.getKey(), (String) entry.getValue());
            }
        }
        return customTag;
    }
}
