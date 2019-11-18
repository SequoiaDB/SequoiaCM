package com.sequoiacm.client.util;

import java.io.Closeable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmHelper {
    private static final Logger logger = LoggerFactory.getLogger(ScmHelper.class);

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
}
