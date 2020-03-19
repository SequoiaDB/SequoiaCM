package com.sequoiacm.s3.utils;

import java.io.Closeable;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.core.Range;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

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

    public static String concatPath(String p1, String p2) {
        if (p1.charAt(p1.length() - 1) == S3CommonDefine.SCM_DIR_SEP_CHAR
                && p2.charAt(0) != S3CommonDefine.SCM_DIR_SEP_CHAR) {
            return p1 + p2;
        }
        if (p1.charAt(p1.length() - 1) != S3CommonDefine.SCM_DIR_SEP_CHAR
                && p2.charAt(0) == S3CommonDefine.SCM_DIR_SEP_CHAR) {
            return p1 + p2;
        }
        if (p1.charAt(p1.length() - 1) == S3CommonDefine.SCM_DIR_SEP_CHAR
                && p2.charAt(0) == S3CommonDefine.SCM_DIR_SEP_CHAR) {
            p1 = p1.substring(0, p1.length() - 1);
            return p1 + p2;
        }

        return p1 + S3CommonDefine.SCM_DIR_SEP + p2;
    }

    public static String dirname(String path) throws S3ServerException {
        if (path.equals(S3CommonDefine.SCM_DIR_SEP)) {
            return S3CommonDefine.SCM_DIR_SEP;
        }

        int startIndex = path.length();
        for (int index = path.length() - 1; index >= 0; index--) {
            if (path.charAt(index) == S3CommonDefine.SCM_DIR_SEP_CHAR) {
                if (index != startIndex - 1) {
                    return path.substring(0, index + 1);
                }
                startIndex = index;
            }
        }

        throw new S3ServerException(S3Error.INVALID_ARGUMENT, "invalid path:" + path);
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

    public static boolean isValidPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return true;
        }
        if (prefix.startsWith(S3CommonDefine.SCM_DIR_SEP)) {
            return false;
        }
        String[] pathArr = prefix.split(S3CommonDefine.SCM_DIR_SEP);
        for (int i = 1; i < pathArr.length; i++) {
            if (pathArr[i].isEmpty()) {
                return false;
            }
        }

        return true;

    }

    public static void checkStartAfter(String startAfter) throws S3ServerException {
        if (startAfter == null || startAfter.isEmpty()) {
            return;
        }

        if (startAfter.startsWith(S3CommonDefine.SCM_DIR_SEP)) {
            throw new S3ServerException(S3Error.OBJECT_INVALID_START_AFTER,
                    "startAfter is not valid, can not starts with '/': " + startAfter);
        }

        String[] pathArr = startAfter.split(S3CommonDefine.SCM_DIR_SEP);
        for (int i = 1; i < pathArr.length; i++) {
            if (pathArr[i].isEmpty()) {
                throw new S3ServerException(S3Error.OBJECT_INVALID_KEY,
                        "startAfter is not valid, can not contains double slash: " + startAfter);
            }
        }
    }

    public static void checkKey(String key) throws S3ServerException {
        // a/v/d
        if (key == null) {
            throw new S3ServerException(S3Error.OBJECT_INVALID_KEY, "key is null");
        }

        if (key.isEmpty()) {
            throw new S3ServerException(S3Error.OBJECT_INVALID_KEY, "key is empty");
        }

        if (key.startsWith(S3CommonDefine.SCM_DIR_SEP)) {
            throw new S3ServerException(S3Error.OBJECT_INVALID_KEY,
                    "key is not valid path, can not starts with '/': " + key);
        }

        if (key.endsWith(S3CommonDefine.SCM_DIR_SEP)) {
            throw new S3ServerException(S3Error.OBJECT_INVALID_KEY,
                    "key is not valid path, can not ends with '/': " + key);
        }

        String[] pathArr = key.split(S3CommonDefine.SCM_DIR_SEP);
        for (int i = 0; i < pathArr.length; i++) {
            if (pathArr[i].isEmpty()) {
                throw new S3ServerException(S3Error.OBJECT_INVALID_KEY,
                        "key is not valid path, can not contains double slash: " + key);
            }
            if (!ScmArgChecker.Directory.checkDirectoryName(pathArr[i])) {
                throw new S3ServerException(S3Error.OBJECT_INVALID_KEY,
                        "key is not valid path, can not be '.' , and can not contains special symbol( '/' '\\' ':' '*' '?' '\"' '<' '>' '|'): "
                                + key);
            }
        }
    }
}
