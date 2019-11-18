
package com.sequoiacm.auth.serial;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.UUID;

/**
 * @author fanyu
 * @Description:SCM-1725:有工作区CREATE|READ的权限和目录的UPDATE|READ| DELETE权限，
 * 对表格中的各个接口进行覆盖测试
 * @Date:2018年6月6日
 * @version:1.0
 */
public class AuthWs_NoUpdateRead1725 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private ScmSession sessionUR;
    private ScmWorkspace wsA;
    private ScmWorkspace wsUR;
    private String dirpath = "/AuthWs_NoUpdateRead1725";
    private String username = "AuthWs_NoUpdateRead1725";
    private String rolename = "1725_NUR";
    private String passwd = "1725";
    private String author = "AuthWs_NoUpdateRead1725";
    private ScmUser user;
    private ScmRole role;
    private ScmResource wsrs;
    private ScmResource dirrs;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
            filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
            TestTools.LocalFile.removeFile(localPath);
            TestTools.LocalFile.createDir(localPath.toString());
            TestTools.LocalFile.createFile(filePath, fileSize);

            site = ScmInfo.getBranchSite();
            wsp = ScmInfo.getWs();
            sessionA = TestScmTools.createSession(site);
            wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
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
        testCancelCleanTask();
        testCancelTransferTask();
        testTransferTask();
        testTransferTaskByScope();
        testTransferTaskByTarget();
    }

    private void testUpdateBatch() throws ScmException {
        String batchName = author;
        String newbatchName = author + "_new";
        ScmId batchId = null;
        try {
            ScmBatch expBatch = ScmFactory.Batch.createInstance(wsA);
            expBatch.setName(batchName);
            batchId = expBatch.save();
            ScmBatch actBatch = ScmFactory.Batch.getInstance(wsUR, expBatch.getId());
            // update
            actBatch.setName(newbatchName);
            Assert.fail("the user have not privilege to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        } finally {
            if (batchId != null) {
                ScmFactory.Batch.deleteInstance(wsA, batchId);
            }
        }
    }

    private void testBatchAttachFile() throws ScmException {
        String batchName = author + "_" + UUID.randomUUID();
        String fileName = author + "_" + UUID.randomUUID();
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
            Assert.fail("the user have not privilege to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        } finally {
            if (batchId != null) {
                ScmFactory.Batch.deleteInstance(wsA, batchId);
            }
            if (fileId != null) {
                ScmFactory.File.deleteInstance(wsA, fileId, true);
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
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        } finally {
            if (batchId != null) {
                ScmFactory.Batch.deleteInstance(wsA, batchId);
            }
        }
    }

    private void testUpdateFile() throws ScmException {
        String fileName = author + "_" + UUID.randomUUID();
        String newfileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        try {
            ScmFile expfile = ScmFactory.File.createInstance(wsA);
            expfile.setFileName(fileName);
            fileId = expfile.save();

            ScmFile actfile = ScmFactory.File.getInstance(wsUR, fileId);
            actfile.setFileName(newfileName);
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
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
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
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
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
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
            session = TestScmTools.createSession(site);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            ScmFile expfile = ScmFactory.File.createInstance(ws);
            expfile.setFileName(fileName);
            fileId = expfile.save();

            ScmFactory.File.asyncTransfer(wsUR, fileId);
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
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
        String fileName = author + "_" + UUID.randomUUID();
        ScmSession session = null;
        ScmId fileId = null;
        try {
            session = TestScmTools.createSession(site);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            ScmFile expfile = ScmFactory.File.createInstance(ws);
            expfile.setFileName(fileName);
            fileId = expfile.save();
            ScmFactory.File.asyncTransfer(wsUR, fileId, 1, 0);
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
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
        SiteWrapper rootSite = ScmInfo.getRootSite();
        SiteWrapper branchSite = ScmInfo.getBranchSite();
        String maxStayTime = "0d";
        BSONObject queryCond = null;
        ScmSchedule expSche = null;
        try {
            queryCond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(scheName).get();
            ScmScheduleContent content = new ScmScheduleCopyFileContent(branchSite.getSiteName(),
                    rootSite.getSiteName(), maxStayTime, queryCond);
            String crond = "* * * * * ? 2029";
            expSche = ScmSystem.Schedule.create(sessionA, wsp.getName(), ScheduleType.COPY_FILE, scheName, null,
                    content, crond);

            ScmSchedule upSche = ScmSystem.Schedule.get(sessionUR, expSche.getId());
            upSche.updateDesc(author);
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.HTTP_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        } finally {
            if (expSche != null) {
                ScmSystem.Schedule.delete(sessionA, expSche.getId());
            }
        }
    }

    private void testCleanTask() throws Exception {
        try {
            BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is("").get();
            ScmSystem.Task.startCleanTask(wsUR, condition);
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
    }

    private void testCleanTaskByScope() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        try {
            BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
            ScmSystem.Task.startCleanTask(wsUR, condition, ScopeType.SCOPE_CURRENT);
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
    }

    private void testCancelCleanTask() throws Exception {
        ScmId taskId = null;
        try {
            BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is("").get();
            taskId = ScmSystem.Task.startCleanTask(wsA, condition);
            ScmSystem.Task.stopTask(sessionUR, taskId);
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        } finally {
            if (taskId != null) {
                TestSdbTools.Task.deleteMeta(taskId);
            }
        }
    }

    private void testCancelTransferTask() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId taskId = null;
        try {
            BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
            taskId = ScmSystem.Task.startTransferTask(wsA, condition);
            ScmSystem.Task.stopTask(sessionUR, taskId);
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        } finally {
            if (taskId != null) {
                TestSdbTools.Task.deleteMeta(taskId);
            }
        }
    }

    private void testTransferTask() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        try {
            BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
            ScmSystem.Task.startTransferTask(wsUR, condition);
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
    }

    private void testTransferTaskByScope() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        try {
            BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
            ScmSystem.Task.startTransferTask(wsUR, condition, ScopeType.SCOPE_CURRENT);
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
    }

    private void testTransferTaskByTarget() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        try {
            BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
            ScmSystem.Task.startTransferTask(wsUR, condition, ScopeType.SCOPE_CURRENT,
                    ScmInfo.getRootSite().getSiteName());
            Assert.fail("the user does not have priority to do someting");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
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

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege(sessionA, role, wsrs, ScmPrivilegeType.READ);
            ScmFactory.Role.revokePrivilege(sessionA, role, wsrs, ScmPrivilegeType.CREATE);

            ScmFactory.Role.revokePrivilege(sessionA, role, dirrs, ScmPrivilegeType.READ);
            ScmFactory.Role.revokePrivilege(sessionA, role, dirrs, ScmPrivilegeType.DELETE);
            ScmFactory.Role.revokePrivilege(sessionA, role, dirrs, ScmPrivilegeType.UPDATE);

            ScmFactory.Role.deleteRole(sessionA, role);
            ScmFactory.User.deleteUser(sessionA, user);
            ScmFactory.Directory.deleteInstance(wsA, dirpath);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (sessionA != null) {
                sessionA.close();
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

            ScmFactory.Directory.createInstance(wsA, dirpath);
            wsrs = ScmResourceFactory.createWorkspaceResource(wsp.getName());
            dirrs = ScmResourceFactory.createDirectoryResource(wsp.getName(), dirpath);
            grantPriAndAttachRole(sessionA, wsrs, user, role, ScmPrivilegeType.READ);
            grantPriAndAttachRole(sessionA, wsrs, user, role, ScmPrivilegeType.CREATE);

            grantPriAndAttachRole(sessionA, dirrs, user, role, ScmPrivilegeType.READ);
            grantPriAndAttachRole(sessionA, dirrs, user, role, ScmPrivilegeType.DELETE);
            grantPriAndAttachRole(sessionA, dirrs, user, role, ScmPrivilegeType.UPDATE);

            ScmAuthUtils.checkPriority(site, username, passwd, role, wsp);

            sessionUR = TestScmTools.createSession(site, username, passwd);
            wsUR = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionUR);
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
}
