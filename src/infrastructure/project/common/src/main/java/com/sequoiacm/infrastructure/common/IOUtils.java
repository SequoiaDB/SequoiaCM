package com.sequoiacm.infrastructure.common;

import java.io.Closeable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOUtils {
    private static final Logger logger = LoggerFactory.getLogger(IOUtils.class);

    public static void close(Closeable... rs) {
        if (rs == null || rs.length <= 0) {
            return;
        }
        for (Closeable r : rs) {
            if (r == null) {
                continue;
            }
            try {
                r.close();
            }
            catch (Exception e) {
                logger.warn("failed to close resource:" + r, e);
            }
        }
    }
}
