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
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmBreakpointFileOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestBreakpointFileCalcMd5 extends ScmTestMultiCenterBase {

    private ScmSession aSiteSs;
    private ScmWorkspace aws;

    @BeforeMethod
    @BeforeClass
    public void setUp() throws ScmException {

        aSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        aws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), aSiteSs);
    }

    @Test
    public void test() throws ScmException, IOException {
        Random rd = new Random();
        byte[] data = new byte[1024 * 1024 * 4];
        rd.nextBytes(data);
        String expectedMd5 = DatatypeConverter.printBase64Binary(DigestUtils.md5(data));

        ScmBreakpointFileOption op = new ScmBreakpointFileOption();
        op.setNeedMd5(false);
        ScmBreakpointFile bf = ScmFactory.BreakpointFile.createInstance(aws,
                TestBreakpointFileCalcMd5.class.getSimpleName() + UUID.randomUUID().toString(), op);
        bf.upload(new ByteArrayInputStream(data));

        Assert.assertEquals(bf.isNeedMd5(), false);
        Assert.assertEquals(bf.getMd5(), null);

        bf.calcMd5();
        Assert.assertEquals(bf.isNeedMd5(), true);
        Assert.assertEquals(bf.getMd5(), expectedMd5);

        bf = ScmFactory.BreakpointFile.getInstance(aws, bf.getFileName());
        Assert.assertEquals(bf.isNeedMd5(), true);
        Assert.assertEquals(bf.getMd5(), expectedMd5);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        aSiteSs.close();
    }
}
