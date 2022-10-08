package com.sequoiacm.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.common.CommonDefine;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * 测试驱动直接下载文件内容接口：
 * 1. 站点1创建两个版本的文件
 * 2. ScmFactory.File.getInputStream(ScmWorkspace ws, ScmId fileId) 下载最新版本，比对数据正确性
 * 3. 连接站点2，ScmFactory.File.getInputStream(ScmWorkspace ws, ScmId fileId, int majorVersion, minorVersion, int readFlag) 下载最新版本，并指定 SCM_READ_FILE_FORCE_NO_CACHE，
 * 比对数据正确性，同时检查文件站点列表，确认readFlag生效
 * 4. 使用ScmFile.getInputStream下载文件，检查数据正确
 */
public class TestFileInputStream extends ScmTestMultiCenterBase {

    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmSession ss2;
    private ScmWorkspace ws2;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ss2 = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        ws2 = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss2);
    }

    @Test
    public void getFile() throws ScmException, IOException {
        int sizeVersion1 = 200 * 1024;
        int sizeVersion2 = 300 * 1024;
        byte[] bytesVersion1 = new byte[sizeVersion1];
        byte[] bytesVersion2 = new byte[sizeVersion2];
        Random rd = new Random();
        rd.nextBytes(bytesVersion1);
        rd.nextBytes(bytesVersion2);

        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setFileName(UUID.randomUUID().toString());
        file.setContent(new ByteArrayInputStream(bytesVersion1));
        file.save();

        file.updateContent(new ByteArrayInputStream(bytesVersion2));

        InputStream isV2 = ScmFactory.File.getInputStream(ws, file.getFileId());
        ByteArrayOutputStream v2Server = new ByteArrayOutputStream();
        IOUtils.copy(isV2, v2Server);
        Assert.assertEquals(v2Server.toByteArray(), bytesVersion2);

        InputStream isV1 = ScmFactory.File.getInputStream(ws2, file.getFileId(), 1, 0,
                CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE);
        ByteArrayOutputStream v1Server = new ByteArrayOutputStream();
        IOUtils.copy(isV1, v1Server);
        Assert.assertEquals(v1Server.toByteArray(), bytesVersion1);

        file = ScmFactory.File.getInstance(ws2, file.getFileId(), 1, 0);
        Assert.assertEquals(file.getLocationList().size(), 1);

        isV1 = file.getInputStream();
        v1Server = new ByteArrayOutputStream();
        IOUtils.copy(isV1, v1Server);
        Assert.assertEquals(v1Server.toByteArray(), bytesVersion1);
        file = ScmFactory.File.getInstance(ws2, file.getFileId(), 1, 0);
        Assert.assertEquals(file.getLocationList().size(), 2);

        file.delete(true);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ss.close();
        ss2.close();
    }
}
