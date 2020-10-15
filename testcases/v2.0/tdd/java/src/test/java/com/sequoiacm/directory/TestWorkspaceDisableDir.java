package com.sequoiacm.directory;

import java.io.ByteArrayInputStream;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestWorkspaceDisableDir extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private String wsName = TestWorkspaceDisableDir.class.getSimpleName();
    private ScmFile file;
    private ScmFile file2;

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
    }

    @Test
    public void test() throws ScmException, InterruptedException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setName(wsName);
        conf.setDescription(this.getClass().getName());
        conf.setMetaLocation(new ScmSdbMetaLocation("rootSite", ScmShardingType.MONTH, "domain1"));
        conf.addDataLocation(new ScmSdbDataLocation("rootSite", "domain2", ScmShardingType.YEAR,
                ScmShardingType.QUARTER));
        conf.setEnableDirectory(false);
        ScmWorkspace ws = ScmFactory.Workspace.createWorkspace(ss, conf);
        ScmRole role = ScmFactory.Role.getRole(ss, "ROLE_AUTH_ADMIN");
        ScmFactory.Role.grantPrivilege(ss, role, ScmResourceFactory.createWorkspaceResource(wsName),
                ScmPrivilegeType.ALL);
        Thread.sleep(20000);

        fileTest(ws);
        dirTest(ws);
    }

    private void dirTest(ScmWorkspace ws) {
        try {
            ScmFactory.Directory.createInstance(ws, "/");
            Assert.fail();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.DIR_FEATURE_DISABLE);
        }

        try {
            ScmFactory.Directory.getInstance(ws, "/");
            Assert.fail();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.DIR_FEATURE_DISABLE);
        }

        try {
            ScmFactory.Directory.countInstance(ws, new BasicBSONObject());
            Assert.fail();
        }
        catch (ScmException e) {

            Assert.assertEquals(e.getError(), ScmError.DIR_FEATURE_DISABLE);
        }

        try {
            ScmFactory.Directory.deleteInstance(ws, "/");
            Assert.fail();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.DIR_FEATURE_DISABLE);
        }

        try {
            ScmFactory.Directory.listInstance(ws, new BasicBSONObject());
            Assert.fail();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.DIR_FEATURE_DISABLE);
        }

    }

    private void fileTest(ScmWorkspace ws) throws ScmException {
        // 创建文件不允许设置目录
        file = ScmFactory.File.createInstance(ws);
        file.setFileName(wsName);
        file.setDirectory("notexistDirId");
        try {
            file.save();
            Assert.fail();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.DIR_FEATURE_DISABLE);
        }
        file.setContent(new ByteArrayInputStream("testdata".getBytes()));
        file.setDirectory((String) null);
        file.save();
        try {
            file.setDirectory("notexistDirId");
            Assert.fail();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.DIR_FEATURE_DISABLE);
        }

        // 文件无法获取目录
        try {
            file.getDirectory();
            Assert.fail();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.DIR_FEATURE_DISABLE);
        }

        // 文件可以更新其它属性
        file.setMimeType(MimeType.DOCX);
        file.setFileName(wsName + "2");
        file = ScmFactory.File.getInstance(ws, file.getFileId());
        Assert.assertEquals(file.getMimeTypeEnum(), MimeType.DOCX);
        Assert.assertEquals(file.getFileName(), wsName + "2");

        // 新版本
        file.updateContent(new ByteArrayInputStream("testdata".getBytes()));
        Assert.assertEquals(file.getMajorVersion(), 2);
        file = ScmFactory.File.getInstance(ws, file.getFileId());
        Assert.assertEquals(file.getMajorVersion(), 2);

        // 允许同名
        file2 = ScmFactory.File.createInstance(ws);
        file2.setFileName(wsName + "2");
        ScmBreakpointFile bf = ScmFactory.BreakpointFile.createInstance(ws, wsName);
        bf.upload(new ByteArrayInputStream("testdata".getBytes()));
        file2.setContent(bf);
        file2.save();
        // 新增版本断点文件
        bf = ScmFactory.BreakpointFile.createInstance(ws, wsName);
        bf.upload(new ByteArrayInputStream("testdata".getBytes()));
        file2.updateContent(bf);
        file2 = ScmFactory.File.getInstance(ws, file2.getFileId());
        Assert.assertEquals(file2.getMajorVersion(), 2);

        // 不允许按目录检索文件
        try {
            ScmFactory.File.getInstanceByPath(ws, "/" + file2.getFileName());
            Assert.fail();
        }
        catch (ScmException e) {

            Assert.assertEquals(e.getError(), ScmError.DIR_FEATURE_DISABLE);
        }

        // 列取文件
        ScmCursor<ScmFileBasicInfo> c = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(file2.getFileName())
                        .get());
        Assert.assertTrue(c.hasNext());
        ScmFileBasicInfo fileInfo = c.getNext();
        Assert.assertEquals(fileInfo.getFileId().equals(file.getFileId())
                || fileInfo.getFileId().equals(file2.getFileId()), true);
        Assert.assertTrue(c.hasNext());
        fileInfo = c.getNext();
        Assert.assertEquals(fileInfo.getFileId().equals(file.getFileId())
                || fileInfo.getFileId().equals(file2.getFileId()), true);
        c.close();

        // 删除文件
        file.delete(true);
        file2.delete(true);
        try {
            ScmFactory.File.getInstance(ws, file.getFileId());
            Assert.fail();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_NOT_FOUND);
        }
        try {
            ScmFactory.File.getInstance(ws, file2.getFileId());
            Assert.fail();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_NOT_FOUND);
        }
    }

    @AfterClass
    public void clear() throws ScmException {
        try {
            ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.WORKSPACE_NOT_EXIST) {
                throw e;
            }
        }
        ss.close();
    }
}
