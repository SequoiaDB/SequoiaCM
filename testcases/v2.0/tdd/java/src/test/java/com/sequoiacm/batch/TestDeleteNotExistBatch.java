package com.sequoiacm.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestDeleteNotExistBatch extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestDeleteNotExistBatch.class);
    private ScmSession ss;
    private ScmWorkspace ws;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void test() throws ScmException {
        logger.info("delete not exist batch");
        ScmId inexistentId = new ScmId("ffffffffffffffffffffffff");
        try {
            ScmFactory.Batch.deleteInstance(ws, inexistentId);
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.BATCH_NOT_FOUND) {
                throw e;
            }
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmTestTools.releaseSession(ss);
    }
}
