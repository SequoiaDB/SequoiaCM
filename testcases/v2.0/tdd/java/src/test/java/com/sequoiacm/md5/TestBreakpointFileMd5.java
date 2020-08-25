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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestBreakpointFileMd5 extends ScmTestMultiCenterBase {

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
        byte[] data = new byte[1024 * 1024 * 4 + 5];
        rd.nextBytes(data);
        String expectedMd5 = DatatypeConverter.printBase64Binary(DigestUtils.md5(data));

        ScmBreakpointFileOption op = new ScmBreakpointFileOption();
        op.setNeedMd5(true);
        ScmBreakpointFile bf = ScmFactory.BreakpointFile.createInstance(ws,
                TestBreakpointFileMd5.class.getSimpleName() + UUID.randomUUID().toString(), op);
        bf.upload(new ByteArrayInputStream(data));

        Assert.assertEquals(bf.isNeedMd5(), true);
        Assert.assertEquals(bf.getMd5(), expectedMd5);

        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setFileName(
                TestBreakpointFileMd5.class.getSimpleName() + UUID.randomUUID().toString());
        file.setContent(bf);
        file.save(new ScmUploadConf(false, true));
        String scmMd5 = file.getMd5();
        Assert.assertEquals(expectedMd5, scmMd5);
        file.delete(true);

        file = ScmFactory.File.createInstance(ws);
        file.setFileName(
                TestBreakpointFileMd5.class.getSimpleName() + UUID.randomUUID().toString());
        file.setContent(new ByteArrayInputStream(data));
        file.save(new ScmUploadConf(false, false));
        Assert.assertEquals(expectedMd5, scmMd5);
        
        file.delete(true);

        op.setNeedMd5(false);
        bf = ScmFactory.BreakpointFile.createInstance(ws,
                TestBreakpointFileMd5.class.getSimpleName() + UUID.randomUUID().toString(), op);
        bf.upload(new ByteArrayInputStream(data));
        file = ScmFactory.File.createInstance(ws);
        file.setFileName(
                TestBreakpointFileMd5.class.getSimpleName() + UUID.randomUUID().toString());
        file.setContent(bf);
        try {
            file.save(new ScmUploadConf(false, true));
            Assert.fail();
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getErrorCode(), ScmError.INVALID_ARGUMENT.getErrorCode());
        }
        
        
        

    }

    @AfterClass
    public void tearDown() throws ScmException {
        bSiteSs.close();
    }
}
