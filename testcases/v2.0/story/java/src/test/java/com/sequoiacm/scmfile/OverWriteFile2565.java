package com.sequoiacm.scmfile;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

/**
 * @Description: SCM-2565:指定isOverwrite参数，断点文件转换/更新带有批次的同名scm文件
 * @author fanyu
 * @Date:2019年8月21日
 * @version:1.0
 */
public class OverWriteFile2565 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String dirPath = "/dir2565A/";
    private String fileName = "file2565A";
    private String batchName = "batch2565A";
    private ScmDirectory scmDirectory;
    private ScmId batchId;
    private ScmId fileId;
    private File localPath;
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
        List<SiteWrapper> sites =  ScmInfo.getAllSites();
        for(SiteWrapper tmpSite : sites){
            if(tmpSite.getDataType().equals(ScmType.DatasourceType.SEQUOIADB)){
                site = tmpSite;
                break;
            }
        }
        if(site == null){
            throw new SkipException("Upload BreakpointFile is not support in hdfs(hbase)");
        }
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession(site);
        ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
        BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.Batch.NAME).is(batchName).get();
        ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(ws,cond);
        while (cursor.hasNext()){
            ScmFactory.Batch.deleteInstance(ws,cursor.getNext().getId());
        }
        cursor.close();
        BSONObject cond1 = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
        ScmFileUtils.cleanFile(wsp,cond1);
        prepareFileWithBatch();
    }

    @Test
    private void test() throws Exception {
        //create breakpointFile
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.createInstance(ws, fileName);
        InputStream inputStream = new FileInputStream(updateFilePath);
        breakpointFile.upload( inputStream );
        inputStream.close();

        //overwrite: null false true
        ScmFile scmFile = ScmFactory.File.createInstance(ws);
        scmFile.setFileName(fileName);
        scmFile.setDirectory(scmDirectory);
        scmFile.setContent(breakpointFile);
        //ScmUploadConf is null
        try {
            scmFile.save(null);
            Assert.fail("exp fail but act success");
        } catch (ScmException e) {
            if (e.getError() != ScmError.FILE_EXIST) {
                throw e;
            }
        }
        //overwrite is false
        try {
            scmFile.save(new ScmUploadConf(false));
            Assert.fail("exp fail but act success");
        } catch (ScmException e) {
            if (e.getError() != ScmError.FILE_EXIST) {
                throw e;
            }
        }
        //overwrite is true
        String newVal = fileName + "-new";
        scmFile.setAuthor(newVal);
        scmFile.setTitle(newVal);
        ScmTags scmTags = new ScmTags();
        scmTags.addTag(newVal);
        scmFile.setTags(scmTags);
        fileId = scmFile.save(new ScmUploadConf(true));
        //get scm file by path and check
        ScmFile actFile = ScmFactory.File.getInstanceByPath(ws, dirPath + fileName);
        checkFile(actFile, newVal, fileSize + 1, updateFilePath, scmTags);
        //get batch and check 覆盖后的文件与批次关系解除
        ScmBatch scmBatch = ScmFactory.Batch.getInstance(ws,batchId);
        Assert.assertEquals(scmBatch.listFiles().size(),0,scmBatch.toString());
        //delete file
        ScmFactory.File.deleteInstance(ws,fileId,true);
        //delete directory
        ScmFactory.Directory.deleteInstance(ws,dirPath);
        //delete batch
        ScmFactory.Batch.deleteInstance(ws,batchId);
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if (runSuccess || TestScmBase.forceClear) {
                TestTools.LocalFile.removeFile(localPath);
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private void prepareFileWithBatch() throws ScmException {
        scmDirectory = ScmFactory.Directory.createInstance(ws,dirPath);
        //create tags
        ScmTags scmTags = new ScmTags();
        scmTags.addTag(fileName);
        //create file
        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setFileName(fileName);
        file.setAuthor(fileName);
        file.setTitle(fileName);
        file.setTags(scmTags);
        file.setDirectory(scmDirectory);
        file.setContent(filePath);
        fileId = file.save();
        //create batch and attach file
        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName(batchName);
        batch.setTags(scmTags);
        batchId = batch.save();
        batch.attachFile(fileId);
    }

    private void checkFile(ScmFile file, String expVal, int expSize, String expFilePath, ScmTags expScmTags) throws Exception {
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
            Assert.assertEquals(file.getDirectory().getPath(),dirPath);
            Assert.assertEquals(file.getTags().toSet().toString(), expScmTags.toSet().toString());
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
