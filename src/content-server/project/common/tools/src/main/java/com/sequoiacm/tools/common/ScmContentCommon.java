package com.sequoiacm.tools.common;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import com.sequoiacm.client.element.bizconf.*;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.infrastructure.common.ScmManifestParser;
import com.sequoiacm.infrastructure.common.ScmManifestParser.ManifestInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.ScmCtl;
import com.sequoiacm.tools.exception.ScmExitCode;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class ScmContentCommon {
    public static final String LOG_FILE_CREATESITE = "logback_createsite.xml";
    public static final String LOG_FILE_CREATEWS = "logback_createws.xml";
    public static final String LOG_FILE_ADMIN = "logback_admin.xml";
    public static final String LOG_FILE_STARTNODE = "logback_start.xml";
    public static final String LOG_FILE_STOPNODE = "logback_stop.xml";
    public static final String LOG_FILE_GENERATE_META = "logback_generatemeta.xml";
    public static final String LOG_FILE_INSPECT = "logback_inspect.xml";

    public static final String CONTENTSERVER_NAME = "sequoiacm-content-server";
    public static final String APPLICATION_PROPERTIES = "application.properties";

    public static final String LOGCONF_NAME = "logback.xml";
    public static final String START_LOG_PATH = ".." + File.separator + "log" + File.separator
            + "start" + File.separator + "start.log";
    public static final String GENERATE_META_LOG_PATH = ".." + File.separator + "log" + "createmeta"
            + File.separator + File.separator + "createmeta.log";
    public static final String INSPECT_LOG_PATH = ".." + File.separator + "log" + File.separator
            + "inspect" + File.separator + "inspect.log";
    public static final String STOP_LOG_PATH = ".." + File.separator + "log" + File.separator
            + "stop" + File.separator + "stop.log";
    public static final String CREATE_SITE_LOG_PATH = ".." + File.separator + "log" + File.separator
            + "createsite" + File.separator + "createsite.log";
    public static final String CREATE_WS_LOG_PATH = ".." + File.separator + "log" + File.separator
            + "createws" + File.separator + "createws.log";
    public static final String SCM_ADMIN_LOG_PATH = ".." + File.separator + "log" + File.separator
            + "admin" + File.separator + "admin.log";
    public static final String ERROR_LOG_FILE_NAME = "error.out";
    public static final String DEFAULT_CONTENSERVER_HOST = "localhost";
    public static final String SCM_CONF_DIR_NAME = "content-server";
    public static final String SCM_LOG_DIR_NAME = "content-server";
    public static final String SCM_TOOL_LOG4J_PATH = File.separator + "main" + File.separator
            + "resources" + File.separator + "log4j.properties";
    public static final int DEDAULT_CONTENSERVER_PORT = 15000;

    public static final String SDBADMIN_USER_NAME = "sdbadmin";
    public static final String SCM_SAMPLE_SYS_CONF_NAME = "scm.application.properties";
    public static final String SCM_SAMPLE_LOG_CONF_NAME = "scm.logback.xml";

    public static final String DAEMON_DIR_PATH = "." + File.separator + ".." + File.separator + ".."
            + File.separator + "daemon";
    public static final String DAEMON_CONF_FILE_PATH = "." + File.separator + ".."
            + File.separator + "conf" + File.separator + ".scmd.properties";
    public static final String DAEMON_LOCATION = "daemonHomePath";
    public static final String BIN = "bin";
    public static final String DAEMON_SCRIPT = "scmd.sh";
    public static final String IGNORE_DAEMON_ENV = "IGNORE_DAEMON";

    private static final Logger logger = LoggerFactory.getLogger(ScmContentCommon.class);

    public static void printVersion() throws ScmToolsException {
        ManifestInfo info;
        try {
            info = ScmManifestParser.getManifestInfoFromJar(ScmContentCommon.class);
        }
        catch (IOException e) {
            throw new ScmToolsException("failed to load manifest info:", ScmExitCode.SYSTEM_ERROR,
                    e);
        }
        String revision = info.getGitCommitIdOrSvnRevision();
        if (revision == null) {
            revision = "missing revision infomation";
        }

        String version = info.getScmVersion();
        if (version == null) {
            version = "missing version infomation";
        }

        String compTime = info.getBuildTime();
        if (compTime == null) {
            compTime = "missing compile time infomation";
        }
        System.out.println("SequoiaCM tools version:" + version);
        System.out.println("Release:" + revision);
        System.out.println(compTime);
    }

    public static String getScmConfAbsolutePath() {
        File f = new File("..");
        return f.getAbsolutePath() + File.separator + "conf" + File.separator + SCM_CONF_DIR_NAME
                + File.separator;
    }

    public static String getScmLibAbsolutePath() {
        File f = new File("..");
        return f.getAbsolutePath() + File.separator + "lib" + File.separator;
    }

    public static String getContenserverAbsolutePath() {
        File f = new File("..");
        return f.getAbsolutePath() + File.separator;
    }

    public static String getClassPath() throws ScmToolsException {
        return "." + File.separator + getContentServerJarName() + ":." + File.separator + "lib"
                + File.separator + "*";
        // + ":." + File.separator + "lib" + File.separator + "hbase"
        // + File.separator + "*" + ":." + File.separator + "lib" +
        // File.separator + "aws_s3"
        // + File.separator + "*";
    }

    public static int convertStrToInt(String str) throws ScmToolsException {
        int port;
        try {
            port = Integer.valueOf(str);
            return port;
        }
        catch (Exception e) {
            logger.error("Can't convert " + str + " to integer", e);
            throw new ScmToolsException("Can't convert " + str + " to integer",
                    ScmExitCode.INVALID_ARG);
        }
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
            // ignore
            logger.warn("sleep occur execption,millis:" + millis, e);
        }
    }

    // public static boolean urlIsValid(String sdbUrl) {
    // if (sdbUrl == null) {
    // return false;
    // }
    // String[] urls = sdbUrl.split(",");
    // for (String url : urls) {
    // String[] hostAndPort = url.split(":");
    // if (hostAndPort.length != 2) {
    // return false;
    // }
    // try {
    // Integer.valueOf(hostAndPort[1]);
    // }
    // catch (Exception e) {
    // return false;
    // }
    // }
    // return true;
    // }

    public static void configToolsLog(String logFile) throws ScmToolsException {
        InputStream is = ScmCtl.class.getClassLoader().getResourceAsStream(logFile);
        try {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            JoranConfigurator configure = new JoranConfigurator();
            configure.setContext(lc);
            lc.reset();
            configure.doConfigure(is);
        }
        catch (JoranException e) {
            e.printStackTrace();
            logger.error("config logback failed", e);
        }
        finally {
            try {
                is.close();
            }
            catch (Exception e) {
                logger.warn("close inpustream occur error", e);
            }
        }
    }

    public static boolean isWindows() {
        String os = System.getProperties().getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return true;
        }
        else {
            return false;
        }
    }

    public static boolean isLinux() {
        String os = System.getProperties().getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            return true;
        }
        else {
            return false;
        }
    }

    public static void printSpace(int count) {
        for (int i = 0; i < count; i++) {
            System.out.print(" ");
        }
    }

    public static String getScmOwner() {
        String userName = null;
        try {
            userName = getContentServerOwner().getName();
        }
        catch (ScmToolsException e) {
            // ignore it
        }
        return userName;
    }

    private static UserPrincipal getContentServerOwner() throws ScmToolsException {
        UserPrincipal owner;
        try {
            owner = Files.getOwner(Paths.get(getContentServerJarName()), LinkOption.NOFOLLOW_LINKS);
        }
        catch (IOException e) {
            logger.error("Failed to get owner of " + ScmContentCommon.getContenserverAbsolutePath()
                    + getContentServerJarName(), e);
            throw new ScmToolsException(
                    "Failed to get owner of " + ScmContentCommon.getContenserverAbsolutePath()
                            + getContentServerJarName() + ",errorMsg:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR);
        }
        return owner;
    }

    public static boolean isFileCanWirte(String filePath) {
        File f = new File(filePath);
        if (!f.exists()) {
            return true;
        }
        if (f.canWrite()) {
            return true;
        }
        else {
            return false;
        }
    }

    public static void setFileOwnerAndGroup(String filePath) throws ScmToolsException {
        UserPrincipal owner = null;
        owner = getContentServerOwner();
        Path path = Paths.get(filePath);
        try {
            Files.setOwner(path, owner);
        }
        catch (IOException e) {
            logger.error("Failed to set file's owner to " + owner.getName() + ",file:" + filePath,
                    e);
            throw new ScmToolsException(
                    "Failed to set file's owner to " + owner.getName() + ",file:" + filePath,
                    ScmExitCode.SYSTEM_ERROR);
        }

        GroupPrincipal contentServerGroup = null;
        try {
            contentServerGroup = (GroupPrincipal) Files.getAttribute(
                    Paths.get(ScmContentCommon.getContenserverAbsolutePath()), "posix:group",
                    LinkOption.NOFOLLOW_LINKS);
        }
        catch (IOException e) {
            logger.error("Failed to get group name of "
                    + ScmContentCommon.getContenserverAbsolutePath() + getContentServerJarName(),
                    e);
            throw new ScmToolsException(
                    "Failed to get group name of " + ScmContentCommon.getContenserverAbsolutePath()
                            + getContentServerJarName() + ",errorMsg" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR);
        }
        try {
            Files.getFileAttributeView(path, PosixFileAttributeView.class)
                    .setGroup(contentServerGroup);
        }
        catch (IOException e) {
            logger.error("Failed to set file's group to " + contentServerGroup.getName() + ",file:"
                    + filePath, e);
            throw new ScmToolsException("Failed to set file's group to "
                    + contentServerGroup.getName() + ",file:" + filePath, ScmExitCode.SYSTEM_ERROR);
        }

    }

    public static void createDir(String dirPath) throws ScmToolsException {
        File file = new File(dirPath);
        try {
            if (!file.exists() && file.mkdirs() != true) {
                logger.error("Faile to create dir:" + file.toString());
                throw new ScmToolsException("Faile to create dir:" + file.toString(),
                        ScmExitCode.SYSTEM_ERROR);
            }
        }
        catch (SecurityException e) {
            logger.error("Failed to create dir:" + file.toString(), e);
            throw new ScmToolsException("Failed to create dir:" + file.toString()
                    + ",permisson error:" + e.getMessage(), ScmExitCode.PERMISSION_ERROR);
        }
    }

    public static void createFile(String filePath) throws ScmToolsException {
        File file = new File(filePath);
        if (!ScmContentCommon.isFileExists(file.getParent())) {
            try {
                Files.createDirectories(Paths.get(file.getParent()));
            }
            catch (SecurityException e) {
                logger.error("Failed to create dir:" + file.getParent(), e);
                throw new ScmToolsException("Failed to create dir:" + file.getParent()
                        + ",permisson error:" + e.getMessage(), ScmExitCode.PERMISSION_ERROR);
            }
            catch (IOException e) {
                logger.error("Failed to create dir:" + file.getParent(), e);
                throw new ScmToolsException(
                        "Failed to create dir:" + file.getParent() + ",io error:" + e.getMessage(),
                        ScmExitCode.SYSTEM_ERROR);
            }
            catch (Exception e) {
                logger.error("Failed to create dir:" + file.getParentFile().toString(), e);
                throw new ScmToolsException("Failed to create dir:"
                        + file.getParentFile().toString() + ",error:" + e.getMessage(),
                        ScmExitCode.SYSTEM_ERROR);
            }
            // setFileOwnerAndGroup(file.getParent());
        }
        try {
            if (!file.createNewFile()) {
                logger.error("Faile to create file:" + file.getAbsolutePath());
                throw new ScmToolsException("Faile to create file:" + file.getAbsolutePath(),
                        ScmExitCode.SYSTEM_ERROR);
            }
            // setFileOwnerAndGroup(file.getPath());
        }
        catch (IOException e) {
            logger.error("Faile to create file:" + filePath, e);
            throw new ScmToolsException("Failed to create file,io exception:" + filePath
                    + ",errorMsg:" + e.getMessage(), ScmExitCode.SYSTEM_ERROR);
        }
        catch (SecurityException e) {
            logger.error("Faile to create file:" + filePath, e);
            throw new ScmToolsException("Failed to create file,permission error:" + filePath
                    + ",errorMsg:" + e.getMessage(), ScmExitCode.SYSTEM_ERROR);
        }
    }

    public static void setLogAndProperties(String logPath, String propFileName)
            throws ScmToolsException {
        if (!ScmContentCommon.isFileExists(logPath)) {
            ScmContentCommon.createFile(logPath);
            // try {
            // setFileOwnerAndGroup(logPath);
            // }
            // catch (ScmToolsException e) {
            // System.out.println("WARN:" + e.getMessage());
            // return;
            // }
            ScmContentCommon.configToolsLog(propFileName);
        }
        else if (ScmContentCommon.isFileCanWirte(logPath)) {
            ScmContentCommon.configToolsLog(propFileName);
        }
        else {
            System.out.println("WARN:could not write log to " + logPath + ",permission error");
        }
    }

    public static boolean isFileExists(String filePath) throws ScmToolsException {
        if (filePath == null) {
            return false;
        }
        File f = new File(filePath);
        try {
            if (f.exists()) {
                return true;
            }
            else {
                return false;
            }
        }
        catch (SecurityException e) {
            logger.error("Could not determine the existence of file:" + filePath, e);
            throw new ScmToolsException("Could not determine the existence of file:" + filePath,
                    ScmExitCode.PERMISSION_ERROR);
        }
    }

    public static boolean isLocalHost(String address) throws ScmToolsException {
        String localHost = null;
        try {
            InetAddress ia = InetAddress.getLocalHost();
            localHost = ia.getHostName();
        }
        catch (Exception e) {
            logger.error("get local hostname failed", e);
            throw new ScmToolsException("get local hostname failed", ScmExitCode.SYSTEM_ERROR);
        }

        String argHost = null;
        try {
            InetAddress ia = InetAddress.getByName(address);
            argHost = ia.getHostName();
        }
        catch (Exception e) {
            logger.error("get hostname failed,address:" + address, e);
            throw new ScmToolsException("get hostname failed,address:" + address,
                    ScmExitCode.SYSTEM_ERROR);
        }

        if (localHost.equals(argHost)) {
            return true;
        }
        return false;
    }

    public static class DateUtil {

        public static final String MONTH1 = "01";
        public static final String MONTH3 = "03";
        public static final String MONTH4 = "04";
        public static final String MONTH6 = "06";
        public static final String MONTH7 = "07";
        public static final String MONTH9 = "09";
        public static final String MONTH10 = "10";

        public static final String QUARTER1 = "Q1";
        public static final String QUARTER2 = "Q2";
        public static final String QUARTER3 = "Q3";
        public static final String QUARTER4 = "Q4";
        private static SimpleDateFormat ymDateFormat = new SimpleDateFormat("yyyyMM");
        private static SimpleDateFormat yearDateFormat = new SimpleDateFormat("yyyy");
        private static SimpleDateFormat monthDateFormat = new SimpleDateFormat("MM");

        public static String getCurrentYearMonth(Date date) {
            return ymDateFormat.format(date);
        }

        public static String getNextYearMonth(Date date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MONTH, 1);
            date = calendar.getTime();
            return ymDateFormat.format(date);
        }

        public static String getCurrentMonth(Date date) {
            return monthDateFormat.format(date);
        }

        public static String getCurrentYear(Date date) {
            return yearDateFormat.format(date);
        }

        public static String getNextYear(Date date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.YEAR, 1);
            date = calendar.getTime();
            return yearDateFormat.format(date);
        }

        public static String getQuarter(String month) {
            StringBuilder sb = new StringBuilder();
            if (month.compareTo(MONTH6) <= 0) {
                if (month.compareTo(MONTH3) <= 0) {
                    sb.append(QUARTER1);
                }
                else {
                    sb.append(QUARTER2);
                }
            }
            else {
                if (month.compareTo(MONTH9) <= 0) {
                    sb.append(QUARTER3);
                }
                else {
                    sb.append(QUARTER4);
                }
            }

            return sb.toString();
        }
    }

    public static String getContentServerJarName() throws ScmToolsException {
        File libDir = new File(".");
        final String matchCondition = "^" + CONTENTSERVER_NAME + "-(.*)\\.jar$";
        FilenameFilter jarNameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return Pattern.matches(matchCondition, name);
            }
        };
        File[] files = libDir.listFiles(jarNameFilter);
        if (files == null || files.length <= 0) {
            logger.error("Missing jar, jarPrefix:{}, path:{}", matchCondition, libDir.getAbsolutePath());
            throw new ScmToolsException(
                    "Missing jar, jarPrefix:" + matchCondition + ",path:" + libDir.getAbsolutePath(),
                    ScmExitCode.FILE_NOT_FIND);
        }
        if (files.length == 1) {
            return files[0].getName();
        }
        Comparator<File> versionComparator = new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o2.getName().compareTo(o1.getName());
            }
        };
        Arrays.sort(files, versionComparator);
        logger.info("Multiple jar in the path, jar:{}, path:{}", Arrays.toString(files), libDir.getAbsolutePath());
        return files[0].getName();
    }

    public static ScmDataLocation createDataLocation(BSONObject dataLocationBSON,
            Map<String, com.sequoiacm.client.element.ScmSiteInfo> siteMap)
            throws ScmInvalidArgumentException, ScmToolsException {
        String siteName = (String) dataLocationBSON
                .get(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
        com.sequoiacm.client.element.ScmSiteInfo site = siteMap.get(siteName);
        if (site == null) {
            throw new ScmToolsException("unknown location:" + dataLocationBSON,
                    ScmExitCode.INVALID_ARG);
        }
        DatasourceType dataType = site.getDataType();
        switch (dataType) {
            case SEQUOIADB:
                return new ScmSdbDataLocation(dataLocationBSON);
            case HDFS:
                return new ScmHdfsDataLocation(dataLocationBSON);
            case HBASE:
                return new ScmHbaseDataLocation(dataLocationBSON);
            case CEPH_S3:
                return new ScmCephS3DataLocation(dataLocationBSON);
            case CEPH_SWIFT:
                return new ScmCephSwiftDataLocation(dataLocationBSON);
            case SFTP:
                return new ScmSftpDataLocation(dataLocationBSON);
            default:
                throw new ScmToolsException(
                        "unknown siteType:siteName=" + siteName + ",type=" + dataType,
                        ScmExitCode.INVALID_ARG);
        }
    }

    public static Map<String, com.sequoiacm.client.element.ScmSiteInfo> querySite(ScmSession ss)
            throws ScmException {
        ScmCursor<com.sequoiacm.client.element.ScmSiteInfo> c = null;
        try {
            c = ScmFactory.Site.listSite(ss);
            Map<String, com.sequoiacm.client.element.ScmSiteInfo> siteMap = new HashMap<>();
            while (c.hasNext()) {
                com.sequoiacm.client.element.ScmSiteInfo site = c.getNext();
                siteMap.put(site.getName(), site);
            }
            return siteMap;
        }
        finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static void closeResource(Closeable resource) throws ScmToolsException {
        if (resource != null) {
            try {
                resource.close();
            }
            catch (IOException e) {
                throw new ScmToolsException("close resouce failed", ScmExitCode.SYSTEM_ERROR, e);
            }
        }
    }
}
