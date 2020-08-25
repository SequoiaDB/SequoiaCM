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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestScmFileCalcMd5 extends ScmTestMultiCenterBase {

    private ScmSession bSiteSs;
    private ScmWorkspace bws;
    private ScmSession cSiteSs;
    private ScmWorkspace cws;
    private ScmSession aSiteSs;
    private ScmWorkspace aws;

    @BeforeMethod
    @BeforeClass
    public void setUp() throws ScmException {

        aSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        aws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), aSiteSs);

        bSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        bws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), bSiteSs);

        cSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer3().getUrl(), getScmUser(), getScmPasswd()));
        cws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), cSiteSs);
    }

    @Test
    public void test() throws ScmException, IOException {

        Random rd = new Random();
        byte[] data = new byte[1024 * 1024 * 4];
        rd.nextBytes(data);
        String expectedMd5 = DatatypeConverter.printBase64Binary(DigestUtils.md5(data));

        // local site calc
        ScmFile file = ScmFactory.File.createInstance(bws);
        file.setFileName(TestScmFileCalcMd5.class.getSimpleName() + UUID.randomUUID().toString());
        file.setContent(new ByteArrayInputStream(data));
        file.save();
        String scmMd5 = file.getMd5();
        Assert.assertEquals(scmMd5, null);
        file.calcMd5();
        Assert.assertEquals(file.getMd5(), expectedMd5);
        file = ScmFactory.File.getInstance(bws, file.getFileId());
        Assert.assertEquals(file.getMd5(), expectedMd5);
        file.delete(true);

        // forward to rootSite calc
        file = ScmFactory.File.createInstance(aws);
        file.setFileName(TestScmFileCalcMd5.class.getSimpleName() + UUID.randomUUID().toString());
        file.setContent(new ByteArrayInputStream(data));
        file.save();
        file = ScmFactory.File.getInstance(bws, file.getFileId());
        file.calcMd5();
        Assert.assertEquals(file.getMd5(), expectedMd5);
        file = ScmFactory.File.getInstance(bws, file.getFileId());
        Assert.assertEquals(file.getMd5(), expectedMd5);
        file.delete(true);

        //forward to rootsite, rootsite forward to branchsite calc
        file = ScmFactory.File.createInstance(cws);
        file.setFileName(TestScmFileCalcMd5.class.getSimpleName() + UUID.randomUUID().toString());
        file.setContent(new ByteArrayInputStream(data));
        file.save();
        file = ScmFactory.File.getInstance(bws, file.getFileId());
        file.calcMd5();
        Assert.assertEquals(file.getMd5(), expectedMd5);
        file = ScmFactory.File.getInstance(bws, file.getFileId());
        Assert.assertEquals(file.getMd5(), expectedMd5);
        file.delete(true);

        // Version
        file = ScmFactory.File.createInstance(cws);
        file.setFileName(TestScmFileCalcMd5.class.getSimpleName() + UUID.randomUUID().toString());
        file.setContent(new ByteArrayInputStream(data));
        file.save();
        byte[] data2 = new byte[1024 * 1024 * 4];
        rd.nextBytes(data2);
        String expected2Md5 = DatatypeConverter.printBase64Binary(DigestUtils.md5(data2));
        file.updateContent(new ByteArrayInputStream(data2));
        file.calcMd5();
        Assert.assertEquals(file.getMd5(), expected2Md5);

        ScmFile fileVersion1 = ScmFactory.File.getInstance(cws, file.getFileId(), 1, 0);
        Assert.assertEquals(fileVersion1.getMd5(), null);

        fileVersion1.calcMd5();
        Assert.assertEquals(fileVersion1.getMd5(), expectedMd5);
        fileVersion1 = ScmFactory.File.getInstance(cws, file.getFileId(), 1, 0);
        Assert.assertEquals(fileVersion1.getMd5(), expectedMd5);

        file = ScmFactory.File.getInstance(bws, file.getFileId());
        Assert.assertEquals(file.getMd5(), expected2Md5);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        bSiteSs.close();
        aSiteSs.close();
        cSiteSs.close();
    }
}
