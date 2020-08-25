package com.sequoiacm.md5;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.digest.DigestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestScmFileUpdateContentMd5 extends ScmTestMultiCenterBase {

    private ScmSession bSiteSs;
    private ScmWorkspace ws;

    @BeforeMethod
    @BeforeClass
    public void setUp() throws ScmException {
        bSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), bSiteSs);
    }

    @Test
    public void test() throws ScmException, IOException {
        ScmFile file = ScmFactory.File.createInstance(ws);
        Random rd = new Random();
        byte[] version1Data = new byte[1024 * 1024 * 4];
        rd.nextBytes(version1Data);
        String version1ExpectedMd5 = DatatypeConverter
                .printBase64Binary(DigestUtils.md5(version1Data));

        file.setFileName(
                TestScmFileUpdateContentMd5.class.getSimpleName() + UUID.randomUUID().toString());
        file.setContent(new ByteArrayInputStream(version1Data));
        file.save(new ScmUploadConf(false, true));
        String scmV1Md5 = file.getMd5();
        Assert.assertEquals(version1ExpectedMd5, scmV1Md5);

        byte[] version2Data = new byte[1024 * 1024 * 4];
        rd.nextBytes(version2Data);
        String version2ExpectedMd5 = DatatypeConverter
                .printBase64Binary(DigestUtils.md5(version2Data));
        file.updateContent(new ByteArrayInputStream(version2Data),
                new ScmUpdateContentOption(true));
        String scmV2Md5 = file.getMd5();
        Assert.assertEquals(version2ExpectedMd5, scmV2Md5);

        ScmFile v1File = ScmFactory.File.getInstance(ws, file.getFileId(), 1, 0);
        Assert.assertEquals(version1ExpectedMd5, v1File.getMd5());

        ScmFile v2File = ScmFactory.File.getInstance(ws, file.getFileId());
        Assert.assertEquals(version2ExpectedMd5, v2File.getMd5());

        file.updateContent(new ByteArrayInputStream(version2Data));
        Assert.assertEquals(file.getMd5(), null);

        file.delete(true);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        bSiteSs.close();
    }
}
