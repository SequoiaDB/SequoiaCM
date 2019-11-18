package com.sequoiacm.session;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

/**
 * 未登陆的session进行非法操作
 *
 * @author linyoubin
 *
 */
public class NoAuthSessionDoAuthOp extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(NoAuthSessionDoAuthOp.class);

    @Test
    public void testOp() throws ScmException, InterruptedException, IOException {

        ScmSession tempSs = null;
        try {
            tempSs = ScmFactory.Session.createSession(SessionType.NOT_AUTH_SESSION,
                    new ScmConfigOption(getServer2().getUrl(),
                            getScmUser(), getScmPasswd()));
            ScmFactory.Workspace.getWorkspace(getWorkspaceName(), tempSs);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getErrorCode(), ScmError.HTTP_FORBIDDEN.getErrorCode());
        }
        catch (Exception e) {
            Assert.fail("unexpected exception!", e);
        }
        finally {
            if (tempSs != null) {
                tempSs.close();
            }
        }
    }
}
