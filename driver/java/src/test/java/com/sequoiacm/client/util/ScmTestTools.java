package com.sequoiacm.client.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;
import org.testng.Assert;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.MimeType;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

public class ScmTestTools {

    public static void main(String[] args) throws ScmException, IOException, InterruptedException {
        //        ScmSession ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
        //                new ScmConfigOption("ubuntu-200-031", 15000, "user", "passwd"));
        //        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace("ws_default", ss);
        //        ScmFile f = ScmFactory.File.createInstance(ws);
        //        f.setContent("E:\\scm_test_data\\test.txt");
        //        ScmId id = f.save();
        //        System.out.println(id.get());
        //        System.out.println("save success");

        System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));

        //        ScmSession ssB = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
        //                new ScmConfigOption("ubuntu-200-031", 15010, "user", "passwd"));
        //        ScmWorkspace wsB = ScmFactory.Workspace.getWorkspace("ws_default", ssB);
        //        ScmFile fB = ScmFactory.File.getInstance(wsB, id);
        //
        //        ScmInputStream is = ScmFactory.File.createInputStream(InputStreamType.UNSEEKABLE, fB);
        //        java.io.File localFile = new java.io.File("E:\\scm_test_data\\3.tar.gz");
        //        FileOutputStream os = new FileOutputStream(localFile);
        //        while (true) {
        //            byte[] b = new byte[100];
        //            int len = is.read(b, 0, 100);
        //            is.seek(0, 0);
        //            if (len == -1) {
        //                break;
        //            }
        //            System.out.println(len);
        //            Thread.sleep(10000);
        //        }
        //
        //        is.read(os);
        //        os.close();
        //        is.close();
        //        ss.close();
        //
        //        System.out.println(getMD5("E:\\scm_test_data\\2.tar.gz")
        //                .equals(getMD5("E:\\scm_test_data\\3.tar.gz")));

    }

    public static String getMD5(InputStream in) {
        MessageDigest digest = null;
        byte buffer[] = new byte[1024];
        int length = 0;
        try {
            digest = MessageDigest.getInstance("MD5");
            while ((length = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, length);
            }
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }

    public static String getMD5(String fullName) {
        File file = new File(fullName);
        if (!file.isFile()) {
            return null;
        }

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            return getMD5(in);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        finally {
            try {
                in.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static ScmFile createFile(ScmSession ss, String workspaceName, String testName,
            String contents) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, ss);
        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setTitle("title_" + testName);
        file.setFileName("name_" + testName);
        file.setMimeType(MimeType.PLAIN);
        try {
            file.setContent(new ByteArrayInputStream(contents.getBytes("utf-8")));
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Assert.assertTrue(false, e.toString());
        }
        file.save();
        return file;
    }

    public static ScmSession createSession(String url, String user, String password) {
        try {
            return ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
        }
        catch (ScmException e) {
            Assert.assertTrue(false, "connect to server failed:url=" + url + ",user=" + user
                    + ",passwd=" + password + ",e=" + e);
        }

        return null;
    }

    public static void releaseSession(ScmSession ss) {
        if (ss != null) {
            ss.close();
        }
    }

    public static void removeFileIfExist(String sdbUrl, String user, String passwd,
            String workspaceName, ScmFile file) {

        if (file == null) {
            return;
        }

        Sequoiadb sdb = null;
        try {
            String fileID = file.getFileId().get();
            sdb = new Sequoiadb(sdbUrl, user, passwd);
            String csName = workspaceName + "_META";
            String clName = "FILE";
            String clHisName = "FILE_HISTORY";
            List<String> currentLobList = getLobList(sdb, csName, clName, fileID);
            List<String> historyLobList = getLobList(sdb, csName, clHisName, fileID);

            String lobCsName = workspaceName + "_LOB";
            String lobClName = "LOB";
            removeLob(sdb, lobCsName, lobClName, currentLobList);
            removeLob(sdb, lobCsName, lobClName, historyLobList);

            removeRecord(sdb, csName, clName, fileID);
            removeRecord(sdb, csName, clHisName, fileID);
        }
        finally {
            sdb.disconnect();
        }
    }

    private static void removeRecord(Sequoiadb sdb, String csName, String clName, String fileID) {
        try {
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            cl.delete(new BasicBSONObject(FieldName.FIELD_CLFILE_ID, fileID));
        }
        catch (BaseException e) {
            e.printStackTrace();
        }
    }

    private static void removeLob(Sequoiadb sdb, String lobCsName, String lobClName,
            List<String> lobList) {
        try {
            CollectionSpace cs = sdb.getCollectionSpace(lobCsName);
            DBCollection cl = cs.getCollection(lobClName);
            for (int i = 0; i < lobList.size(); i++) {
                cl.removeLob(new ObjectId(lobList.get(i)));
            }
        }
        catch (BaseException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getLobList(Sequoiadb sdb, String csName, String clName,
            String fileID) {

        DBCursor cursor = null;
        List<String> lobList = new ArrayList<String>();
        try {
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            cursor = cl.query(new BasicBSONObject(FieldName.FIELD_CLFILE_ID, fileID), null, null,
                    null);

            lobList = new ArrayList<String>();
            while (cursor.hasNext()) {
                BSONObject o = cursor.getNext();
                lobList.add((String) o.get(FieldName.FIELD_CLFILE_FILE_DATA_ID));
            }
        }
        catch (BaseException e) {
            e.printStackTrace();
        }
        finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return lobList;
    }
}
