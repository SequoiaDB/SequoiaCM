package com.sequoiacm.fileversion;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmOutputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestCreateNewVersionByOs extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private String version1File;
    private String version2File;
    private String downVersion1File;
    private String downVersion2File;
    private String downVersion2BFile;

    @BeforeClass
    public void init() throws ScmException {
        String workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.deleteDir(workDir);
        ScmTestTools.createDir(workDir);
        version1File = workDir + File.separator + "version1.data";
        version2File = workDir + File.separator + "version2.data";
        ScmTestTools.createFile(version1File, "version1version1", 1024);
        ScmTestTools.createFile(version2File, "version2version2", 2048);

        downVersion1File = workDir + File.separator + "version1.down.data";
        downVersion2File = workDir + File.separator + "version2.down.data";
        downVersion2BFile = workDir + File.separator + "version2_B.down.data";

        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        clearEnv();
    }

    private void clearEnv() throws ScmException {
        ScmCursor<ScmFileBasicInfo> c = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
                .is(this.getClass().getSimpleName()).get());
        try {
            while (c.hasNext()) {
                ScmFactory.File.deleteInstance(ws, c.getNext().getFileId(), true);
            }
        }
        finally {
            c.close();
        }
    }

    @Test
    public void test() throws ScmException, IOException {
        ScmFile f = ScmFactory.File.createInstance(ws);
        f.setFileName(this.getClass().getSimpleName());
        f.setContent(version1File);
        f.save();

        ScmOutputStream os = null;
        FileInputStream fis = null;
        try {
            os = ScmFactory.File.createUpdateOutputStream(f);
            fis = new FileInputStream(version2File);
            byte[] b = new byte[1024];
            while (true) {
                int len = fis.read(b, 0, 1024);
                if (len == -1) {
                    break;
                }
                os.write(b, 0, len);
            }
            os.commit();
        }
        catch (Exception e) {
            os.cancel();
            throw e;
        }
        finally {
            fis.close();
        }

        f.getContent(downVersion2File);
        ScmTestTools.compareMd5(version2File, downVersion2File);

        ScmFile f1 = ScmFactory.File.getInstance(ws, f.getFileId(), 1, 0);
        f1.getContent(downVersion1File);
        ScmTestTools.compareMd5(version1File, downVersion1File);

        ScmFile f2 = ScmFactory.File.getInstance(ws, f.getFileId(), 2, 0);
        f2.getContent(downVersion2BFile);
        ScmTestTools.compareMd5(version2File, downVersion2BFile);

        f.delete(true);
    }

    @AfterClass
    public void tearDown() {
        ss.close();
    }

}
