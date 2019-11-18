package com.sequoiacm.client.core.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.sequoiacm.client.core.*;
import com.sequoiacm.exception.ScmError;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;

public class TestScmFileGetContent extends ScmTestBase {

    private static final String SMALL_FILE_NAME = "getContent_smallFile.txt";
    private static final String BIG_FILE_NAME = "getContent_bigFile.txt";
    private static final String EXIST_FILE_NAME = "getContent_existFile.txt";
    private static final String DIRECTORY_NAME = "getContent_tmpFolder";
    private static final String WITHOUT_CONTENT_NAME = "noneContent";

    private static final String SMALL_FILE_CONTENT = "create_smallFile.txt";
    private static final String BIG_FILE_CONTENT = "create_bigFile.txt";

    @BeforeClass
    public void setUp() throws IOException {
        File eFile = new File(dataDirectory + File.separator + EXIST_FILE_NAME);
        if (!eFile.createNewFile()) {
            Assert.fail("[getContent] create " + EXIST_FILE_NAME + " failed");
        }

        File dFile = new File(dataDirectory + File.separator + DIRECTORY_NAME);
        if (!dFile.mkdir()) {
            Assert.fail("[getContent] mkdir " + DIRECTORY_NAME + " failed");
        }
    }

    @AfterClass
    public void clean() {
        File eFile = new File(dataDirectory + File.separator + EXIST_FILE_NAME);
        if (eFile.exists()) {
            if (!eFile.delete()) {
                Assert.fail("[getContent] delete " + EXIST_FILE_NAME + " failed");
            }
        }
        else {
            Assert.fail("[getContent] not found " + EXIST_FILE_NAME);
        }

        File dFile = new File(dataDirectory + File.separator + DIRECTORY_NAME);
        if (dFile.exists()) {
            if (!dFile.delete()) {
                Assert.fail("[getContent] delete " + DIRECTORY_NAME + " failed");
            }
        }
        else {
            Assert.fail("[getContent] not found " + DIRECTORY_NAME);
        }
    }

    @AfterMethod
    public void tearDown() {
        File bFile = new File(dataDirectory + File.separator + BIG_FILE_NAME);
        if (bFile.exists()) {
            if (!bFile.delete()) {
                Assert.fail("[getContent] delete " + BIG_FILE_NAME + " failed");
            }
        }

        File sFile = new File(dataDirectory + File.separator + SMALL_FILE_NAME);
        if (sFile.exists()) {
            if (!sFile.delete()) {
                Assert.fail("[getContent] delete " + SMALL_FILE_NAME + " failed");
            }
        }
    }

    // speed slowly
    private boolean checkContent(String fileName, byte[] content, long fileSize)
            throws IOException {
        boolean check = true;
        FileInputStream fis = null;
        try {
            File file = new File(dataDirectory + File.separator + fileName);
            Assert.assertEquals(file.length(), fileSize);

            if (0 == fileSize) {
                return true;
            }

            fis = new FileInputStream(file);
            byte[] buf = new byte[content.length];
            int len, index, offset;
            while (true) {
                len = offset = 0;
                while (offset < content.length) {
                    len = fis.read(buf, offset, content.length - offset);
                    if (-1 == len) {
                        break;
                    }
                    offset += len;
                }

                for (index = 0; index < offset; index++) {
                    if (buf[index] != content[index]) {
                        check = false;
                        break;
                    }
                }

                if (-1 == len) {
                    break;
                }
            }
            return check;
        }
        finally {
            if (null != fis) {
                fis.close();
            }
        }
    }

    @Test
    public void testGetBigFileContentByOutputPath() throws ScmException, IOException {
        ScmSession session = null;
        ScmCursor<ScmFileBasicInfo> fbiCursor = null;
        long fileSize = -1L;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
                    .is("BFName1");

            fbiCursor = ScmFactory.File.listInstance(ws, ScmType.ScopeType.SCOPE_ALL, b.get());
            ScmFileBasicInfo fbi = null;
            while (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
                Assert.assertEquals(fbi.getFileName(), "BFName1");
            }
            fbiCursor.close();

            Assert.assertNotNull(fbi);
            ScmFile file = ScmFactory.File.getInstance(ws, fbi.getFileId());
            String outputPath = dataDirectory + File.separator + BIG_FILE_NAME;
            file.getContent(outputPath);
            fileSize = file.getSize();
        }
        finally {
            session.close();
        }

        boolean check = checkContent(BIG_FILE_NAME, new String(BIG_FILE_CONTENT).getBytes(),
                fileSize);
        Assert.assertEquals(check, true);
    }

    @Test
    public void testGetBigFileContentByOutputStream() throws ScmException, IOException {
        ScmSession session = null;
        OutputStream os = null;
        long fileSize = -1L;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
                    .is("BFName2");
            ScmCursor<ScmFileBasicInfo> fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());
            ScmFileBasicInfo fbi = null;
            while (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
                Assert.assertEquals(fbi.getFileName(), "BFName2");
            }
            fbiCursor.close();

            Assert.assertNotNull(fbi);
            ScmFile file = ScmFactory.File.getInstance(ws, fbi.getFileId());
            String outputPath = dataDirectory + File.separator + BIG_FILE_NAME;
            os = new FileOutputStream(new File(outputPath));
            file.getContent(os);
            fileSize = file.getSize();
        }
        finally {
            if (null != os) {
                os.close();
            }
            session.close();
        }

        boolean check = checkContent(BIG_FILE_NAME, new String(BIG_FILE_CONTENT).getBytes(),
                fileSize);
        Assert.assertEquals(check, true);
    }

    @Test
    public void testGetSmallFileContentByOutputPath() throws ScmException, IOException {
        ScmSession session = null;
        long fileSize = -1L;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.TITLE).is("SFTitle1");

            ScmCursor<ScmFileBasicInfo> fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());
            ScmFileBasicInfo fbi = null;
            if (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
            }
            fbiCursor.close();

            Assert.assertNotNull(fbi);
            ScmFile file = ScmFactory.File.getInstance(ws, fbi.getFileId());
            String outputPath = dataDirectory + File.separator + SMALL_FILE_NAME;
            file.getContent(outputPath);
            fileSize = file.getSize();
        }
        finally {
            session.close();
        }

        boolean check = checkContent(SMALL_FILE_NAME, new String(SMALL_FILE_CONTENT).getBytes(),
                fileSize);
        Assert.assertEquals(check, true);
    }

    @Test
    public void testGetSmallFileContentByOutputStream() throws ScmException, IOException {
        ScmSession session = null;
        OutputStream os = null;
        long fileSize = -1L;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
                    .is("SFName2");

            ScmCursor<ScmFileBasicInfo> fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());
            ScmFileBasicInfo fbi = null;
            if (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
            }
            fbiCursor.close();

            ScmFile file = ScmFactory.File.getInstance(ws, fbi.getFileId());
            String outputPath = dataDirectory + File.separator + SMALL_FILE_NAME;
            os = new FileOutputStream(new File(outputPath));
            file.getContent(os);
            fileSize = file.getSize();

            Assert.assertNotNull(fbi);
        }
        finally {
            if (null != os) {
                os.close();
            }
            session.close();
        }

        boolean check = checkContent(SMALL_FILE_NAME, new String(SMALL_FILE_CONTENT).getBytes(),
                fileSize);
        Assert.assertEquals(check, true);
    }

    @Test
    public void testGetNoneContentByOutputPath() throws ScmException, IOException {
        ScmSession session = null;
        long fileSize = -1L;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
                    .is(WITHOUT_CONTENT_NAME);

            ScmCursor<ScmFileBasicInfo> fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());
            ScmFileBasicInfo fbi = null;
            if (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
            }
            fbiCursor.close();

            Assert.assertNotNull(fbi);
            ScmFile file = ScmFactory.File.getInstance(ws, fbi.getFileId());
            String outputPath = dataDirectory + File.separator + SMALL_FILE_NAME;
            file.getContent(outputPath);
            fileSize = file.getSize();
        }
        finally {
            session.close();
        }

        byte[] buf = new byte[0];
        boolean check = checkContent(SMALL_FILE_NAME, buf, fileSize);
        Assert.assertEquals(check, true);
    }

    @Test
    public void testGetNoneContentByOutputStream() throws ScmException, IOException {
        ScmSession session = null;
        FileOutputStream fos = null;
        long fileSize = -1L;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
                    .is(WITHOUT_CONTENT_NAME);

            ScmCursor<ScmFileBasicInfo> fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());
            ScmFileBasicInfo fbi = null;
            if (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
            }
            fbiCursor.close();

            Assert.assertNotNull(fbi);
            ScmFile file = ScmFactory.File.getInstance(ws, fbi.getFileId());
            String outputPath = dataDirectory + File.separator + SMALL_FILE_NAME;
            fos = new FileOutputStream(new File(outputPath));

            file.getContent(fos);
            fileSize = file.getSize();
        }
        finally {
            if (null != fos) {
                fos.close();
            }
            session.close();
        }

        byte[] buf = new byte[0];

        boolean check = checkContent(SMALL_FILE_NAME, buf, fileSize);
        Assert.assertEquals(check, true);
    }

    @Test
    public void testGetSmallFileContentByOutputStreamWhenExistFile()
            throws ScmException, IOException {
        ScmSession session = null;
        OutputStream os = null;
        long fileSize = -1L;
        boolean hasException = false;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
                    .is("SFName2");

            ScmCursor<ScmFileBasicInfo> fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());
            ScmFileBasicInfo fbi = null;
            if (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
            }
            fbiCursor.close();

            ScmFile file = ScmFactory.File.getInstance(ws, fbi.getFileId());
            String outputPath = dataDirectory + File.separator + EXIST_FILE_NAME;
            os = new FileOutputStream(new File(outputPath));
            file.getContent(os);
            fileSize = file.getSize();

            Assert.assertNotNull(fbi);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_ALREADY_EXISTS);
            hasException = true;
        }
        finally {
            if (null != os) {
                os.close();
            }
            session.close();
        }

        Assert.assertEquals(hasException, false);
        boolean check = checkContent(EXIST_FILE_NAME, new String(SMALL_FILE_CONTENT).getBytes(),
                fileSize);
        Assert.assertEquals(check, true);
    }

    @Test
    public void testGetSmallFileContentByOutputPathWhenExistFile()
            throws ScmException, IOException {
        ScmSession session = null;
        long fileSize = -1L;
        boolean hasException = false;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
                    .is("SFName2");

            ScmCursor<ScmFileBasicInfo> fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());
            ScmFileBasicInfo fbi = null;
            if (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
            }
            fbiCursor.close();

            ScmFile file = ScmFactory.File.getInstance(ws, fbi.getFileId());
            String outputPath = dataDirectory + File.separator + EXIST_FILE_NAME;
            file.getContent(outputPath);

            Assert.assertNotNull(fbi);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_ALREADY_EXISTS);
            hasException = true;
        }
        finally {
            session.close();
        }

        Assert.assertEquals(hasException, true);
    }

    public void testGetSmallFileContentByOutputPathWhenDirectory()
            throws ScmException, IOException {
        ScmSession session = null;
        boolean hasException = false;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
                    .is("SFName2");

            ScmCursor<ScmFileBasicInfo> fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());
            ScmFileBasicInfo fbi = null;
            if (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
            }
            fbiCursor.close();

            ScmFile file = ScmFactory.File.getInstance(ws, fbi.getFileId());
            String outputPath = dataDirectory + File.separator + DIRECTORY_NAME;
            file.getContent(outputPath);

            Assert.assertNotNull(fbi);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_IS_DIRECTORY);
            hasException = true;
        }
        finally {
            session.close();
        }

        Assert.assertEquals(hasException, true);
    }

    public void testGetSmallFileContentByOutputPathWhenPathIsNull() throws ScmException {
        ScmSession session = null;
        boolean hasException = false;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.TITLE).is("SFTitle1");

            ScmCursor<ScmFileBasicInfo> fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());
            ScmFileBasicInfo fbi = null;
            if (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
            }
            fbiCursor.close();

            Assert.assertNotNull(fbi);
            ScmFile file = ScmFactory.File.getInstance(ws, fbi.getFileId());
            String outputPath = null;
            file.getContent(outputPath);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT);
            hasException = true;
        }
        finally {
            session.close();
        }
        Assert.assertEquals(hasException, true);
    }

    public void testGetSmallFileContentByOutputPathWhenPathIsEmpty() throws ScmException {
        ScmSession session = null;
        boolean hasException = false;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.TITLE).is("SFTitle1");

            ScmCursor<ScmFileBasicInfo> fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());
            ScmFileBasicInfo fbi = null;
            if (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
            }
            fbiCursor.close();

            Assert.assertNotNull(fbi);
            ScmFile file = ScmFactory.File.getInstance(ws, fbi.getFileId());
            String outputPath = "";
            file.getContent(outputPath);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT);
            hasException = true;
        }
        finally {
            session.close();
        }
        Assert.assertEquals(hasException, true);
    }

    public void testGetSmallFileContentByOutputStreamWhenStreamIsNull()
            throws ScmException, IOException {
        ScmSession session = null;
        OutputStream os = null;
        boolean hasException = false;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
                    .is("SFName2");

            ScmCursor<ScmFileBasicInfo> fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());
            ScmFileBasicInfo fbi = null;
            if (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
            }
            fbiCursor.close();

            ScmFile file = ScmFactory.File.getInstance(ws, fbi.getFileId());
            file.getContent(os);

            Assert.assertNotNull(fbi);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_ALREADY_EXISTS);
            hasException = true;
        }
        finally {
            if (null != os) {
                os.close();
            }
            session.close();
        }

        Assert.assertEquals(hasException, true);
    }
}
