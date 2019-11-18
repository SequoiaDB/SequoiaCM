
package com.sequoiacm.testcommon.scmutils;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import org.apache.log4j.Logger;
import org.testng.Assert;

import java.util.UUID;

public class ScmAuthUtils extends TestScmBase {
    private static final Logger logger = Logger.getLogger(ScmAuthUtils.class);
    private static final int defaultTimeOut = 1 * 60 * 1000; // 1min
    private static final int sleepTime = 10 * 1000;  // 10s

    /**
     * check privilege is effect by wsp
     *
     * @author huangxiaoni
     */
    public static void checkPriority(SiteWrapper site, String username, String password, ScmRole role, WsWrapper wsp) throws Exception {
        checkPriority(site, username, password, role, wsp.getName());
    }

    /**
     * check privilege is effect by wsName
     *
     * @author huangxiaoni
     */
    public static void checkPriority(SiteWrapper site, String username, String password, ScmRole role, String wsName) throws Exception {
        ScmSession ss = null;
        try {
            // login
            ss = TestScmTools.createSession(site);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);

            ScmUser user = ScmFactory.User.getUser(ss, username);
            Assert.assertTrue(user.hasRole(role));

            //create path
            String dirPath = "/ScmAuthUtils"+"_"+username + "_" + UUID.randomUUID() ;
            ScmDirectory dir = ScmFactory.Directory.createInstance(ws,dirPath);
            String fileName = "ScmAuthUtils" + "_" + username + "_" + UUID.randomUUID();

            // grant privilege
            int version1 = ScmFactory.Privilege.getMeta(ss).getVersion();
            ScmResource resource = ScmResourceFactory.createDirectoryResource(wsName, dirPath);
            ScmFactory.Role.grantPrivilege(ss, role, resource, ScmPrivilegeType.DELETE);

            // check privilege is effect
            ScmSession newSS = null;
            try {
                newSS = TestScmTools.createSession(site, username, password);
                ScmWorkspace newWS = ScmFactory.Workspace.getWorkspace(wsName, newSS);

                int curTimes = 0;
                int maxTimes = defaultTimeOut / sleepTime;
                ScmId fileId = null;
                while (true) {
                    try {
                        Thread.sleep(sleepTime);
                        ScmFile file = ScmFactory.File.createInstance(ws);
                        file.setFileName(fileName);
                        file.setDirectory(dir);
                        fileId = file.save();
                        ScmFactory.File.deleteInstance(newWS, fileId, true);
                        ScmFactory.Directory.deleteInstance(ws,dirPath);
                        break;
                    } catch (ScmException e) {
                        if (ScmError.OPERATION_UNAUTHORIZED == e.getError() && curTimes < maxTimes) {
                            Thread.sleep(sleepTime);
                            curTimes++;
                        } else if (curTimes >= maxTimes) {
                            throw new Exception("privilege did not come into effect, timeout.version1 = " + version1
                                    + ",version2 = " + ScmFactory.Privilege.getMeta(ss).getVersion() + ",fileId = " + fileId);
                        } else {
                            logger.error("failed to wait privilege come into effect,version1 = " + version1
                                    + ",version2 = " + ScmFactory.Privilege.getMeta(ss).getVersion() + ",fileId = " + fileId);
                            throw e;
                        }
                    }
                }
            } finally {
                if (null != newSS) {
                    newSS.close();
                }
            }
        } finally {
            if (null != ss) {
                ss.close();
            }
        }
    }
}
