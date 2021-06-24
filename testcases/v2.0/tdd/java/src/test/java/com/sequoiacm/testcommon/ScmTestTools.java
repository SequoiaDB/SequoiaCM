package com.sequoiacm.testcommon;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sequoiacm.client.util.ScmHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;
import org.bson.util.JSON;
import org.testng.Assert;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

public class ScmTestTools {
    private final static Logger logger = Logger.getLogger(ScmTestTools.class);

    public static final String WORKSPACE_CS_META_SUFFIX = "_META";
    public static final String WORKSPACE_CS_LOB_SUFFIX = "_LOB";

    public static final String CL_FILE = "FILE";
    public static final String CL_LOB = "LOB";

    private static final int DEFAULT_VALUE_LEN = 1024 * 1024 * 10;

    private static final String SAMPLE = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    /*
     * private static List<String> clList = new ArrayList<String>(); static {
     * clList.add(CLSCMBATCHVER); clList.add(CLSCMBATCH); clList.add(CLSCMITEM);
     * clList.add(CLSCMITEMHIS); clList.add(CLSCMITEMALL);
     * clList.add(CLSCMITEMFILE); clList.add(CLSCMCHECKEDOUT); }
     */

    /**
     * create scm file, set content.
     *
     * @param ws
     * @param filePath
     *            , upload file
     * @return fileId
     */
    public static ScmId createScmFile(ScmWorkspace ws, String filePath) {
        ScmId fileId = null;
        try {
            ScmFile file = createScmFile(ws, filePath, "scmFile", "test", "sequoiacm");
            fileId = file.getFileId();
        }
        catch (ScmException e) {
            Assert.fail(e.getMessage());
        }
        return fileId;
    }

    /**
     * create scm file, don't set content.
     *
     * @param ws
     * @return fileId
     */
    public static ScmId createScmFile(ScmWorkspace ws) {
        ScmId fileId = null;
        try {
            ScmFile file = createScmFile(ws, null, "scmFile", null, "sequoiacm");
            fileId = file.getFileId();
        }
        catch (ScmException e) {
            Assert.fail(e.getMessage());
        }
        return fileId;
    }

    public static ScmFile createScmFile(ScmWorkspace ws, String filePath, String fileName,
            String author, String FileTitle) throws ScmException {

        ScmFile file = ScmFactory.File.createInstance(ws);
        if (null != fileName) {
            file.setFileName(fileName);
        }
        file.setTitle(FileTitle);
        file.setContent(filePath);
        file.save();
        return file;
    }

    //上传文件基本 带自定义属性
    public static ScmFile createScmFile(ScmWorkspace ws, String filePath, String fileName,
            String author, String FileTitle,ScmClassProperties properties) throws ScmException {

        ScmFile file = ScmFactory.File.createInstance(ws);
        if (null != fileName) {
            file.setFileName(fileName);
        }
        file.setTitle(FileTitle);
        file.setClassProperties(properties);
        file.setContent(filePath);
        file.save();
        return file;
    }

    public static void removeScmFile(ScmWorkspace ws, ScmId fileId) throws ScmException {
        ScmFactory.File.deleteInstance(ws, fileId, true);
    }

    public static void removeScmFileSilence(ScmWorkspace ws, ScmId fileId) {
        try {
            removeScmFile(ws, fileId);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param filePath
     *            , local file
     * @param downloadPath
     *            , download file
     */
    public static void compareMd5(String filePath, String downloadPath) {
        try {
            String actMd5 = getMD5(downloadPath);
            String expMd5 = getMD5(filePath);
            Assert.assertEquals(actMd5, expMd5);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getMD5(String file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(file));
            return DigestUtils.md5Hex(fis);
        }
        finally {
            if (null != fis) {
                fis.close();
            }
        }
    }

    /**
     * random generate string
     *
     * @param length
     * @return character string
     */
    public static String generateString(int length) {
        try {
            String str = "adcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            Random random = new Random();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < length; i++) {
                int number = random.nextInt(62);
                sb.append(str.charAt(number));
            }
            return sb.toString();
        }
        catch (BaseException e) {
            throw e;
        }
    }

    /**
     * create directory
     *
     * @param dir
     */
    public static void createDir(String dir) {
        mkdir(new File(dir));
    }

    private static void mkdir(File file) {
        if (!file.getParentFile().exists()) {
            mkdir(file.getParentFile());
        }
        file.mkdir();
    }

    /**
     * remove directory including sub files and directories
     *
     * @param filePath
     */
    public static void deleteDir(String filePath) {
        File file = new File(filePath);
        deleteDir(file);
    }

    public static void deleteDir(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            }
            else {
                File[] files = file.listFiles();
                for (File subFile : files) {
                    deleteDir(subFile);
                }

                file.delete();
            }
        }
    }

    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isFile()) {
                boolean ret = file.delete();
            }
        }
    }

    /**
     * create file
     *
     * @param filePath
     * @param contentOutline
     * @param fileSize
     *            e.g: createFile("E:\a\a.txt", "adc123", 1024);
     */
    public static void createFile(String filePath, String contentOutline, int fileSize) {
        byte[] content = contentOutline.getBytes();
        int written = 0;
        FileOutputStream fos = null;

        try {
            createFile(filePath);

            File file = new File(filePath);
            fos = new FileOutputStream(file);
            while (written < fileSize) {
                int toWrite = fileSize - written;
                int len = content.length < toWrite ? content.length : toWrite;
                fos.write(content, 0, len);
                written += len;
            }
        }
        catch (IOException e) {
            System.out.println("create file failed, file=" + filePath);
            e.printStackTrace();
            closeStream(fos);
        }
    }

    /**
     * create empty file
     *
     * @param filePath
     * @throws IOException
     */
    public static void createFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }

        mkdir(file.getParentFile());
        file.createNewFile();
    }

    private static void closeStream(Closeable stream) {
        try {
            if (stream != null) {
                stream.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getMethodName() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }

    public static String getClassName() {
        String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
        int index = fullClassName.lastIndexOf(".");
        return fullClassName.substring(index + 1);
    }

    /**
     * create download path and file, by methodName and threadId
     *
     * @param localPath
     * @param methodsName
     * @param threadID
     * @param times
     * @return
     */
    public static String initDownloadPath(File localPath, String methodName, long threadId,
            int times) {
        String downloadPath = null;
        try {
            String downLoadDir = localPath + File.separator + methodName;
            ScmTestTools.createDir(downLoadDir);

            downloadPath = downLoadDir + File.separator + "thread" + threadId + "_" + times
                    + ".lob";
            times++;
        }
        catch (BaseException e) {
            System.out.println(downloadPath);
            e.printStackTrace();
        }
        return downloadPath;
    }

    public static List<SiteAccessInfo> getSiteAccessInfoList(BasicBSONList siteList)
            throws ScmException {
        List<SiteAccessInfo> infoList = new ArrayList<>();
        try {
            for (Object o : siteList) {
                BSONObject bo = (BSONObject) o;
                SiteAccessInfo info = bo.as(SiteAccessInfo.class);
                infoList.add(info);
            }

            return infoList;
        }
        catch (Exception e) {
            throw new ScmException(ScmError.SYSTEM_ERROR,
                    "parse siteAccessInfo failed", e);
        }
    }

    public static boolean isSiteExist(List<ScmFileLocation> locationList, int siteId) {
        for (ScmFileLocation location : locationList) {
            if (location.getSiteId() == siteId) {
                return true;
            }
        }

        return false;
    }

    public static String formatLocationList(List<ScmFileLocation> locationList) {
        StringBuilder sb = new StringBuilder();
        for (ScmFileLocation location : locationList) {
            sb.append(location.toString());
            sb.append(",");
        }

        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        else {
            return "";
        }
    }

    public static void checkSiteList(Sequoiadb sdb, String workspaceName, ScmId fileId,
            List<Integer> expectSiteList) throws ScmException {
        String csName = workspaceName + WORKSPACE_CS_META_SUFFIX;
        String clName = CL_FILE;
        CollectionSpace cs = sdb.getCollectionSpace(csName);
        DBCollection cl = cs.getCollection(clName);

        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLFILE_ID, fileId.get());
        DBCursor cursor = cl.query(matcher, null, null, null);
        if (!cursor.hasNext()) {
            throw new ScmException(-1, "file is not exist:cs=" + csName + ",cl=" + clName
                    + ",fileId=" + fileId.get());
        }

        BSONObject file = cursor.getNext();
        BasicBSONList bSiteList = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<SiteAccessInfo> siteList = getSiteAccessInfoList(bSiteList);
        for (Integer i : expectSiteList) {
            if (existInList(siteList, i)) {
                continue;
            }
            else {
                throw new ScmException(-1, "site is not exists in file record:expectSiteList="
                        + intListToString(expectSiteList) + ",file=" + file.toString());
            }
        }
    }

    private static boolean existInList(List<SiteAccessInfo> siteList, int checkedId) {
        for (SiteAccessInfo info : siteList) {
            if (checkedId == info.getSite_id()) {
                return true;
            }
        }

        return false;
    }

    private static String intListToString(List<Integer> valueList) {
        StringBuilder sb = new StringBuilder();
        for (Integer i : valueList) {
            sb.append(i + ",");
        }

        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        else {
            return sb.toString();
        }
    }

    public static Sequoiadb getSequoiadb(String url, String user, String passwd) {
        Sequoiadb sdb = new Sequoiadb(url, user, passwd);
        sdb.setSessionAttr(new BasicBSONObject("PreferedInstance", "m"));
        return sdb;
    }

    public static void readAndCheckFile(ScmWorkspace ws, ScmId fileId, String srcFile,
            String destFile, int readFlag) throws ScmException, IOException {
        OutputStream os = null;
        try {
            ScmFile file = ScmFactory.File.getInstance(ws, fileId);
            os = new FileOutputStream(new File(destFile));
            file.getContent(os, readFlag);
        }
        finally {
            ScmHelper.closeStream(os);
            os = null;
        }
        String srcMd5 = getMD5(srcFile);
        String destMd5 = getMD5(destFile);
        if (srcMd5 != destMd5) {
            Assert.assertEquals(destMd5, srcMd5);
        }
    }

    public static String getLobId(Sequoiadb sdb, String workspaceName, ScmId fileId)
            throws ScmException {
        String csName = workspaceName + WORKSPACE_CS_META_SUFFIX;
        String clName = CL_FILE;
        CollectionSpace cs = sdb.getCollectionSpace(csName);
        DBCollection cl = cs.getCollection(clName);

        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLFILE_ID, fileId.get());
        DBCursor cursor = cl.query(matcher, null, null, null);
        if (!cursor.hasNext()) {
            throw new ScmException(-1, "file is not exist:cs=" + csName + ",cl=" + clName
                    + ",fileId=" + fileId.get());
        }

        BSONObject file = cursor.getNext();
        return (String) file.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
    }

    public static boolean checkLob(String scmUrl, String user, String passwd, String wsName,
            ScmId fileId, String srcFile, String checkLobFile) throws IOException, ScmException {

        deleteFile(checkLobFile);
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                    scmUrl, user, passwd));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);
            ScmFile file = ScmFactory.File.getInstance(ws, fileId);
            file.getContentFromLocalSite(checkLobFile);
            String srcMd5 = getMD5(srcFile);
            String destMd5 = getMD5(checkLobFile);
            Assert.assertEquals(destMd5, srcMd5);
            return true;
        }
        finally {
            releaseSession(ss);
        }
    }

    public static void releaseSession(ScmSession ss) {
        if (null != ss) {
            ss.close();
        }
    }

    public static void deleteScmFile(String scmUrl, String user, String passwd,
            String workSpaceName, String fileId) {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                    scmUrl, user, passwd));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workSpaceName, ss);
            ScmFactory.File.deleteInstance(ws, new ScmId(fileId), true);
        }
        catch (ScmException e) {
            logger.warn("delete file failed", e);
        }
        finally {
            ss.close();
        }
    }

    public static void createSeekFile(int seek, String srcFile, String outputFile)
            throws FileNotFoundException, IOException {
        RandomAccessFile raf = new RandomAccessFile(srcFile, "rw");
        OutputStream os = new FileOutputStream(outputFile);
        raf.seek(seek);
        byte[] buf = new byte[1024];
        int readSize = 0;
        while (true) {
            readSize = raf.read(buf, 0, 1024);
            if (readSize < 0) {
                break;
            }
            os.write(buf, 0, readSize);
        }
        raf.close();
        os.close();
    }

    /**
     * query record by queryMacher,return boolean(is record exist)
     *
     * @param coordUrl
     * @param csName
     * @param clName
     * @param queryMacher
     * @return
     */
    public static boolean isRecordExist(String coordUrl, String user, String passwd, String csName,
            String clName, String queryMacher) {
        Sequoiadb db = new Sequoiadb(coordUrl, user, passwd);
        try {
            db.setSessionAttr(new BasicBSONObject("PreferedInstance", "m"));
            DBCollection metaCl = db.getCollectionSpace(csName).getCollection(clName);
            BSONObject res = metaCl.queryOne((BSONObject) JSON.parse(queryMacher), null, null,
                    null, 0);
            if (res == null) {
                return false;
            }
            return true;
        }
        finally {
            db.disconnect();
        }
    }

    /**
     * query Lob by queryMacher,return boolean(is record exist)
     *
     * @param coordUrl
     * @param csName
     * @param clName
     * @param queryMacher
     * @return
     */
    public static boolean isLobExist(String coordUrl, String csName, String clName, String id) {
        Sequoiadb db = new Sequoiadb(coordUrl, "", "");
        try {
            db.setSessionAttr(new BasicBSONObject("PreferedInstance", "m"));
            DBCollection cl = db.getCollectionSpace(csName).getCollection(clName);
            cl.openLob(new ObjectId(id)).close();
            return true;
        }
        catch (BaseException e) {
            if (e.getErrorCode() == -4) {
                return false;
            }
            else {
                throw e;
            }
        }
        finally {
            db.disconnect();
        }
    }

    public static boolean isLocalSiteDataExist(String scmUrl, String user, String passwd,
            String wsName, ScmId fileId) throws ScmException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                 scmUrl, user, passwd));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);
            ScmFactory.File.getInstance(ws, fileId).getContentFromLocalSite(new ByteArrayOutputStream());
        }
        catch (ScmException e) {
            if (e.getError().equals(ScmError.DATA_NOT_EXIST)) {
                return false;
            }
            throw e;
        }
        finally {
            releaseSession(ss);
        }
        return true;
    }

    public static String randomString(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(SAMPLE.length());
            sb.append(SAMPLE.charAt(index));
        }

        return sb.toString();
    }

    public static ScmSiteInfo getSiteInfo(ScmSession ss, int siteId) throws ScmException {
        ScmCursor<ScmSiteInfo> cursor = ScmFactory.Site.listSite(ss);
        try {
            while (cursor.hasNext()) {
                ScmSiteInfo info = cursor.getNext();
                logger.info("ScmSiteInfo is --->>>:"+info.toString());
                if (info.getId() == siteId) {
                    return info;
                }
            }

            return null;
        }
        finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    public static void clearFile(ScmWorkspace ws, String path) throws ScmException {
        try {
            ScmFile f = ScmFactory.File.getInstanceByPath(ws, path);
            f.delete(true);
        }catch(ScmException e) {
            if(e.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode()) {
                throw e;
            }
        }
    }

    public static void clearFile(ScmWorkspace ws, ScmId id) throws ScmException {
        try {
            ScmFactory.File.deleteInstance(ws, id, true);
        }
        catch (ScmException e) {
            if(e.getError() != ScmError.FILE_NOT_FOUND) {
                throw e;
            }
        }
    }

    public static void clearDir(ScmWorkspace ws, String path) throws ScmException {
        try {
            ScmFactory.Directory.deleteInstance(ws, path);
        }
        catch (ScmException e) {
            if(e.getError() != ScmError.DIR_NOT_FOUND) {
                throw e;
            }
        }
    }

    public static void clearBatch(ScmWorkspace ws, ScmId id) throws ScmException {
        try {
            ScmFactory.Batch.deleteInstance(ws, id);
        }
        catch (ScmException e) {
            if(e.getError() != ScmError.BATCH_NOT_FOUND) {
                throw e;
            }
        }
    }

    public static void clearB(ScmWorkspace ws, ScmId id) throws ScmException {
        try {
            ScmFactory.Batch.deleteInstance(ws, id);
        }
        catch (ScmException e) {
            if(e.getError() != ScmError.BATCH_NOT_FOUND) {
                throw e;
            }
        }
    }

    public static void main(String[] args) throws ScmException {
        BSONObject o = ScmQueryBuilder.start("site_id").is(3).and("last_access_time")
                .greaterThan(12345).get();
        BSONObject r = ScmQueryBuilder.start("site_list").elemMatch(o).get();
        System.out.println("r=" + r.toString());
    }
}
