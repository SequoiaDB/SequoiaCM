package com.sequoiacm.fileversion;

import java.io.File;

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
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestReadFileCrossSite extends ScmTestMultiCenterBase {
    private ScmSession branch1Session;
    private ScmWorkspace branch1Ws;
    private String version1File;
    private String version2File;
    private String downFile;
    private ScmSession branch2Session;
    private ScmWorkspace branch2Ws;
    private ScmSession rootSession;
    private ScmWorkspace rootWs;

    @BeforeClass
    public void init() throws ScmException {
        String workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.deleteDir(workDir);
        ScmTestTools.createDir(workDir);
        version1File = workDir + File.separator + "version1.data";
        version2File = workDir + File.separator + "version2.data";
        ScmTestTools.createFile(version1File, "version1version1", 1024);
        ScmTestTools.createFile(version2File, "version2version2", 2048);

        downFile = workDir + File.separator + "downFile.data";

        branch1Session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        branch1Ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), branch1Session);

        branch2Session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer3().getUrl(), getScmUser(), getScmPasswd()));
        branch2Ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), branch2Session);

        rootSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        rootWs = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), rootSession);
        clearEnv();
    }

    private void clearEnv() throws ScmException {
        ScmCursor<ScmFileBasicInfo> c = ScmFactory.File.listInstance(branch1Ws,
                ScopeType.SCOPE_CURRENT, ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
                .is(this.getClass().getSimpleName()).get());
        try {
            while (c.hasNext()) {
                ScmFactory.File.deleteInstance(branch1Ws, c.getNext().getFileId(), true);
            }
        }
        finally {
            c.close();
        }
    }

    @Test
    public void test() throws ScmException {
        ScmFile branch1F = ScmFactory.File.createInstance(branch1Ws);
        branch1F.setFileName(this.getClass().getSimpleName());
        branch1F.setContent(version1File);
        branch1F.save();
        branch1F.updateContent(version2File);

        testReadCurrentVersionFile(branch1F.getFileId());
        testReadHistoryVersionFile(branch1F.getFileId());
        branch1F.delete(true);
    }

    private void testReadHistoryVersionFile(ScmId fileId) throws ScmException {
        ScmFile branch2File = ScmFactory.File.getInstance(branch2Ws, fileId, 1, 0);
        branch2File.getContent(downFile);
        ScmTestTools.compareMd5(version1File, downFile);
        ScmTestTools.deleteFile(downFile);

        branch2File.getContentFromLocalSite(downFile);
        ScmTestTools.compareMd5(version1File, downFile);
        ScmTestTools.deleteFile(downFile);

        ScmFile rootFile = ScmFactory.File.getInstance(rootWs, fileId, 1, 0);
        rootFile.getContentFromLocalSite(downFile);
        ScmTestTools.compareMd5(version1File, downFile);
        ScmTestTools.deleteFile(downFile);

    }

    private void testReadCurrentVersionFile(ScmId fileId) throws ScmException {
        ScmFile branch2File = ScmFactory.File.getInstance(branch2Ws, fileId, 2, 0);
        branch2File.getContent(downFile);
        ScmTestTools.compareMd5(version2File, downFile);
        ScmTestTools.deleteFile(downFile);

        branch2File.getContentFromLocalSite(downFile);
        ScmTestTools.compareMd5(version2File, downFile);
        ScmTestTools.deleteFile(downFile);

        ScmFile rootFile = ScmFactory.File.getInstance(rootWs, fileId, 2, 0);
        rootFile.getContentFromLocalSite(downFile);
        ScmTestTools.compareMd5(version2File, downFile);
        ScmTestTools.deleteFile(downFile);
    }

    @AfterClass
    public void tearDown() {
        branch1Session.close();
    }
}
