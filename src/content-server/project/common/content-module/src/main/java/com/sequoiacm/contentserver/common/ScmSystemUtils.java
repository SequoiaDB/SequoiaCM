package com.sequoiacm.contentserver.common;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.StringUtils;
import org.bson.BSONObject;
import org.bson.types.BSONTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.exception.ScmError;

public class ScmSystemUtils {
    private static final Logger logger = LoggerFactory.getLogger(ScmSystemUtils.class);
    private static byte[] garbageBuffer = new byte[512 * 1024];

    public static String getFullDateStr(Date date) {
        return CommonHelper.getFullDateStr(date);
    }

    public static Date getDate(long date) {
        return CommonHelper.getDate(date);
    }

    public static Date getDate(BSONTimestamp ts) {
        return CommonHelper.getDate(ts);
    }

    public static String getCurrentYearMonth(Date date) {
        return CommonHelper.getCurrentYearMonth(date);
    }

    public static String getNextYearMonth(Date date) {
        return CommonHelper.getNextYearMonth(date);
    }

    public static String getCurrentMonth(Date date) {
        return CommonHelper.getCurrentMonth(date);
    }

    public static String getCurrentYear(Date date) {
        return CommonHelper.getCurrentYear(date);
    }

    public static String getNextYear(Date date) {
        return CommonHelper.getNextYear(date);
    }

    public static String getHostName() {
        String hostName = "";
        try {
            InetAddress ia = InetAddress.getLocalHost();
            hostName = ia.getHostName();
        }
        catch (Exception e) {
            logger.warn("get local hostname failed", e);
        }

        return hostName;
    }

    public static String getHostName(String address) {
        String hostName = "";
        try {
            InetAddress ia = InetAddress.getByName(address);
            hostName = ia.getHostName();
        }
        catch (Exception e) {
            logger.warn("get hostname failed:address=" + address, e);
        }

        return hostName;
    }

    public static boolean isLocalHost(String address) {
        String localHostName = getHostName();
        String hostName = getHostName(address);
        if (hostName.equals(localHostName) && !hostName.equals("")) {
            return true;
        }

        return false;
    }

    public static String getHostPort(SocketAddress address) {
        String[] result = address.toString().split("/");
        if (result.length >= 2) {
            return result[1];
        }

        return address.toString();
    }

    public static String getVersionStr(int majorVersion, int minorVersion) {
        return majorVersion + "." + minorVersion;
    }

    /**
     * getDuration between end and begin
     *
     * @param begin
     *            begin date.
     * @param end
     *            end date.
     * @return the duration(seconds)
     */
    public static int getDuration(Date begin, Date end) {
        long l = begin.getTime();
        long e = end.getTime();
        if (e > l) {
            return (int) ((e - l) / 1000);
        }
        else {
            return (int) ((l - e) / 1000);
        }
    }

    public static int ipToInt(String ip) {
        int result = 0;
        String[] tmp = ip.split("\\.");
        for (int i = 0; i < tmp.length; i++) {
            int v = Integer.parseInt(tmp[i]);
            result |= v << (3 - i) * 8;
        }

        return result;
    }

    public static String intToIp(int v) {
        return (v >> 24 & 0xFF) + "." + (v >> 16 & 0xFF) + "." + (v >> 8 & 0xFF) + "." + (v & 0xFF);
    }

    public static boolean checkUrlExist(Set<String> urlSet, String url) {
        return urlSet.contains(url);
    }

    public static boolean equals(List<String> left, List<String> right) {
        return CommonHelper.equals(left, right);
    }

    public static String formatList(List<String> valueList) {
        StringBuilder sb = new StringBuilder();
        for (String value : valueList) {
            sb.append(value);
            sb.append(",");
        }

        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }

        return "";
    }

    private static String getSystemProperty(String name) {
        try {
            return System.getProperty(name);
        }
        catch (Exception e) {
            return "";
        }
    }

    private static List<String> getArguments() {
        try {
            return ManagementFactory.getRuntimeMXBean().getInputArguments();
        }
        catch (Exception e) {
            return null;
        }
    }

    public static void logJVM() {
        logger.info("os.name:" + getSystemProperty("os.name"));
        logger.info("os.arch:" + getSystemProperty("os.arch"));
        logger.info("os.version:" + getSystemProperty("os.version"));

        logger.info("java.version:" + getSystemProperty("java.version"));
        logger.info("java.home:" + getSystemProperty("java.home"));

        logger.info("java.arguments:" + getArguments());
    }

    public static String getPid() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            return name.split("@")[0];
        }
        catch (Exception e) {
            logger.warn("get pid failed", e);
            return "";
        }
    }

    public static String getQuarter(String month) {
        return CommonHelper.getQuarter(month);
    }

    public static String getMyDir(Class<?> className) throws ScmServerException {
        URL url = className.getProtectionDomain().getCodeSource().getLocation();
        String filePath = null;
        try {
            String jar = ".jar";
            filePath = URLDecoder.decode(url.getPath(), "utf-8");
            logger.debug("jarDir={}", filePath);
            int endIdx = filePath.indexOf(jar);
            endIdx += jar.length();
            int startIdx = filePath.indexOf(":") + 1;
            if (-1 == startIdx || startIdx >= endIdx) {
                startIdx = 0;
            }

            filePath = filePath.substring(startIdx, endIdx);
            logger.debug("jarDir={}", filePath);
        }
        catch (Exception e) {
            throw new ScmSystemException("get path failed:className=" + className.toString(), e);
        }

        return new File(filePath).getAbsoluteFile().getParent();
    }

    public static String dirname(String path) throws ScmInvalidArgumentException {
        if (path.equals(CommonDefine.Directory.SCM_DIR_SEP)) {
            return CommonDefine.Directory.SCM_DIR_SEP;
        }

        int startIndex = path.length();
        for (int index = path.length() - 1; index >= 0; index--) {
            if (path.charAt(index) == CommonDefine.Directory.SCM_DIR_SEP_CHAR) {
                if (index != startIndex - 1) {
                    return path.substring(0, index + 1);
                }
                startIndex = index;
            }
        }
        throw new ScmInvalidArgumentException("invlid path,path is not valid:path=" + path);
    }

    public static String generatePath(String dir, String name) {
        while (dir.length() > 1 && dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }

        while (name.startsWith("/")) {
            name = name.substring(1);
        }

        if (dir.endsWith("/") || name.isEmpty()) {
            return dir + name;
        }
        else {
            return dir + "/" + name;
        }
    }

    public static String basename(String path) throws ScmServerException {
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
        throw new ScmInvalidArgumentException("invlid path,path is root dir:path=" + path);
    }

    public static void closeResource(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        }
        catch (Exception e) {
            logger.warn("close resource failed", e);
        }
    }

    public static long toLongValue(Object value) {
        if (value instanceof Integer) {
            return (int) value;
        }
        else {
            return (long) value;
        }
    }

    public static void consumeAndCloseResource(InputStream is) {
        if (is == null) {
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

    public static String formatDirPath(String path) throws ScmInvalidArgumentException {
        int index = checkPath(path);
        if (index != path.length()) {
            return formatPath(path, index, true);
        }
        if (!path.endsWith(CommonDefine.Directory.SCM_DIR_SEP)) {
            return path + CommonDefine.Directory.SCM_DIR_SEP;
        }
        return path;
    }

    public static String formatFilePath(String path) throws ScmInvalidArgumentException {
        int index = checkPath(path);
        if (index != path.length()) {
            return formatPath(path, index, false);
        }
        if (path.endsWith(CommonDefine.Directory.SCM_DIR_SEP)) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    // return error index or path length
    private static int checkPath(String path) throws ScmInvalidArgumentException {
        if (!path.startsWith(CommonDefine.Directory.SCM_DIR_SEP)) {
            throw new ScmInvalidArgumentException("path must start with '/':" + path);
        }
        int index = 1;
        int startIndex = 0;
        while (index < path.length()) {
            char tmpChar = path.charAt(index);
            if (tmpChar == CommonDefine.Directory.SCM_DIR_SEP_CHAR) {
                if (index == startIndex + 1) {
                    return index;
                }
                startIndex = index;
            }
            index++;
        }
        return index;
    }

    private static String formatPath(String path, int index, boolean needSepSuffix) {
        StringBuilder newPath = new StringBuilder(path.substring(0, index));
        boolean sepSuffix = true;
        while (index < path.length()) {
            char tmpChar = path.charAt(index);
            if (tmpChar != CommonDefine.Directory.SCM_DIR_SEP_CHAR) {
                newPath.append(tmpChar);
                sepSuffix = false;
            }
            else {
                if (!sepSuffix) {
                    newPath.append(CommonDefine.Directory.SCM_DIR_SEP_CHAR);
                    sepSuffix = true;
                }
            }
            index++;
        }
        if (needSepSuffix && !sepSuffix) {
            newPath.append(CommonDefine.Directory.SCM_DIR_SEP_CHAR);
            return newPath.toString();
        }

        if (!needSepSuffix && sepSuffix) {
            return newPath.substring(0, newPath.length() - 1);
        }
        return newPath.toString();

    }



    public static MessageDigest createMd5Calc() throws ScmSystemException {
        try {
            return MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new ScmSystemException("failed to get md5 message digest instance", e);
        }
    }

    public static String calcMd5(ScmWorkspaceInfo wsInfo, ScmDataInfo dataInfo)
            throws ScmServerException {
        byte[] buf = new byte[1024 * 4];
        MessageDigest md5Calc = createMd5Calc();
        ScmDataReader reader = null;
        try {
            reader = ScmDataOpFactoryAssit.getFactory().createReader(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(dataInfo.getWsVersion()),
                    ScmContentModule.getInstance().getDataService(),
                    dataInfo);
            while (true) {
                int readLen = reader.read(buf, 0, buf.length);
                if (readLen <= -1) {
                    break;
                }
                md5Calc.update(buf, 0, readLen);
            }
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_READ_ERROR),
                    "failed to read data for calc md5:ws=" + wsInfo.getName() + ", dataInfo="
                            + dataInfo,
                    e);
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
        byte[] md5Bytes = md5Calc.digest();
        return DatatypeConverter.printBase64Binary(md5Bytes);
    }

    public static Date getDateFromCustomBatchId(String batchId, String idTimeRegex,
            String idTimePattern) throws ScmServerException {
        Pattern p = Pattern.compile(idTimeRegex);
        Matcher m = p.matcher(batchId);
        if (!m.find()) {
            throw new ScmServerException(ScmError.INVALID_ID,
                    "failed to parse batch id, can not find time in batch id:batchId=" + batchId
                            + ", timeRegex=" + idTimeRegex);
        }
        try {
            String timeStr = m.group();
            SimpleDateFormat sdfDate = new SimpleDateFormat(idTimePattern);
            return sdfDate.parse(timeStr);
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.INVALID_ID,
                    "failed to parse batch id, can not format time:batchId=" + batchId
                            + ", timePattern=" + idTimePattern);
        }
    }

    public static String getCreateMonthFromBatchId(ScmWorkspaceInfo ws, String batchId)
            throws ScmServerException {
        if (!ws.isBatchSharding()) {
            return null;
        }
        try {
            if (!ws.isBatchUseSystemId()) {
                Date date = getDateFromCustomBatchId(batchId, ws.getBatchIdTimeRegex(),
                        ws.getBatchIdTimePattern());
                return getCurrentYearMonth(date);
            }
            return new ScmIdParser(batchId).getMonth();
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.INVALID_ID) {
                throw new ScmServerException(ScmError.BATCH_NOT_FOUND,
                        "batch not found, cause by failed to parse batchId:ws=" + ws.getName()
                                + ", batchId=" + batchId,
                        e);
            }
            throw e;
        }
    }

    /**
     * 参考 S3 规则，统计/列取最新版本文件接口，其结果不包含 deleteMarker
     */
    public static boolean isDeleteMarkerRequired(Integer scope) {
        if (scope == CommonDefine.Scope.SCOPE_CURRENT) {
            return false;
        }
        return true;
    }

    public static boolean isEmptyBSONObject(BSONObject bsonObject) {
        return bsonObject == null || StringUtils.equals("{  }", bsonObject.toString());
    }
}
