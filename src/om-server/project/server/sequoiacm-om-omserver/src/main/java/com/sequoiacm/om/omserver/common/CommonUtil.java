package com.sequoiacm.om.omserver.common;

import java.io.Closeable;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import com.sequoiacm.infrastructure.common.UriUtil;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonUtil {
    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);
    private static byte[] garbageBuffer = new byte[512 * 1024];
    private static final String CHARSET_UTF8 = "UTF-8";

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

    public static void consumeAndCloseResource(ScmOmInputStream is) {
        if (is == null || is.isClosed()) {
            return;
        }
        try {
            while (true) {
                int ret = is.read(garbageBuffer);
                if (ret <= -1) {
                    return;
                }
            }
        }
        catch (Exception e) {
            logger.warn("failed to consume resource", e);
        }
        finally {
            closeResource(is);
        }
    }

    public static String urlEncode(String s) throws ScmOmServerException {
        try {
            return UriUtil.encode(s);
        }
        catch (UnsupportedEncodingException e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "Encoding is not supported", e);
        }
        catch (IllegalArgumentException e) {
            throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                    "Invalid string: " + s, e);
        }
    }

    public static String urlDecode(String s) throws ScmOmServerException {
        try {
            return UriUtil.decode(s);
        }
        catch (UnsupportedEncodingException e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "Encoding is not supported", e);
        }
        catch (IllegalArgumentException e) {
            throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                    "Invalid string: " + s, e);
        }
    }
}
