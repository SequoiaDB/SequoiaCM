package com.sequoiacm.scmfile.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Description: SCM-2569:并发覆盖上传文件和关联批次
 * @author fanyu
 * @Date:2019年8月22日
 * @version:1.0
 */
public class OverWriteFile2569 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String batchName = "batch2569A";
    private String nameBase = "file2569A";
    private List<ScmId> origFileIdList = new ArrayList<ScmId>();
    private List<ScmId> fileIdList = new CopyOnWriteArrayList<ScmId>();
    private ScmBatch batch;
    private ScmId batchId;
    private File localPath;
    private int fileNum = 10;
    private int fileSize = 1024 * new Random().nextInt(1024);
    private String filePath;
    private String updateFilePath;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
        filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
        updateFilePath = localPath + File.separator + "localFile_" + (fileSize + 1) + ".txt";
        TestTools.LocalFile.removeFile(localPath);
        TestTools.LocalFile.createDir(localPath.toString());
        TestTools.LocalFile.createFile(filePath, fileSize);
        TestTools.LocalFile.createFile(updateFilePath, fileSize + 1);
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession(site);
        ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
        BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.Batch.NAME).is(batchName).get();
        ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(ws, cond);
        while (cursor.hasNext()) {
            ScmFactory.Batch.deleteInstance(ws, cursor.getNext().getId());
        }
        cursor.close();
        //create batch  and prepare file
        batch = ScmFactory.Batch.createInstance(ws);
        batch.setName(batchName);
        batchId = batch.save();
        for (int i = 0; i < fileNum; i++) {
            origFileIdList.add(prepareFile(nameBase + "-" + i));
        }
    }

    @Test
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        for (int i = 0; i < fileNum; i++) {
            threadExec.addWorker(new OverWriteFile(nameBase + "-" + i));
            threadExec.addWorker(new AttachFile(origFileIdList.get(i)));
        }
        threadExec.run();
        //check result
        String newVal = nameBase + "-new";
        ScmTags newScmTags = new ScmTags();
        newScmTags.addTag(newVal);
        ScmTags oldScmTags = new ScmTags();
        oldScmTags.addTag(nameBase);
        for (int i = 0; i < fileNum; i++) {
            ScmFile actFile = ScmFactory.File.getInstanceByPath(ws, nameBase + "-" + i);
            if (actFile.getAuthor().equals(newVal)) {
                checkFile(actFile, newVal, fileSize + 1, updateFilePath, newScmTags);
                Assert.assertNull(actFile.getBatchId());
            } else {
                checkFile(actFile, nameBase, fileSize, filePath, oldScmTags);
                Assert.assertEquals(actFile.getBatchId(), batchId);
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if (runSuccess || TestScmBase.forceClear) {
                ScmFactory.Batch.deleteInstance(ws, batchId);
                for (ScmId fileId : fileIdList) {
                    ScmFactory.File.deleteInstance(ws, fileId, true);
                }
                TestTools.LocalFile.removeFile(localPath);
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private class OverWriteFile {
        private String fileName;
        private ScmSession session;
        private ScmWorkspace ws;
        private ScmFile scmFile;

        public OverWriteFile(String fileName) throws ScmException {
            this.fileName = fileName;
            this.session = TestScmTools.createSession(site);
            this.ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            this.scmFile = ScmFactory.File.createInstance(ws);
            this.scmFile.setFileName(this.fileName);
            this.scmFile.setContent(updateFilePath);
            //overwrite is true
            String newVal = nameBase + "-new";
            this.scmFile.setAuthor(newVal);
            this.scmFile.setTitle(newVal);
            ScmTags scmTags = new ScmTags();
            scmTags.addTag(newVal);
            this.scmFile.setTags(scmTags);
        }

        @ExecuteOrder(step = 1)
        private void overwriteScmFile() throws ScmException {
            try {
                ScmId fileId = this.scmFile.save(new ScmUploadConf(true));
                fileIdList.add(fileId);
            } catch (ScmException e) {
                if (e.getError() != ScmError.FILE_IN_ANOTHER_BATCH) {
                    throw e;
                }
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        }
    }

    private class AttachFile {
        private ScmId fileId;

        public AttachFile(ScmId fileId) {
            this.fileId = fileId;
        }

        @ExecuteOrder(step = 1)
        private void attachFile() throws ScmException {
            try {
                batch.attachFile(fileId);
            } catch (ScmException e) {
                System.out.println("fail fileId = " + fileId.get());
                if (e.getError() != ScmError.FILE_NOT_FOUND) {
                    throw e;
                }
            }
        }
    }

    private ScmId prepareFile(String fileName) throws ScmException {
        //create tags
        ScmTags scmTags = new ScmTags();
        scmTags.addTag(fileName);
        //create file
        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setFileName(fileName);
        file.setAuthor(fileName);
        file.setTitle(fileName);
        file.setTags(scmTags);
        file.setContent(filePath);
        return file.save();
    }

    private void checkFile(ScmFile file, String expVal, int expSize, String expFilePath, ScmTags expScmTags) throws Exception {
        try {
            Assert.assertEquals(file.getWorkspaceName(), wsp.getName());
            Assert.assertTrue(file.getAuthor().contains(expVal));
            Assert.assertTrue(file.getAuthor().contains(expVal));
            Assert.assertEquals(file.getMimeType(), "text/plain");
            Assert.assertEquals(file.getSize(), expSize);
            Assert.assertEquals(file.getMinorVersion(), 0);
            Assert.assertEquals(file.getMajorVersion(), 1);
            Assert.assertEquals(file.getTags().toSet().size(), expScmTags.toSet().size());
            Assert.assertEquals(file.getUser(), TestScmBase.scmUserName);
            Assert.assertNotNull(file.getCreateTime().getTime());
            String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId());
            file.getContent(downloadPath);
            // check content
            Assert.assertEquals(TestTools.getMD5(downloadPath), TestTools.getMD5(expFilePath));
        } catch (AssertionError e) {
            throw new Exception("file = " + file.toString(), e);
        }
    }
}

