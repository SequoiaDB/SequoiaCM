package com.sequoiacm.file;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * 
 * 上传同名文件覆盖
 *
 */
public class TestUploadSameFile extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private String dirPath;
    private String sameFileName;
    private String batchName;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        dirPath = getDataDirectory() + File.separator + ScmTestTools.getClassName();
    }

    private void createFile(String filePath, int fileSize) throws IOException {
        ScmTestTools.createFile(filePath, " ", fileSize);
    }

    @Test
    public void testSameUpload() throws ScmException, IOException {
        String filePath = dirPath + File.separator + "samefile_0.txt";
        createFile(filePath, 1024 * 1024);
        String filePath_1 = dirPath + File.separator + "samefile_1.txt";
        createFile(filePath_1, 2 * 1024 * 1024);
        String filePath_2 = dirPath + File.separator + "samefile_2.txt";
        createFile(filePath_2, 3 * 1024 * 1024);

        sameFileName = "sameFile" + System.currentTimeMillis();
        batchName = "batch" + System.currentTimeMillis();

        // upload file
        ScmFile scmfile = createScmFile(ws, filePath, sameFileName, "scm_user", "uploadfile");
        ScmBatch batch = createBatch(batchName);
        // generate file version
        scmfile.updateContent(filePath);
        scmfile.updateContent(filePath);
        batch.attachFile(scmfile.getFileId());
        checkUpload(scmfile.getFileId(), 1024 * 1024, true, true);

        // upload same file
        ScmFile scmfile1 = createScmFile(ws, filePath_1, sameFileName, "scm_user", "uploadfile");
        checkUpload(scmfile1.getFileId(), 2 * 1024 * 1024, false, false);
        scmfile1.updateContent(filePath_1);
        scmfile1.updateContent(filePath_1);
        batch.attachFile(scmfile1.getFileId());
        checkUpload(scmfile1.getFileId(), 2 * 1024 * 1024, true, true);

        // upload same break file
        ScmBreakpointFile breakFile = ScmFactory.BreakpointFile.createInstance(ws, sameFileName,
                ScmChecksumType.CRC32, 1024 * 1024);
        File uploadFile = new File(filePath_2);
        breakFile.upload(uploadFile);
        ScmFile scmfile2 = createScmFile(ws, filePath_2, sameFileName, "scm_user", "uploadfile");
        checkUpload(scmfile2.getFileId(), 3 * 1024 * 1024, false, false);
    }

    private void checkUpload(ScmId fileId, long fileSize, boolean isHistoryVersion,
            boolean isExistBatch) throws ScmException {

        BSONObject matchFileId = new BasicBSONObject(FieldName.FIELD_CLFILE_ID, fileId.get());
        ScmCursor<ScmFileBasicInfo> cursor = null;
        try {
            ScmFile queryOneFile = ScmFactory.File.getInstanceByPath(ws, sameFileName);
            cursor = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_HISTORY, matchFileId);
            assertEquals(true, queryOneFile != null);
            assertEquals(fileSize, queryOneFile.getSize());
            assertEquals(isHistoryVersion, cursor.hasNext());
            assertEquals(isExistBatch, queryOneFile.getBatchId() != null);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    private ScmBatch createBatch(String batchName) throws ScmException {
        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName(batchName);
        batch.save();
        return batch;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmFile file = ScmFactory.File.getInstanceByPath(ws, sameFileName);
        ScmCursor<ScmBatchInfo> batchCursor = ScmFactory.Batch.listInstance(ws,
                new BasicBSONObject(ScmAttributeName.Batch.NAME, batchName));
        assertEquals(true, batchCursor.hasNext());
        ScmFactory.Batch.deleteInstance(ws, batchCursor.getNext().getId());
        ScmFactory.File.deleteInstance(ws, file.getFileId(), true);
        ss.close();
    }

    private ScmFile createScmFile(ScmWorkspace ws, String filePath, String fileName, String author,
            String FileTitle) throws ScmException {

        ScmFile file = ScmFactory.File.createInstance(ws);
        if (null != fileName) {
            file.setFileName(fileName);
        }
        file.setTitle(FileTitle);
        file.setContent(filePath);
        file.save(new ScmUploadConf(true));
        return file;
    }
}
