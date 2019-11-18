package com.sequoiacm.client.core.file;

import com.sequoiacm.exception.ScmError;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;
import com.sequoiacm.client.util.ScmTestTools;

public class TestScmFileUpdate extends ScmTestBase {

    @BeforeClass
    public void setUp(){
    }

    @AfterClass
    public void tearDown(){
    }

    /*
    @Test
    public void testUpdatePropertyType() {
        String testFuncName = "testUpdatePropertyType";
        ScmFile savedFile = null;
        ScmSession ss = null;
        try {
            ss = ScmTestTools.createSession(url, user, password);
            savedFile = ScmTestTools.createFile(ss, workspaceName, testFuncName,
                    testFuncName);

            //unsupportedField
            //savedFile.setPropertyType(PropertyType.VIDEO);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.OPERATION_UNSUPPORTED,
                    e.getMessage());
        }
        finally {
            ScmTestTools.removeFileIfExist(sdbUrl, sdbUser, sdbPasswd, workspaceName, savedFile);
            ScmTestTools.releaseSession(ss);
        }
    }
    */
    
    /*@Test
    public void testUpdatePropertyType() {
        String testFuncName = "testUpdatePropertyType";
        ScmFile savedFile = null;
        ScmSession ss = null;
        try {
            ss = ScmTestTools.createSession(host, port, user, password);
            savedFile = ScmTestTools.createFile(ss, workspaceName, testFuncName,
                    testFuncName);

            //unsupportedField
            savedFile.setPropertyType(PropertyType.VIDEO);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getErrorCode(), ScmErrorCode.SCM_COMMON_OPERATION_UNSUPPORTED,
                    e.getMessage());
        }
        finally {
            ScmTestTools.removeFileIfExist(sdbUrl, sdbUser, sdbPasswd, workspaceName, savedFile);
            ScmTestTools.releaseSession(ss);
        }
    }*/

    @Test
    public void testUpdateFile1() {
        String testFuncName = "testUpdateFile1";
        ScmFile savedFile = null;
        ScmSession ss = null;
        try {
            ss = ScmTestTools.createSession(url, user, password);
            savedFile = ScmTestTools.createFile(ss, workspaceName, testFuncName,
                    testFuncName);

            String newName = "newName";
            savedFile.setFileName(newName);
            checkName(ss, savedFile.getFileId(), newName);

            String newTitle = "newTitle";
            savedFile.setTitle(newTitle);
            checkTitle(ss, savedFile.getFileId(), newTitle);

            String newAuthor = "newAuthor";
            savedFile.setAuthor(newAuthor);
            checkAuthor(ss, savedFile.getFileId(), newAuthor);

            checkUpdateUser(ss, savedFile.getFileId(), user);
        }
        catch (ScmException e) {
            e.printStackTrace();
            Assert.assertTrue(false, e.toString());
        }
        finally {
            ScmTestTools.removeFileIfExist(sdbUrl, sdbUser, sdbPasswd, workspaceName, savedFile);
            ScmTestTools.releaseSession(ss);
        }
    }

    private void checkTitle(ScmSession ss, ScmId fileId, String newTitle) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, ss);
        ScmFile file = ScmFactory.File.getInstance(ws, fileId);
        Assert.assertEquals(file.getTitle(), newTitle, file.getTitle());
        Assert.assertNotEquals(file.getCreateTime(), file.getUpdateTime(),
                "createTime=" + file.getCreateTime() + ",updateTime" + file.getUpdateTime());
    }

    private void checkName(ScmSession ss, ScmId fileId, String name) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, ss);
        ScmFile file = ScmFactory.File.getInstance(ws, fileId);
        Assert.assertEquals(file.getFileName(), name, file.getFileName());
        Assert.assertTrue(file.getCreateTime().getTime() <= file.getUpdateTime().getTime(),
                "createTime=" + file.getCreateTime() + ",updateTime" + file.getUpdateTime());
    }

    private void checkAuthor(ScmSession ss, ScmId fileId, String name) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, ss);
        ScmFile file = ScmFactory.File.getInstance(ws, fileId);
        Assert.assertEquals(file.getAuthor(), name, file.getAuthor());
        Assert.assertNotEquals(file.getCreateTime(), file.getUpdateTime(),
                "createTime=" + file.getCreateTime() + ",updateTime" + file.getUpdateTime());
    }

    private void checkUpdateUser(ScmSession ss, ScmId fileId, String name) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, ss);
        ScmFile file = ScmFactory.File.getInstance(ws, fileId);
        Assert.assertEquals(file.getUpdateUser(), name, file.getUpdateUser());
        Assert.assertNotEquals(file.getCreateTime(), file.getUpdateTime(),
                "createTime=" + file.getCreateTime() + ",updateTime" + file.getUpdateTime());
    }
}
