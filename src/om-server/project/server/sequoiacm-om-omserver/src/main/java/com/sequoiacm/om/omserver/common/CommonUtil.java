package com.sequoiacm.om.omserver.common;

import java.io.Closeable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonUtil {
    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public static void closeResource(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            }
            catch (Exception e) {
                logger.warn("failed to close resource:{}", resource.toString(), e);
            }
        }

    }
}
