package com.sequoiacm.client.core.file;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;
import com.sequoiacm.client.util.ScmTestTools;

public class TestScmFileDelete extends ScmTestBase {

    @Test
    public static void testDelete() throws IOException, ScmException {
        String testFuncName = "testDelete";
        ScmFile file = null;
        ScmSession ss = null;
        try {
            ss = ScmTestTools.createSession(url, user, password);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, ss);
            file = ScmTestTools.createFile(ss, workspaceName, testFuncName, testFuncName);
            ScmId id = file.getFileId();
            file.delete();

            ScmFile tmp = ScmFactory.File.getInstance(ws, id);
            Assert.assertNull(tmp);
        }
        catch (ScmException e) {
            e.printStackTrace();
            Assert.assertTrue(false, e.toString());
        }
        finally {
            ScmTestTools.removeFileIfExist(sdbUrl, sdbUser, sdbPasswd, workspaceName, file);
            ScmTestTools.releaseSession(ss);
        }
    }
}
