package com.sequoiacm.s3.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.s3.core.Range;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

public class CommonUtil {
    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public static void consumeAndCloseResource(InputStream is, long len) {
        if (is == null) {
            return;
        }

        try {
            if (is.available() != 0) {
                is.skip(len);
            }
        }
        catch (Exception e) {
            logger.warn("skip resource fail", e);
        }
        finally {
            closeResource(is);
        }
    }

    public static void closeResource(Closeable resource) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        }
        catch (Exception e) {
            logger.warn("close resource fail", e);
        }
    }



    public static String basename(String path) throws S3ServerException {
        if (path.equals(CommonDefine.Directory.SCM_DIR_SEP)) {
            return CommonDefine.Directory.SCM_DIR_SEP;
        }

        int startIndex = path.length();
        for (int index = path.length() - 1; index >= 0; index--) {
            if (path.charAt(index) == CommonDefine.Directory.SCM_DIR_SEP_CHAR) {
                if (index != startIndex - 1) {
                    return path.substring(index + 1, startIndex);
                }
                startIndex = index;
            }
        }
        throw new S3ServerException(S3Error.INVALID_ARGUMENT, "invalid path:" + path);
    }

    public static void analyseRangeWithFileSize(Range range, long fileSize)
            throws S3ServerException {
        long contentLength = fileSize;
        if (range.getStart() >= contentLength) {
            throw new S3ServerException(S3Error.OBJECT_RANGE_NOT_SATISFIABLE,
                    "start > contentlength. start:" + range.getStart() + ", contentlength:"
                            + contentLength);
        }

        // final bytes
        if (range.getStart() == -1) {
            if (range.getEnd() == 0) {
                throw new S3ServerException(S3Error.OBJECT_RANGE_NOT_SATISFIABLE,
                        " range is invalid,range=-0 ");
            }
            if (range.getEnd() < contentLength) {
                range.setStart(contentLength - range.getEnd());
                range.setEnd(contentLength - 1);
            }
            else {
                range.setStart(0);
                range.setEnd(contentLength - 1);
            }
        }

        // from start to the final of Lob
        if (range.getEnd() == -1 || range.getEnd() >= contentLength) {
            range.setEnd(contentLength - 1);
        }

        // from 0 - final of Lob
        if (range.getStart() == 0 && range.getEnd() == contentLength - 1) {
            range.setContentLength(contentLength);
            return;
        }

        long readLength = range.getEnd() - range.getStart() + 1;
        range.setContentLength(readLength);
    }

    public static String readStream(HttpServletRequest httpServletRequest) throws IOException {
        int ONCE_READ_BYTES = 1024;
        ServletInputStream inputStream = httpServletRequest.getInputStream();
        StringBuilder stringBuilder = new StringBuilder();
        byte[] b = new byte[ONCE_READ_BYTES];
        int len = inputStream.read(b, 0, ONCE_READ_BYTES);
        while (len > 0) {
            stringBuilder.append(new String(b, 0, len));
            len = inputStream.read(b, 0, ONCE_READ_BYTES);
        }
        return stringBuilder.toString();
    }

    public static Map<String, String> parseObjectTagging(String taggingStr)
            throws S3ServerException {
        Map<String, String> tagMap = new HashMap<>();
        for (String s : taggingStr.split("&")) {
            String[] arr = s.split("=");
            if (tagMap.put(arr[0], arr[1]) != null) {
                throw new S3ServerException(S3Error.OBJECT_TAGGING_SAME_KEY,
                        "the object tag can not contain same key");
            }
        }
        return tagMap;
    }
}
