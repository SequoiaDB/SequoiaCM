package com.sequoiadb.infrastructure.map;

import java.io.Closeable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiadb.infrastructure.map.server.service.MapServiceImpl;

public class CommonHelper {
    private static final Logger logger = LoggerFactory.getLogger(MapServiceImpl.class);

    public static void close(Closeable closeble) {
        try {
            if (null != closeble) {
                closeble.close();
            }
        }
        catch (Exception e) {
            // don't handle IOException
            logger.warn("close resource failed", e);
        }
    }

    public static void closeNoLogging(Closeable stream) {
        try {
            if (null != stream) {
                stream.close();
            }
        }
        catch (Exception e) {
        }
    }
}
