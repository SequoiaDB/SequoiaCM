package com.sequoiacm.file.seek;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.InputStreamType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestSeek extends ScmTestMultiCenterBase {
    private ScmSession ssM;
    private ScmWorkspace wsM;
    private static final Logger logger = LoggerFactory.getLogger(TestSeek.class);

    @BeforeClass
    public void init() throws ScmException {

        ssM = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        wsM = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ssM);
        clearEnv();
    }

    private void clearEnv() throws ScmException {
        ScmCursor<ScmFileBasicInfo> c = ScmFactory.File.listInstance(wsM, ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
                .is(this.getClass().getSimpleName()).get());
        try {
            while (c.hasNext()) {
                ScmFactory.File.deleteInstance(wsM, c.getNext().getFileId(), true);
            }
        }
        finally {
            c.close();
        }
    }

    @Test
    public void test() throws ScmException, InterruptedException, IOException {
        ScmFile scmFile = ScmFactory.File.createInstance(wsM);
        scmFile.setFileName(getClass().getSimpleName());

        String fileContent = "0123456789abcdefghijk";
        scmFile.setContent(new ByteArrayInputStream(fileContent.getBytes()));
        scmFile.save();

        ScmInputStream is = ScmFactory.File.createInputStream(InputStreamType.SEEKABLE, scmFile);
        ByteArrayOutputStream byteOs1 = new ByteArrayOutputStream();
        is.read(byteOs1);
        byte[] downByte = byteOs1.toByteArray();
        Assert.assertEquals(downByte, fileContent.getBytes());

        byte[] buf = new byte[5];
        Assert.assertEquals(is.read(buf, 0, 5), -1);

        is.seek(CommonDefine.SeekType.SCM_FILE_SEEK_SET, 3);
        ByteArrayOutputStream byteOs2 = new ByteArrayOutputStream();
        is.read(byteOs2);
        downByte = byteOs2.toByteArray();
        Assert.assertEquals(downByte, fileContent.substring(3).getBytes());

        Assert.assertEquals(is.read(buf, 0, 5), -1);

        is.seek(CommonDefine.SeekType.SCM_FILE_SEEK_SET, 6);
        int actualReadLen = is.read(buf, 0, 5);
        logger.info("readlen=" + actualReadLen);
        Assert.assertEquals(new String(buf, 0, actualReadLen),
                fileContent.substring(6, 6 + 5));

        is.seek(CommonDefine.SeekType.SCM_FILE_SEEK_SET, 3);
        actualReadLen = is.read(buf, 2, 3);
        logger.info("readlen=" + actualReadLen);
        Assert.assertEquals(new String(buf, 2, actualReadLen),
                fileContent.substring(3, 3 + 3));

        is.close();
        scmFile.delete(true);
    }

    @AfterClass
    public void tearDown() {
        ssM.close();
    }
}
