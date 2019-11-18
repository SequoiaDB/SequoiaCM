package com.sequoiacm.file.seek;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeTest;
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
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestSeekReadFromRemote extends ScmTestMultiCenterBase {
    private ScmSession ssM;
    private ScmWorkspace wsM;
    private ScmSession ssB;
    private ScmWorkspace wsB;
    private String downFile;
    private String workDir;

    @BeforeTest
    public void setUp() throws ScmException {
        workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.createDir(workDir);
        downFile = workDir + File.separator + "down.txt";
        ssB = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        wsB = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ssB);

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
    public void test() throws ScmException, IOException {
        ScmFile scmFileM = ScmFactory.File.createInstance(wsM);
        scmFileM.setFileName(getClass().getSimpleName());
        scmFileM.setContent(getSrcFile());
        scmFileM.save();

        ScmFile scmFileB = ScmFactory.File.getInstance(wsB, scmFileM.getFileId());
        ScmInputStream is = ScmFactory.File.createInputStream(InputStreamType.SEEKABLE, scmFileB);
        FileOutputStream fileOs = new FileOutputStream(downFile);

        is.seek(CommonDefine.SeekType.SCM_FILE_SEEK_SET, 64);
        byte[] buf = new byte[1024 * 1024];
        while (true) {
            int actReadLen = is.read(buf, 0, 1024 * 1024);
            if (actReadLen == -1) {
                break;
            }
            fileOs.write(buf, 0, actReadLen);
        }
        fileOs.close();

        String expectFile = createExpectFile();
        ScmTestTools.compareMd5(expectFile, downFile);

        ScmFile f = ScmFactory.File.getInstance(wsM, scmFileM.getFileId());
        List<ScmFileLocation> list = f.getLocationList();
        Assert.assertEquals(list.size(), 2);
        Assert.assertNotEquals(list.get(0).getSiteId(), list.get(1).getSiteId());
        scmFileM.delete(true);
    }

    private String createExpectFile() throws IOException {
        String expectFile = workDir + File.separator + "expect.txt";
        FileOutputStream expOs = new FileOutputStream(expectFile);

        RandomAccessFile rdSrcFile = new RandomAccessFile(new File(getSrcFile()), "r");
        rdSrcFile.seek(64);
        byte[] buf = new byte[1024 * 1024];
        while (true) {
            int actReadlen = rdSrcFile.read(buf, 0, 1024 * 1024);
            if (actReadlen == -1) {
                break;
            }
            expOs.write(buf, 0, actReadlen);
        }
        rdSrcFile.close();
        expOs.close();
        return expectFile;
    }

    @AfterClass
    public void tearDown() {
        ssM.close();
        ssB.close();
    }
}
