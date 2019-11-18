package com.sequoiacm.client.core.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;
import com.sequoiacm.client.util.ScmTestTools;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;

public class TestScmFileCreate extends ScmTestBase {

    private static final int SF_SEND_SIZE = 5 * 1024;
    private static final int BF_SEND_SIZE = 5 * 1024 * 1024;
    private static final String SMALL_FILE_NAME = "create_smallFile.txt";
    private static final String BIG_FILE_NAME = "create_bigFile.txt";
    private static final String BIG_FILE_NAME_CHECK = BIG_FILE_NAME + ".check";
    private static final String DIRECTORY_NAME = "create_tmpFolder";
    private static final String UNEXIST_FILE_NAME = "create_nonefile.txt";
    private static final String WITHOUT_CONTENT_NAME = "noneContent";

    private static int sfSize, bfSize;

    private ScmSession ss = null;

    @BeforeClass
    public void setUp() throws IOException {
        FileOutputStream fos = null;
        int offset;
        File bFile, sFile, tmpFolder;

        // create bigFile.txt
        try {
            bFile = new File(dataDirectory + File.separator + BIG_FILE_NAME);
            if (!bFile.createNewFile()) {
                Assert.fail("[createFile] create " + BIG_FILE_NAME + " failed");
            }

            fos = new FileOutputStream(bFile);

            byte[] bBuf = new String(BIG_FILE_NAME).getBytes();
            offset = 0;
            while (offset < BF_SEND_SIZE) {
                fos.write(bBuf);
                offset += bBuf.length;
            }
            bfSize = offset;
        }
        finally {
            if (null != fos) {
                fos.close();
            }
        }

        // create smallFile.txt
        try {
            sFile = new File(dataDirectory + File.separator + SMALL_FILE_NAME);
            if (!sFile.createNewFile()) {
                Assert.fail("[createFile] create " + SMALL_FILE_NAME + " failed");
            }

            fos = new FileOutputStream(sFile);

            byte[] sBuf = new String(SMALL_FILE_NAME).getBytes();
            offset = 0;
            while (offset < SF_SEND_SIZE) {
                fos.write(sBuf);
                offset += sBuf.length;
            }
            sfSize = offset;
        }
        finally {
            if (null != fos) {
                fos.close();
            }
        }

        // create directory
        tmpFolder = new File(dataDirectory + File.separator + DIRECTORY_NAME);
        if (!tmpFolder.exists() && !tmpFolder.isDirectory()) {
            if (!tmpFolder.mkdir()) {
                Assert.fail("[createFile] create " + DIRECTORY_NAME + " failed");
            }
        }
    }

    @AfterClass
    public void tearDown() {
        File bFile = new File(dataDirectory + File.separator + BIG_FILE_NAME);
        if (bFile.exists()) {
            if (!bFile.delete()) {
                Assert.fail("[createFile] delete " + BIG_FILE_NAME + " failed");
            }
        }
        else {
            Assert.fail("[createFile] not found " + BIG_FILE_NAME);
        }

        File bcFile = new File(dataDirectory + File.separator + BIG_FILE_NAME_CHECK);
        if (bcFile.exists()) {
            if (!bcFile.delete()) {
                Assert.fail("[createFile] delete " + BIG_FILE_NAME + " failed");
            }
        }
        else {
            Assert.fail("[createFile] not found " + BIG_FILE_NAME);
        }

        File sFile = new File(dataDirectory + File.separator + SMALL_FILE_NAME);
        if (sFile.exists()) {
            if (!sFile.delete()) {
                Assert.fail("[createFile] delete " + SMALL_FILE_NAME + " failed");
            }
        }
        else {
            Assert.fail("[createFile] not found " + SMALL_FILE_NAME);
        }

        File tmpFolder = new File(dataDirectory + File.separator + DIRECTORY_NAME);
        if (tmpFolder.exists()) {
            if (!tmpFolder.delete()) {
                Assert.fail("[createFile] delete " + DIRECTORY_NAME + " failed");
            }
        }
        else {
            Assert.fail("[createFile] not found " + DIRECTORY_NAME);
        }
    }

    @Test
    public void testCreateBigFileByPath() throws ScmException {
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmId fileId = null;
            String author = "author";

            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName("BFName1");
            file.setTitle("BFTitle1");
            file.setContent(dataDirectory + File.separator + BIG_FILE_NAME);
            // file.setPropertyType(PropertyType.VIDEO);
            // file.setFolder(folder);
            file.setMimeType(MimeType.PLAIN);
            file.setAuthor(author);

            fileId = file.save();
            Assert.assertNotNull(fileId);
            Assert.assertNotNull(file.getUser());
            Assert.assertNotNull(file.getAuthor());
            Assert.assertEquals(file.getAuthor(), author);
            Assert.assertEquals(file.getUser(), file.getUpdateUser());

            ScmFile file2 = ScmFactory.File.getInstance(ws, fileId);

            Assert.assertEquals(file2.getSize(), file.getSize());
            Assert.assertNotNull(fileId);
            Assert.assertEquals(file2.getFileId(), file.getFileId());
            Assert.assertEquals(file2.getMajorVersion(), 1);
            Assert.assertEquals(file2.getMinorVersion(), 0);
            Assert.assertEquals(file2.getMajorVersion(), file.getMajorVersion());
            Assert.assertEquals(file2.getMinorVersion(), file.getMinorVersion());
            Assert.assertEquals(file2.getAuthor(), file.getAuthor());

            Assert.assertEquals(file2.getUser(), file.getUser());
            Assert.assertEquals(file2.getCreateTime(), file.getCreateTime());
            Assert.assertEquals(file2.getUpdateUser(), file.getUpdateUser());
            Assert.assertEquals(file2.getUpdateUser(), file.getUpdateUser());

            file2.getContent(dataDirectory + File.separator + BIG_FILE_NAME_CHECK);
            String file1MD5 = ScmTestTools.getMD5(dataDirectory + File.separator + BIG_FILE_NAME);
            String file2MD5 = ScmTestTools.getMD5(dataDirectory + File.separator
                    + BIG_FILE_NAME_CHECK);
            Assert.assertNotNull(file1MD5);
            Assert.assertEquals(file2MD5, file1MD5, file2MD5);
        }
        finally {
            session.close();
        }
    }

    @Test
    public void testCreateBigFileByStream() throws ScmException, IOException {
        ScmSession session = null;
        FileInputStream fis = null;
        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmId fileId = null;

            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName("BFName2");
            file.setTitle("BFTitle2");
            fis = new FileInputStream(new File(dataDirectory + File.separator + BIG_FILE_NAME));
            file.setContent(fis);
            // file.setPropertyType(PropertyType.VIDEO);
            // file.setFolder(folder);
            file.setMimeType(MimeType.PLAIN);

            fileId = file.save();

            Assert.assertNotNull(fileId);
            Assert.assertEquals(file.getSize(), bfSize);
            Assert.assertEquals(file.getFileId(), fileId);
            Assert.assertNotEquals("", file.getFileId());
        }
        finally {
            if (null != fis) {
                fis.close();
            }
            session.close();
        }
    }

    @Test
    public void testCreateSmallFileByPath() throws ScmException {
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmId fileId = null;

            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName("SFName1");
            file.setTitle("SFTitle1");
            file.setContent(dataDirectory + File.separator + SMALL_FILE_NAME);
            // file.setPropertyType(PropertyType.VIDEO);
            // file.setFolder(folder);
            file.setMimeType(MimeType.PLAIN);

            fileId = file.save();
            Assert.assertNotNull(fileId);
            Assert.assertEquals(file.getSize(), sfSize);
            Assert.assertEquals(file.getFileId(), fileId);
            Assert.assertNotEquals(file.getFileId(), "");
        }
        finally {
            session.close();
        }
    }

    @Test
    public void testCreateSmallFileByStream() throws IOException, ScmException {
        ScmSession session = null;
        FileInputStream fis = null;
        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmId fileId = null;

            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName("SFName2");
            file.setTitle("SFTitle2");
            fis = new FileInputStream(new File(dataDirectory + File.separator + SMALL_FILE_NAME));
            file.setContent(fis);
            // file.setPropertyType(PropertyType.VIDEO);
            // file.setFolder(folder);
            file.setMimeType(MimeType.PLAIN);

            fileId = file.save();

            Assert.assertNotNull(fileId);
            Assert.assertEquals(file.getSize(), sfSize);
            Assert.assertEquals(file.getFileId(), fileId);
            Assert.assertNotEquals(file.getFileId(), "");
        }
        finally {
            if (null != fis) {
                fis.close();
            }
            session.close();
        }
    }

    @Test
    public void testCreateWithNullFileName() throws ScmException {
        ScmSession session = null;
        boolean hasException = false;
        ScmId fileId = null;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName(null);
            file.setTitle("Title");
            file.setContent(dataDirectory + File.separator + SMALL_FILE_NAME);
            file.setMimeType(MimeType.PLAIN);

            fileId = file.save();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.ATTRIBUTE_FORMAT_ERROR);
            hasException = true;
        }
        finally {
            session.close();
        }

        Assert.assertNull(fileId);
        Assert.assertEquals(hasException, true);
    }

    @Test
    public void testCreateWithEmptyFileName() throws ScmException {
        ScmSession session = null;
        boolean hasException = false;
        ScmId fileId = null;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName("");
            file.setTitle("Title");
            file.setContent(dataDirectory + File.separator + SMALL_FILE_NAME);
            file.setMimeType(MimeType.PLAIN);

            fileId = file.save();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.ATTRIBUTE_FORMAT_ERROR);
            hasException = true;
        }
        finally {
            session.close();
        }

        Assert.assertNull(fileId);
        Assert.assertEquals(hasException, true);
    }

    @Test
    public void testCreateWithNullTitle() throws ScmException {
        ScmSession session = null;
        boolean hasException = false;
        ScmId fileId = null;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName("FileName");
            file.setTitle(null);
            file.setMimeType(MimeType.PLAIN);
            file.setContent(dataDirectory + File.separator + SMALL_FILE_NAME);

            fileId = file.save();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.ATTRIBUTE_FORMAT_ERROR);
            hasException = true;
        }
        finally {
            session.close();
        }

        Assert.assertNull(fileId);
        Assert.assertEquals(hasException, true);
    }

    @Test
    public void testCreateWithEmptyTitle() throws ScmException {
        ScmSession session = null;
        boolean hasException = false;
        ScmId fileId = null;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName("FileName");
            file.setTitle("");
            file.setMimeType(MimeType.PLAIN);
            file.setContent(dataDirectory + File.separator + SMALL_FILE_NAME);

            fileId = file.save();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.ATTRIBUTE_FORMAT_ERROR);
            hasException = true;
        }
        finally {
            session.close();
        }

        Assert.assertNull(fileId);
        Assert.assertEquals(hasException, true);
    }

    @Test
    public void testCreateWithNullMimeType() throws ScmException {
        ScmSession session = null;
        boolean hasException = false;
        ScmId fileId = null;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName("FileName");
            file.setTitle("Title");
            // file.setMimeType(null);
            // file.setContent(dataDirectory + File.separator +
            // SMALL_FILE_NAME);

            fileId = file.save();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.ATTRIBUTE_FORMAT_ERROR);
            hasException = true;
        }
        finally {
            session.close();
        }

        Assert.assertNull(fileId);
        Assert.assertEquals(hasException, true);
    }

    @Test
    public void testCreateFileWithoutContent() throws ScmException {
        ScmSession session = null;
        ScmId fileId = null;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName(WITHOUT_CONTENT_NAME);
            file.setTitle("FileTitle");
            file.setMimeType(MimeType.PLAIN);

            fileId = file.save();

            Assert.assertNotNull(fileId);
        }
        finally {
            session.close();
        }
    }

    @Test
    public void testCreateFileWithUnExistPath() throws ScmException {
        ScmSession session = null;
        boolean hasException = false;
        ScmId fileId = null;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName("FileName");
            file.setTitle("Title");
            file.setMimeType(MimeType.PLAIN);
            file.setContent(dataDirectory + File.separator + UNEXIST_FILE_NAME);

            fileId = file.save();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_NOT_EXIST);
            hasException = true;
        }
        finally {
            session.close();
        }

        Assert.assertNull(fileId);
        Assert.assertEquals(hasException, true);
    }

    @Test
    public void testCreateFileWithDirectoryPath() throws ScmException {
        ScmSession session = null;
        boolean hasException = false;
        ScmId fileId = null;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName("FileName");
            file.setTitle("Title");
            file.setMimeType(MimeType.PLAIN);
            file.setContent(dataDirectory + File.separator + DIRECTORY_NAME);

            fileId = file.save();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_IS_DIRECTORY);
            hasException = true;
        }
        finally {
            session.close();
        }

        Assert.assertNull(fileId);
        Assert.assertEquals(hasException, true);
    }

    @Test
    public void testCreateFileWithAutoParseMimeType() throws ScmException {
        ScmSession session = null;
        ScmId fileId = null;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setAuthor("Author");
            file.setFileName("FileName");
            file.setTitle("Title");
            file.setContent(dataDirectory + File.separator + SMALL_FILE_NAME);

            fileId = file.save();

            Assert.assertEquals(file.getMimeType(), MimeType.PLAIN.getType());
            Assert.assertEquals(file.getUser(), file.getUpdateUser());

            ScmFile file2 = ScmFactory.File.getInstance(ws, fileId);
            Assert.assertEquals(file2.getSize(), file.getSize());
            Assert.assertNotNull(fileId);
            Assert.assertEquals(file2.getFileId(), file.getFileId());
            Assert.assertEquals(file2.getMajorVersion(), 1);
            Assert.assertEquals(file2.getMinorVersion(), 0);
            Assert.assertEquals(file2.getMajorVersion(), file.getMajorVersion());
            Assert.assertEquals(file2.getMinorVersion(), file.getMinorVersion());
            Assert.assertEquals(file2.getCreateTime(), file.getCreateTime());
            Assert.assertEquals(file2.getUser(), file.getUser());
            Assert.assertEquals(file2.getUpdateUser(), file.getUpdateUser());
            Assert.assertEquals(file2.getUpdateUser(), file.getUpdateUser());
            Assert.assertEquals(file2.getCreateTime(), file.getCreateTime());
            Assert.assertEquals(file2.getAuthor(), file.getAuthor());
            Assert.assertEquals(file2.getAuthor(), "Author");
            Assert.assertEquals(file2.getMimeType(), MimeType.PLAIN.getType());
        }
        finally {
            session.close();
        }
    }

    // TODO : modify Server Time to supplement create file with different
    // months.

    @Test
    public void testParameter() throws ScmException {
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmFile file = ScmFactory.File.createInstance(ws);

            String fileName = "fileName";
            String title = "title";
            String author = "author";
            // ScmClassProperties prop = new ScmClassProperties("0");

            file.setFileName(fileName);
            file.setTitle(title);
            file.setMimeType(MimeType.PLAIN);
            // file.setPropertyType(PropertyType.VIDEO);
            // file.setClassProperties(prop);
            file.setAuthor(author);

            Assert.assertEquals(file.getFileName(), fileName);
            Assert.assertEquals(file.getTitle(), title);
            Assert.assertEquals(file.getMimeType(), MimeType.PLAIN.getType());
            // Assert.assertEquals(file.getPropertyType(), PropertyType.VIDEO);
            // Assert.assertEquals(file.getClassProperties(), prop);
            Assert.assertEquals(file.getAuthor(), author);
        }
        finally {
            session.close();
        }
    }

}
