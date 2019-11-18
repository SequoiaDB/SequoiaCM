
package com.sequoiacm.net.auth.serial;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @Description:SCM-1723:有单个工作区权限(Update)，对“当前版本权限列表.xlsx”中的各个接口进行覆盖测试,
 * @author fanyu
 * @Date:2018年6月6日
 * @version:1.0
 */
public class AuthWs_UpdateRead1723 extends TestScmBase {
    // private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private ScmSession sessionA1;
    private ScmSession sessionUR;
    private ScmWorkspace wsA;
    private ScmWorkspace wsA1;
    private ScmWorkspace wsUR;
    private String author = "AuthWs_UpdateRead1723";
    private String username = "AuthWs_Update1723";
    private String rolename = "1723_UR";
    private String passwd = "1723";
    private ScmUser user;
    private ScmRole role;
    private ScmResource rs;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;
    private List<ScmId> fileIdList = new ArrayList<ScmId>();
    private List<ScmId> taskIdList = new ArrayList<ScmId>();

    private SiteWrapper branchsite;
    private SiteWrapper rootsite;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
            filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
            TestTools.LocalFile.removeFile(localPath);
            TestTools.LocalFile.createDir(localPath.toString());
            TestTools.LocalFile.createFile(filePath, fileSize);
            wsp = ScmInfo.getWs();
            List<SiteWrapper> sites = ScmNetUtils.getSortSites(wsp);
            rootsite = sites.get(1);
            branchsite = sites.get(0);
            sessionA = TestScmTools.createSession(branchsite);
            sessionA1 = TestScmTools.createSession(rootsite);
            wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
            wsA1 = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA1);
            cleanEnv();
            prepare();
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test(groups = {"twoSite", "fourSite"})
    private void test() throws Exception {
        testUpdateBatch();
        testBatchAttachFile();
        testBatchDetachFile();
        testUpdateFile();
        testAsynCaheFile();
        testAsynCaheFileByVersion();
        testAsynTransfer();
        testAsynTransferByVersion();
        testUpdateSche();
        testCleanTask();
        testCleanTaskByScope();
        testTransferTask();
        testTransferTaskByScope();
        testTransferTaskByTarget();
    }

    private void testUpdateBatch() throws ScmException {
        String batchName = "AuthWs_Update1723";
        String newbatchName = "AuthWs_Update1723_new";
        ScmId batchId = null;
        try {
            ScmBatch expBatch = ScmFactory.Batch.createInstance(wsA);
            expBatch.setName(batchName);
            batchId = expBatch.save();
            ScmBatch actBatch = ScmFactory.Batch.getInstance(wsUR, expBatch.getId());
            // update
            actBatch.setName(newbatchName);

            ScmBatch finaBatch = ScmFactory.Batch.getInstance(wsUR, expBatch.getId());
            Assert.assertEquals(actBatch.getName(), finaBatch.getName());
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (batchId != null) {
                ScmFactory.Batch.deleteInstance(wsA, batchId);
            }
        }
    }

    private void testBatchAttachFile() throws ScmException {
        String batchName = "AuthWs_Update1723" + "_" + UUID.randomUUID();
        String fileName = "AuthWs_Update1723_file" + "_" + UUID.randomUUID();
        ScmId batchId = null;
        ScmId fileId = null;
        try {
            ScmFile file = ScmFactory.File.createInstance(wsA);
            file.setFileName(fileName);
            fileId = file.save();
            ScmBatch expBatch = ScmFactory.Batch.createInstance(wsA);
            expBatch.setName(batchName);
            batchId = expBatch.save();

            ScmBatch expBatch1 = ScmFactory.Batch.getInstance(wsUR, batchId);
            expBatch1.attachFile(fileId);

            ScmBatch actBatch = ScmFactory.Batch.getInstance(wsA, expBatch.getId());
            Assert.assertEquals(actBatch.getName(), expBatch1.getName());
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (batchId != null) {
                ScmFactory.Batch.deleteInstance(wsA, batchId);
            }
        }
    }

    private void testBatchDetachFile() throws ScmException {
        String batchName = "AuthWs_Update1723" + "_" + UUID.randomUUID();
        String fileName = "AuthWs_Update1723_file" + "_" + UUID.randomUUID();
        ScmId batchId = null;
        ScmId fileId = null;
        try {
            ScmFile file = ScmFactory.File.createInstance(wsA);
            file.setFileName(fileName);
            fileId = file.save();
            ScmBatch expBatch = ScmFactory.Batch.createInstance(wsA);
            expBatch.setName(batchName);
            batchId = expBatch.save();
            expBatch.attachFile(fileId);

            ScmBatch expBatch1 = ScmFactory.Batch.getInstance(wsUR, batchId);
            expBatch1.detachFile(fileId);

            ScmBatch actBatch = ScmFactory.Batch.getInstance(wsA, expBatch.getId());
            Assert.assertEquals(actBatch.getName(), expBatch1.getName());
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (batchId != null) {
                ScmFactory.Batch.deleteInstance(wsA, batchId);
            }
            if (fileId != null) {
                ScmFactory.File.deleteInstance(wsA, fileId, true);
            }
        }
    }

    private void testUpdateFile() throws ScmException {
        String fileName = "AuthWs_Update1723_file" + "_" + UUID.randomUUID();
        String newfileName = "AuthWs_Update1723_file_new" + "_" + UUID.randomUUID();
        ScmId fileId = null;
        try {
            ScmFile expfile = ScmFactory.File.createInstance(wsA);
            expfile.setFileName(fileName);
            fileId = expfile.save();

            ScmFile actfile = ScmFactory.File.getInstance(wsUR, fileId);
            actfile.setFileName(newfileName);

            ScmFile expFile1 = ScmFactory.File.getInstance(wsUR, fileId);
            Assert.assertEquals(actfile.getFileName(), expFile1.getFileName());
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (fileId != null) {
                ScmFactory.File.deleteInstance(wsA, fileId, true);
            }
        }
    }

    private void testAsynCaheFile() throws ScmException {
        String fileName = "AuthWs_Update1723_file" + "_" + UUID.randomUUID();
        SiteWrapper rootSite = ScmInfo.getRootSite();
        ScmSession session = null;
        ScmId fileId = null;
        try {
            session = TestScmTools.createSession(rootSite);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            ScmFile expfile = ScmFactory.File.createInstance(ws);
            expfile.setFileName(fileName);
            fileId = expfile.save();
            ScmFactory.File.asyncCache(wsUR, fileId);
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (fileId != null) {
                ScmFactory.File.deleteInstance(wsA, fileId, true);
            }
            if (session != null) {
                session.close();
            }
        }
    }

    private void testAsynCaheFileByVersion() throws ScmException {
        String fileName = "AuthWs_Update1723_file" + "_" + UUID.randomUUID();
        SiteWrapper rootSite = ScmInfo.getRootSite();
        ScmSession session = null;
        ScmId fileId = null;
        try {
            session = TestScmTools.createSession(rootSite);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            ScmFile expfile = ScmFactory.File.createInstance(ws);
            expfile.setFileName(fileName);
            fileId = expfile.save();

            ScmFactory.File.asyncCache(wsUR, fileId, 1, 0);
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (fileId != null) {
                ScmFactory.File.deleteInstance(wsA, fileId, true);
            }
            if (session != null) {
                session.close();
            }
        }
    }

    private void testAsynTransfer() throws ScmException {
        String fileName = "AuthWs_Update1723_file" + "_" + UUID.randomUUID();
        ScmSession session = null;
        ScmId fileId = null;
        try {
            session = TestScmTools.createSession(branchsite);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            ScmFile expfile = ScmFactory.File.createInstance(ws);
            expfile.setFileName(fileName);
            fileId = expfile.save();

            ScmFactory.File.asyncTransfer(wsUR, fileId);
            ;
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (fileId != null) {
                ScmFactory.File.deleteInstance(wsA, fileId, true);
            }
            if (session != null) {
                session.close();
            }
        }
    }

    private void testAsynTransferByVersion() throws ScmException {
        String fileName = "AuthWs_Update1723_file" + "_" + UUID.randomUUID();
        ScmSession session = null;
        ScmId fileId = null;
        try {
            session = TestScmTools.createSession(branchsite);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            ScmFile expfile = ScmFactory.File.createInstance(ws);
            expfile.setFileName(fileName);
            fileId = expfile.save();

            ScmFactory.File.asyncTransfer(wsUR, fileId, 1, 0);
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (fileId != null) {
                ScmFactory.File.deleteInstance(wsA, fileId, true);
            }
            if (session != null) {
                session.close();
            }
        }
    }

    private void testUpdateSche() throws Exception {
        String scheName = "AuthWs_Read1723" + "_" + UUID.randomUUID();
        // SiteWrapper rootSite = ScmInfo.getRootSite();
        // SiteWrapper branchSite = ScmInfo.getBranchSite();
        String maxStayTime = "0d";
        BSONObject queryCond = null;
        ScmSchedule expSche = null;
        try {
            queryCond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(scheName).get();
            ScmScheduleContent content = new ScmScheduleCopyFileContent(branchsite.getSiteName(),
                    rootsite.getSiteName(), maxStayTime, queryCond);
            String crond = "* * * * * ? 2029";
            expSche = ScmSystem.Schedule.create(sessionA, wsp.getName(), ScheduleType.COPY_FILE, scheName, null,
                    content, crond);

            ScmSchedule actSche = ScmSystem.Schedule.get(sessionUR, expSche.getId());

            actSche.updateContent(content);
            actSche.updateCron(crond);
            actSche.updateDesc("");
            actSche.updateName(scheName);
            Assert.assertEquals(actSche.getName(), expSche.getName());
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (expSche != null) {
                ScmSystem.Schedule.delete(sessionA, expSche.getId());
            }
        }
    }

    private void grantPriAndAttachRole(ScmSession session, ScmResource rs, ScmUser user, ScmRole role,
                                       ScmPrivilegeType privileges) {
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            ScmFactory.Role.grantPrivilege(sessionA, role, rs, privileges);
            modifier.addRole(role);
            ScmFactory.User.alterUser(sessionA, user, modifier);
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private void testCleanTask() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId;
        ScmTask task = null;
        try {
            fileId = ScmFileUtils.create(wsA1, fileName, filePath);
            fileIdList.add(fileId);

            ScmFile file = ScmFactory.File.getInstance(wsA1, fileId);
            String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId());
            file.getContent(downloadPath);
            BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
            ScmId taskId = ScmSystem.Task.startCleanTask(wsUR, condition);
            taskIdList.add(taskId);

            ScmSystem.Task.stopTask(sessionUR, taskId);
            task = ScmSystem.Task.getTask(sessionUR, taskId);
            Assert.assertEquals(task.getWorkspaceName(), wsp.getName());
        } catch (AssertionError e) {
            throw new Exception("testCleanTask task = " + task.toString(), e);
        }
    }

    private void testCleanTaskByScope() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId;
        ScmId taskId = null;
        try {
            fileId = ScmFileUtils.create(wsA, fileName, filePath);
            fileIdList.add(fileId);
            ScmFile file = ScmFactory.File.getInstance(wsA1, fileId);
            String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId());
            file.getContent(downloadPath);
            BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
            taskId = ScmSystem.Task.startCleanTask(wsUR, condition, ScopeType.SCOPE_CURRENT);
            taskIdList.add(taskId);

            ScmTaskUtils.waitTaskFinish(sessionA, taskId);
            SiteWrapper[] siteArr = {rootsite};
            ScmFileUtils.checkMetaAndData(wsp, fileId, siteArr, localPath, filePath);
        } catch (AssertionError e) {
            throw new Exception("testCleanTaskByScope taskId = " + taskId, e);
        }
    }

    private void testTransferTask() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId;
        ScmId taskId = null;
        try {
            fileId = ScmFileUtils.create(wsA, fileName, filePath);
            fileIdList.add(fileId);

            BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
            taskId = ScmSystem.Task.startTransferTask(wsUR, condition, ScopeType.SCOPE_CURRENT,
                    rootsite.getSiteName());
            taskIdList.add(taskId);

            ScmTaskUtils.waitTaskFinish(sessionA, taskId);
            SiteWrapper[] siteArr = {rootsite, branchsite};
            ScmFileUtils.checkMetaAndData(wsp, fileId, siteArr, localPath, filePath);
        } catch (AssertionError e) {
            throw new Exception("testTransferTask taskId = " + taskId, e);
        }
    }

    private void testTransferTaskByScope() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId;
        ScmId taskId = null;
        try {
            fileId = ScmFileUtils.create(wsA, fileName, filePath);
            fileIdList.add(fileId);

            BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
            taskId = ScmSystem.Task.startTransferTask(wsUR, condition, ScopeType.SCOPE_CURRENT,
                    rootsite.getSiteName());
            taskIdList.add(taskId);

            ScmTaskUtils.waitTaskFinish(sessionA, taskId);
            SiteWrapper[] siteArr = {rootsite, branchsite};
            ScmFileUtils.checkMetaAndData(wsp, fileId, siteArr, localPath, filePath);
        } catch (AssertionError e) {
            throw new Exception("testTransferTaskByScope taskId = " + taskId, e);
        }
    }

    private void testTransferTaskByTarget() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId;
        ScmId taskId = null;
        try {
            fileId = ScmFileUtils.create(wsA, fileName, filePath);
            fileIdList.add(fileId);

            BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
            taskId = ScmSystem.Task.startTransferTask(wsUR, condition, ScopeType.SCOPE_CURRENT,
                    rootsite.getSiteName());
            taskIdList.add(taskId);

            ScmTaskUtils.waitTaskFinish(sessionA, taskId);
            SiteWrapper[] siteArr = {rootsite, branchsite};
            ScmFileUtils.checkMetaAndData(wsp, fileId, siteArr, localPath, filePath);
        } catch (AssertionError e) {
            throw new Exception("testTransferTaskByTarget taskId = " + taskId, e);
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege(sessionA, role, rs, ScmPrivilegeType.UPDATE);
            ScmFactory.Role.revokePrivilege(sessionA, role, rs, ScmPrivilegeType.READ);
            ScmFactory.Role.deleteRole(sessionA, role);
            ScmFactory.User.deleteUser(sessionA, user);
            for (ScmId fileId : fileIdList) {
                ScmFactory.File.deleteInstance(wsA, fileId, true);
            }
            for (ScmId taskId : taskIdList) {
                TestSdbTools.Task.deleteMeta(taskId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (sessionA != null) {
                sessionA.close();
            }
            if (sessionA1 != null) {
                sessionA1.close();
            }
        }
    }

    private void cleanEnv() {
        try {
            ScmFactory.Role.deleteRole(sessionA, rolename);
        } catch (ScmException e) {
            if (e.getError() != ScmError.HTTP_NOT_FOUND) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
        try {
            ScmFactory.User.deleteUser(sessionA, username);
        } catch (ScmException e) {
            if (e.getError() != ScmError.HTTP_NOT_FOUND) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
    }

    private void prepare() throws Exception {
        try {
            user = ScmFactory.User.createUser(sessionA, username, ScmUserPasswordType.LOCAL, passwd);
            role = ScmFactory.Role.createRole(sessionA, rolename, null);

            rs = ScmResourceFactory.createWorkspaceResource(wsp.getName());
            grantPriAndAttachRole(sessionA, rs, user, role, ScmPrivilegeType.UPDATE);
            grantPriAndAttachRole(sessionA, rs, user, role, ScmPrivilegeType.READ);

            ScmAuthUtils.checkPriority(branchsite, username, passwd, role, wsp);
            sessionUR = TestScmTools.createSession(branchsite, username, passwd);
            wsUR = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionUR);
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
}
