package com.sequoiacm.contentserver.common;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.bson.types.BSONTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;

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
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        int index = path.lastIndexOf("/");
        if (index > 0) {
            return path.substring(0, index);
        }
        else if (index == 0) {
            return path;
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
        String[] eles = path.split("/");
        for (int i = eles.length - 1; i >= 0; i--) {
            if (eles[i].length() > 0) {
                return eles[i];
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
            try {
                is.close();
            }
            catch (Exception e) {
                logger.warn("close resource failed", e);
            }
        }
    }

    public static void main(String[] args) throws ScmServerException {
        System.out.println(ScmSystemUtils.getDate(0));
        ScmSystemUtils.logJVM();

        assert "/".equals(ScmSystemUtils.dirname("/"));
        assert "/".equals(ScmSystemUtils.dirname("//"));
        assert "/abc/a".equals(ScmSystemUtils.dirname("/abc/a/s////"));
        assert "/abc/a".equals(ScmSystemUtils.dirname("/abc/a/s"));

        boolean hasException = false;
        try {
            ScmSystemUtils.dirname("abc");
        }
        catch (Exception e) {
            hasException = true;
        }

        assert hasException;

        assert "/a".equals(ScmSystemUtils.generatePath("/", "a"));
        assert "/a".equals(ScmSystemUtils.generatePath("///", "a"));
        assert "///abc/a".equals(ScmSystemUtils.generatePath("///abc", "a"));
        assert "/abc/a".equals(ScmSystemUtils.generatePath("/abc/", "a"));
        assert "/abc/a".equals(ScmSystemUtils.generatePath("/abc//", "a"));

        assert "/abc/a".equals(ScmSystemUtils.generatePath("/abc//", "///a"));
        assert "/abc".equals(ScmSystemUtils.generatePath("/abc/", "///"));

        Object v = ScmSystemUtils.toLongValue(11111111111L);
        if (v instanceof Long) {
            System.out.println("long");
        }

    }
}
