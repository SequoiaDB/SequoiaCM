
package com.sequoiacm.directory.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-1169 :: 在同一文件夹下大并发的创建文件夹和文件
 * @author fanyu
 * @Date:2018年5月3日
 * @version:1.0
 */
public class CreateDirAndFile1169 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/CreateDirAndFile1169";
    private String fullPath1 = dirBasePath;
    private List<String> pathList = new CopyOnWriteArrayList<String>();
    private List<ScmId> fileIdList = new CopyOnWriteArrayList<ScmId>();
    private int fileSize = 1024 * 1;
    private String author = "CreateDirAndFile1169";
    private File localPath;
    private String filePath;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
        filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            TestTools.LocalFile.removeFile(localPath);
            TestTools.LocalFile.createDir(localPath.toString());
            TestTools.LocalFile.createFile(filePath, fileSize);
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession(site);
            ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
            ScmFileUtils.cleanFile(wsp, cond);
            deleteDir(ws, fullPath1);
            ScmFactory.Directory.createInstance(ws, fullPath1);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        int threadNum = 50;
        List<CreateFile> fThreadLList = new ArrayList<CreateFile>();
        List<CreateDir> dThreadList = new ArrayList<CreateDir>();
        for (int i = 0; i < threadNum; i++) {
            fThreadLList.add(new CreateFile());
            dThreadList.add(new CreateDir());
        }
        for (int i = 0; i < threadNum; i++) {
            fThreadLList.get(i).start();
            dThreadList.get(i).start();
            boolean dflag = fThreadLList.get(i).isSuccess();
            boolean cflag = dThreadList.get(i).isSuccess();
            Assert.assertEquals(dflag, true, fThreadLList.get(i).getErrorMsg());
            Assert.assertEquals(cflag, true, dThreadList.get(i).getErrorMsg());
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if (runSuccess || TestScmBase.forceClear) {
                for (ScmId fileId : fileIdList) {
                    ScmFactory.File.deleteInstance(ws, fileId, true);
                }
                for (String path : pathList) {
                    deleteDir(ws, path);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private class CreateFile extends TestThreadBase {
        @Override
        public void exec() {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession(site);
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
                ScmDirectory dir = ScmFactory.Directory.getInstance(ws, fullPath1);
                ScmFile file = ScmFactory.File.createInstance(ws);
                file.setContent(filePath);
                file.setFileName(author + "_" + UUID.randomUUID());
                file.setAuthor(author);
                file.setTitle(author);
                file.setMimeType(MimeType.HTML);
                file.setTitle(author);
                file.setDirectory(dir);
                ScmId fileId = file.save();
                checkFile(fileId, dir, ws);
                fileIdList.add(fileId);
            } catch (ScmException e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        }
    }

    private class CreateDir extends TestThreadBase {
        @Override
        public void exec() {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession(site);
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
                String path = fullPath1 + "/1169_" + UUID.randomUUID();
                ScmDirectory dir = ScmFactory.Directory.createInstance(ws, path);
                pathList.add(path);
                // check
                BSONObject expBSON = new BasicBSONObject();
                expBSON.put("name", dir.getName());
                expBSON.put("path", path + "/");
                expBSON.put("wsName", wsp.getName());
                expBSON.put("paName", dir.getParentDirectory().getName());
                checkDir(path, expBSON, ws);
            } catch (ScmException e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        }
    }

    private void checkDir(String path, BSONObject expBSON, ScmWorkspace ws) {
        try {
            ScmDirectory dir = ScmFactory.Directory.getInstance(ws, path);
            Assert.assertEquals(dir.getName(), expBSON.get("name"));
            Assert.assertEquals(dir.getPath(), expBSON.get("path"));
            Assert.assertEquals(dir.getUpdateUser(), TestScmBase.scmUserName);
            Assert.assertEquals(dir.getUser(), TestScmBase.scmUserName);
            Assert.assertEquals(dir.getWorkspaceName(), expBSON.get("wsName"));
            Assert.assertNotNull(dir.getCreateTime());
            Assert.assertNotNull(dir.getUpdateTime());
            Assert.assertEquals(dir.getParentDirectory().getName(), expBSON.get("paName"));
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private void checkFile(ScmId fileId, ScmDirectory dir, ScmWorkspace ws) {
        ScmFile file;
        try {
            file = ScmFactory.File.getInstance(ws, fileId);
            Assert.assertEquals(file.getWorkspaceName(), wsp.getName());
            Assert.assertEquals(file.getFileId(), fileId);
            Assert.assertNotNull(file.getFileName());
            Assert.assertEquals(file.getAuthor(), author);
            Assert.assertEquals(file.getTitle(), author);
            Assert.assertEquals(file.getSize(), fileSize);
            Assert.assertEquals(file.getMinorVersion(), 0);
            Assert.assertEquals(file.getMajorVersion(), 1);
            Assert.assertEquals(file.getUser(), TestScmBase.scmUserName);
            Assert.assertEquals(file.getUpdateUser(), TestScmBase.scmUserName);
            Assert.assertNotNull(file.getCreateTime().getTime());
            Assert.assertNotNull(file.getUpdateTime());
            Assert.assertEquals(file.getDirectory().getPath(), dir.getPath());

            // check results
            SiteWrapper[] expSites = { site };
            ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private void deleteDir(ScmWorkspace ws, String dirPath) {
        List<String> pathList = getSubPaths(dirPath);
        for (int i = pathList.size() - 1; i >= 0; i--) {
            try {
                ScmFactory.Directory.deleteInstance(ws, pathList.get(i));
            } catch (ScmException e) {
                if (e.getError() != ScmError.DIR_NOT_FOUND
                        && e.getError() != ScmError.DIR_NOT_EMPTY) {
                    e.printStackTrace();
                    Assert.fail(e.getMessage());
                }
            }
        }
    }

    private List<String> getSubPaths(String path) {
        String ele = "/";
        String[] arry = path.split("/");
        List<String> pathList = new ArrayList<String>();
        for (int i = 1; i < arry.length; i++) {
            ele = ele + arry[i];
            pathList.add(ele);
            ele = ele + "/";
        }
        return pathList;
    }
}
