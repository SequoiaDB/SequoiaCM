package com.sequoiacm.scmfile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @Description: SCM-2674:存在带有批次的scm文件，使用流覆盖文件
 * @author fanyu
 * @Date:2019年10月24日
 * @version:1.0
 */
public class OverWriteFile2674 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String batchName = "batch2674";
    private ScmId batchId;
    private Random random = new Random();
    private int fileNum = 10;
    private String fileNameBase = "file2674";
    private List<ScmId> fileIdList = new ArrayList<ScmId>();
    private File localPath;
    private List<String> filePathList = new ArrayList<String>();
    private List<String> updateFilePathList = new ArrayList<String>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());

        TestTools.LocalFile.removeFile(localPath);
        TestTools.LocalFile.createDir(localPath.toString());
        for(int i = 0; i < fileNum; i++){
            int fileSize = i * random.nextInt(1024) * 1024;
            int updateFileSize = i * random.nextInt(1024) * 1024;
            String filePath = localPath + File.separator + "localFile_C" + i + ".txt";
            String updateFilePath = localPath + File.separator + "localFile_U" + i + ".txt";
            TestTools.LocalFile.createFile(filePath, fileSize);
            TestTools.LocalFile.createFile(updateFilePath, updateFileSize);
            filePathList.add(filePath);
            updateFilePathList.add(updateFilePath);
        }
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession(site);
        ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
        prepareFileWithBatch();
    }

    @Test
    private void test() throws Exception {
        for(int i = 0; i < fileNum; i++) {
            String fileName = fileNameBase + "-" + i;
            ScmFile scmFile = ScmFactory.File.createInstance(ws);
            scmFile.setFileName(fileName);
            //overwrite is true
            String newVal = fileName + "-new";
            scmFile.setAuthor(newVal);
            scmFile.setTitle(newVal);
            ScmTags scmTags = new ScmTags();
            scmTags.addTag(newVal);
            scmFile.setTags(scmTags);
            scmFile.setContent(new FileInputStream(new File(updateFilePathList.get(i))));
            ScmId fileId = scmFile.save(new ScmUploadConf(true));
            fileIdList.add(fileId);
            //get scm file and check
            ScmFile actFile = ScmFactory.File.getInstance(ws, fileId);
            checkFile(actFile, fileId, fileName, newVal, (int) new File(updateFilePathList.get(i)).length(),
                    updateFilePathList.get(i), scmTags);
        }
        //get batch and check
        ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
        Assert.assertEquals(batch.listFiles().size(), 0, batch.toString());
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if (runSuccess || TestScmBase.forceClear) {
                ScmFactory.Batch.deleteInstance(ws, batchId);
                for(ScmId fileId : fileIdList) {
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

    private void prepareFileWithBatch() throws ScmException {
        BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(fileNameBase).get();
        ScmFileUtils.cleanFile(wsp, cond);
        BSONObject cond1 = ScmQueryBuilder.start(ScmAttributeName.Batch.NAME).is(batchName).get();
        ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(ws, cond1);
        while (cursor.hasNext()) {
            ScmFactory.Batch.deleteInstance(ws, cursor.getNext().getId());
        }
        cursor.close();
        for (int i = 0; i < fileNum; i++) {
            String fileName = fileNameBase + "-" + i;
            //create tags
            ScmTags scmTags = new ScmTags();
            scmTags.addTag(fileName);
            //create file
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName(fileName);
            file.setAuthor(fileNameBase);
            file.setTitle(fileName);
            file.setTags(scmTags);
            file.setContent(filePathList.get(i));
            ScmId fileId = file.save();
            //create batch and attach file
            ScmBatch batch = ScmFactory.Batch.createInstance(ws);
            batch.setName(batchName);
            batch.setTags(scmTags);
            batchId = batch.save();
            batch.attachFile(fileId);
        }
    }

    private void checkFile(ScmFile file,ScmId fileId,String fileName,String expVal, int expSize, String expFilePath, ScmTags expScmTags) throws Exception {
        try {
            Assert.assertEquals(file.getWorkspaceName(), wsp.getName());
            Assert.assertEquals(file.getFileId(), fileId);
            Assert.assertEquals(file.getFileName(), fileName);
            Assert.assertEquals(file.getAuthor(), expVal);
            Assert.assertEquals(file.getTitle(), expVal);
            Assert.assertEquals(file.getMimeType(), "");
            Assert.assertEquals(file.getSize(), expSize);
            Assert.assertEquals(file.getMinorVersion(), 0);
            Assert.assertEquals(file.getMajorVersion(), 1);
            Assert.assertEquals(file.getTags().toSet().toString(), expScmTags.toSet().toString());
            Assert.assertEquals(file.getUser(), TestScmBase.scmUserName);
            Assert.assertNotNull(file.getCreateTime().getTime());
            Assert.assertNull(file.getBatchId());
            String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId());
            file.getContent(downloadPath);
            // check content
            Assert.assertEquals(TestTools.getMD5(downloadPath), TestTools.getMD5(expFilePath));
        } catch (AssertionError e) {
            throw new Exception("fileName = " + file.getFileName() + "fileId = " + fileId.get(), e);
        }
    }
}
