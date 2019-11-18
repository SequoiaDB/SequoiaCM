package com.sequoiacm.client.core.breakpointfile;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.zip.Checksum;

import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumFactory;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;

public class TestScmBreakpointFileUpload extends ScmTestBase {

    private ScmSession session;

    @BeforeClass
    public void setUpTestCase() throws ScmException {
        session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                new ScmConfigOption(url, user, password));

        ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(workspaceName, session);

        ScmCursor<ScmBreakpointFile> cursor1 = null;
        try {
            cursor1 = ScmFactory.BreakpointFile.listInstance(workspace);
            while (cursor1.hasNext()) {
                ScmBreakpointFile breakpointFile = cursor1.getNext();
                ScmFactory.BreakpointFile.deleteInstance(workspace, breakpointFile.getFileName());
            }
        }
        finally {
            if (cursor1 != null) {
                cursor1.close();
            }
        }

        ScmCursor<ScmFileBasicInfo> cursor2 = null;
        try {
            cursor2 = ScmFactory.File.listInstance(workspace, ScmType.ScopeType.SCOPE_CURRENT,
                    new BasicBSONObject());
            while (cursor2.hasNext()) {
                ScmFileBasicInfo file = cursor2.getNext();
                ScmFactory.File.deleteInstance(workspace, file.getFileId(), true);
            }
        }
        finally {
            if (cursor2 != null) {
                cursor2.close();
            }
        }
    }

    @AfterClass
    public void tearDownTestCase() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    public void testUpload() throws ScmException {
        String fileName = "testUpload";
        ScmChecksumType checksumType = ScmChecksumType.ADLER32;
        Checksum checksum = ScmChecksumFactory.getChecksum(ScmChecksumType.ADLER32);
        final int fileSize = 1024 * 1024;

        ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(workspaceName, session);

        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.createInstance(workspace,
                fileName, checksumType);

        // before upload
        assertEquals(breakpointFile.getWorkspace(), workspace);
        assertEquals(breakpointFile.getFileName(), fileName);
        assertEquals(breakpointFile.getChecksumType(), checksumType);
        assertEquals(breakpointFile.getChecksum(), 0L);
        assertEquals(breakpointFile.getUploadSize(), 0L);
        assertFalse(breakpointFile.isCompleted());
        assertEquals(breakpointFile.getCreateTime(), 0L);
        assertEquals(breakpointFile.getUploadTime(), 0L);
        assertNull(breakpointFile.getDataId());
        assertNull(breakpointFile.getSiteName());

        byte[] data = new byte[fileSize];
        new Random().nextBytes(data);

        checksum.update(data, 0, fileSize);
        breakpointFile.upload(new ByteArrayInputStream(data));

        // after upload
        assertEquals(breakpointFile.getWorkspace(), workspace);
        assertEquals(breakpointFile.getFileName(), fileName);
        assertEquals(breakpointFile.getChecksumType(), checksumType);
        assertEquals(breakpointFile.getChecksum(), checksum.getValue());
        assertEquals(breakpointFile.getUploadSize(), fileSize);
        assertTrue(breakpointFile.isCompleted());
        assertNotEquals(breakpointFile.getCreateTime().getTime(), 0L);
        assertNotEquals(breakpointFile.getUploadTime().getTime(), 0L);
        assertTrue(breakpointFile.getUploadTime().getTime() >= breakpointFile.getCreateTime()
                .getTime());
        assertNotNull(breakpointFile.getDataId());
        assertNotNull(breakpointFile.getSiteName());

        ScmCursor<ScmBreakpointFile> cursor1 = null;
        try {
            cursor1 = ScmFactory.BreakpointFile.listInstance(workspace);
            assertTrue(cursor1.hasNext());
            ScmBreakpointFile file2 = cursor1.getNext();
            assertFalse(cursor1.hasNext());
            assertEquals(file2, breakpointFile);
        }
        finally {
            if (cursor1 != null) {
                cursor1.close();
            }
        }

        ScmFile file = ScmFactory.File.createInstance(workspace);
        file.setContent(breakpointFile);
        file.setFileName(breakpointFile.getFileName());
        file.save();

        ScmCursor<ScmBreakpointFile> cursor2 = null;
        try {
            cursor2 = ScmFactory.BreakpointFile.listInstance(workspace);
            assertFalse(cursor2.hasNext());
        }
        finally {
            if (cursor2 != null) {
                cursor2.close();
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent(outputStream);
        byte[] fileData = outputStream.toByteArray();
        assertEquals(fileData, data);

        ScmFactory.File.deleteInstance(workspace, file.getFileId(), true);
    }

    @Test
    public void testIncrementalUpload() throws ScmException {
        String fileName = "testIncrementalUpload";
        ScmChecksumType checksumType = ScmChecksumType.ADLER32;
        Checksum checksum = ScmChecksumFactory.getChecksum(ScmChecksumType.ADLER32);
        final int fileSize = 1024 * 1024;

        ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(workspaceName, session);

        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.createInstance(workspace,
                fileName, checksumType);

        // before upload
        assertEquals(breakpointFile.getWorkspace(), workspace);
        assertEquals(breakpointFile.getFileName(), fileName);
        assertEquals(breakpointFile.getChecksumType(), checksumType);
        assertEquals(breakpointFile.getChecksum(), 0L);
        assertEquals(breakpointFile.getUploadSize(), 0L);
        assertFalse(breakpointFile.isCompleted());
        assertEquals(breakpointFile.getCreateTime(), 0L);
        assertEquals(breakpointFile.getUploadTime(), 0L);
        assertNull(breakpointFile.getDataId());
        assertNull(breakpointFile.getSiteName());

        byte[] data = new byte[fileSize];
        new Random().nextBytes(data);

        checksum.update(data, 0, fileSize / 2);
        breakpointFile.incrementalUpload(new ByteArrayInputStream(data, 0, fileSize / 2), false);

        // after upload
        assertEquals(breakpointFile.getWorkspace(), workspace);
        assertEquals(breakpointFile.getFileName(), fileName);
        assertEquals(breakpointFile.getChecksumType(), checksumType);
        assertEquals(breakpointFile.getChecksum(), checksum.getValue());
        assertEquals(breakpointFile.getUploadSize(), fileSize / 2);
        assertFalse(breakpointFile.isCompleted());
        assertNotEquals(breakpointFile.getCreateTime().getTime(), 0L);
        assertNotEquals(breakpointFile.getUploadTime().getTime(), 0L);
        assertTrue(breakpointFile.getUploadTime().getTime() >= breakpointFile.getCreateTime()
                .getTime());
        assertNotNull(breakpointFile.getDataId());
        assertNotNull(breakpointFile.getSiteName());

        // incremental upload
        checksum.reset();
        checksum.update(data, 0, fileSize);
        breakpointFile.incrementalUpload(new ByteArrayInputStream(data, fileSize / 2, fileSize),
                true);

        // after upload
        assertEquals(breakpointFile.getWorkspace(), workspace);
        assertEquals(breakpointFile.getFileName(), fileName);
        assertEquals(breakpointFile.getChecksumType(), checksumType);
        assertEquals(breakpointFile.getChecksum(), checksum.getValue());
        assertEquals(breakpointFile.getUploadSize(), fileSize);
        assertTrue(breakpointFile.isCompleted());
        assertNotEquals(breakpointFile.getCreateTime().getTime(), 0L);
        assertNotEquals(breakpointFile.getUploadTime().getTime(), 0L);
        assertTrue(breakpointFile.getUploadTime().getTime() >= breakpointFile.getCreateTime()
                .getTime());
        assertNotNull(breakpointFile.getDataId());
        assertNotNull(breakpointFile.getSiteName());

        ScmCursor<ScmBreakpointFile> cursor1 = null;
        try {
            cursor1 = ScmFactory.BreakpointFile.listInstance(workspace);
            assertTrue(cursor1.hasNext());
            ScmBreakpointFile file2 = cursor1.getNext();
            assertFalse(cursor1.hasNext());
            assertEquals(file2, breakpointFile);
        }
        finally {
            if (cursor1 != null) {
                cursor1.close();
            }
        }

        ScmFile file = ScmFactory.File.createInstance(workspace);
        file.setContent(breakpointFile);
        file.setFileName(breakpointFile.getFileName());
        file.save();

        ScmCursor<ScmBreakpointFile> cursor2 = null;
        try {
            cursor2 = ScmFactory.BreakpointFile.listInstance(workspace);
            assertFalse(cursor2.hasNext());
        }
        finally {
            if (cursor2 != null) {
                cursor2.close();
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent(outputStream);
        byte[] fileData = outputStream.toByteArray();
        assertEquals(fileData, data);

        ScmFactory.File.deleteInstance(workspace, file.getFileId(), true);
    }

    @Test
    public void testUploadEmptyFile() throws ScmException {
        String fileName = "testUploadEmptyFile";
        ScmChecksumType checksumType = ScmChecksumType.ADLER32;
        Checksum checksum = ScmChecksumFactory.getChecksum(ScmChecksumType.ADLER32);
        final int fileSize = 0;

        ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(workspaceName, session);

        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.createInstance(workspace,
                fileName, checksumType);

        // before upload
        assertEquals(breakpointFile.getWorkspace(), workspace);
        assertEquals(breakpointFile.getFileName(), fileName);
        assertEquals(breakpointFile.getChecksumType(), checksumType);
        assertEquals(breakpointFile.getChecksum(), 0L);
        assertEquals(breakpointFile.getUploadSize(), 0L);
        assertFalse(breakpointFile.isCompleted());
        assertEquals(breakpointFile.getCreateTime(), 0L);
        assertEquals(breakpointFile.getUploadTime(), 0L);
        assertNull(breakpointFile.getDataId());
        assertNull(breakpointFile.getSiteName());

        byte[] data = new byte[fileSize];
        new Random().nextBytes(data);

        checksum.update(data, 0, fileSize);
        breakpointFile.incrementalUpload(new ByteArrayInputStream(data), false);

        // after upload
        assertEquals(breakpointFile.getWorkspace(), workspace);
        assertEquals(breakpointFile.getFileName(), fileName);
        assertEquals(breakpointFile.getChecksumType(), checksumType);
        //assertEquals(breakpointFile.getChecksum(), checksum.getValue());
        assertEquals(breakpointFile.getUploadSize(), fileSize);
        assertFalse(breakpointFile.isCompleted());
        assertNotEquals(breakpointFile.getCreateTime(), 0L);
        //assertNotEquals(breakpointFile.getUploadTime(), 0L);
        //assertTrue(breakpointFile.getUploadTime() >= breakpointFile.getCreateTime());
        //assertNotNull(breakpointFile.getDataId());
        assertNotNull(breakpointFile.getSiteName());

        ScmCursor<ScmBreakpointFile> cursor1 = null;
        try {
            cursor1 = ScmFactory.BreakpointFile.listInstance(workspace);
            assertTrue(cursor1.hasNext());
            ScmBreakpointFile file2 = cursor1.getNext();
            assertFalse(cursor1.hasNext());
            assertEquals(file2, breakpointFile);
        }
        finally {
            if (cursor1 != null) {
                cursor1.close();
            }
        }

        breakpointFile.upload(new ByteArrayInputStream(data));

        ScmFile file = ScmFactory.File.createInstance(workspace);
        file.setContent(breakpointFile);
        file.setFileName(breakpointFile.getFileName());
        file.save();

        ScmCursor<ScmBreakpointFile> cursor2 = null;
        try {
            cursor2 = ScmFactory.BreakpointFile.listInstance(workspace);
            assertFalse(cursor2.hasNext());
        }
        finally {
            if (cursor2 != null) {
                cursor2.close();
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent(outputStream);
        byte[] fileData = outputStream.toByteArray();
        assertEquals(fileData, data);

        ScmFactory.File.deleteInstance(workspace, file.getFileId(), true);
    }
}
