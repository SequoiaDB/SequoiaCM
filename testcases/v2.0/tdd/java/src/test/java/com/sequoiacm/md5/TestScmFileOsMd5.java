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
import com.sequoiacm.client.core.ScmOutputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestScmFileOsMd5 extends ScmTestMultiCenterBase {

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
        byte[] data = new byte[1024 * 1024 * 4];
        rd.nextBytes(data);
        String expectedMd5 = DatatypeConverter.printBase64Binary(DigestUtils.md5(data));

        file.setFileName(TestScmFileOsMd5.class.getSimpleName() + UUID.randomUUID().toString());
        ScmOutputStream os = ScmFactory.File.createOutputStream(file,
                new ScmUploadConf(false, true));
        os.write(data, 0, 2 * 1024 * 1024);
        os.write(data, 2 * 1024 * 1024, 2 * 1024 * 1024);
        os.commit();
        String scmMd5 = file.getMd5();
        Assert.assertEquals(expectedMd5, scmMd5);
        file = ScmFactory.File.getInstance(ws, file.getFileId());
        scmMd5 = file.getMd5();
        Assert.assertEquals(expectedMd5, scmMd5);

        file = ScmFactory.File.createInstance(ws);
        file.setFileName(TestScmFileOsMd5.class.getSimpleName() + UUID.randomUUID().toString());
        os = ScmFactory.File.createOutputStream(file, new ScmUploadConf(true, false));
        os.write(data, 0, 2 * 1024 * 1024);
        os.write(data, 2 * 1024 * 1024, 2 * 1024 * 1024);
        os.commit();
        Assert.assertTrue(file.getMd5() == null);
        file = ScmFactory.File.getInstance(ws, file.getFileId());
        scmMd5 = file.getMd5();
        Assert.assertTrue(file.getMd5() == null);
        file.delete(true);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        bSiteSs.close();
    }
}
