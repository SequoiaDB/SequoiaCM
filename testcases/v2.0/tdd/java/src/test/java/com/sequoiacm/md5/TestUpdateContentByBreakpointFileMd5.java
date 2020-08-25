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
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmBreakpointFileOption;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestUpdateContentByBreakpointFileMd5 extends ScmTestMultiCenterBase {

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
        Random rd = new Random();
        byte[] data1 = new byte[1024 * 1024 * 4 + 5];
        rd.nextBytes(data1);
        String expectedMd51 = DatatypeConverter.printBase64Binary(DigestUtils.md5(data1));

        ScmBreakpointFileOption op = new ScmBreakpointFileOption();
        op.setNeedMd5(true);
        ScmBreakpointFile bf1 = ScmFactory.BreakpointFile.createInstance(ws,
                TestUpdateContentByBreakpointFileMd5.class.getSimpleName()
                        + UUID.randomUUID().toString(),
                op);
        bf1.upload(new ByteArrayInputStream(data1));

        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setFileName(TestUpdateContentByBreakpointFileMd5.class.getSimpleName()
                + UUID.randomUUID().toString());
        file.save(new ScmUploadConf(false, true));
        file.updateContent(bf1);
        Assert.assertEquals(file.getMd5(), expectedMd51);

        byte[] data2 = new byte[1024 * 1024 * 4 + 5];
        rd.nextBytes(data2);
        String expectedMd52 = DatatypeConverter.printBase64Binary(DigestUtils.md5(data2));
        ScmBreakpointFile bf2 = ScmFactory.BreakpointFile.createInstance(ws,
                TestUpdateContentByBreakpointFileMd5.class.getSimpleName()
                        + UUID.randomUUID().toString(),
                op);
        bf2.upload(new ByteArrayInputStream(data2));
        file.updateContent(bf2, new ScmUpdateContentOption(true));
        Assert.assertEquals(file.getMd5(), expectedMd52);

        op.setNeedMd5(false);
        ScmBreakpointFile bf3 = ScmFactory.BreakpointFile.createInstance(ws,
                TestUpdateContentByBreakpointFileMd5.class.getSimpleName()
                        + UUID.randomUUID().toString(),
                op);
        bf3.upload(new ByteArrayInputStream(data1));

        try {
            file.updateContent(bf3, new ScmUpdateContentOption(true));
            Assert.fail();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getErrorCode(), ScmError.INVALID_ARGUMENT.getErrorCode());
        }

        file.delete(true);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        bSiteSs.close();
    }
}
