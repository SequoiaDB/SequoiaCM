package com.sequoiacm.workspace;

import java.util.ArrayList;

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
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class UpdateWorkspaceInMainSite extends ScmTestMultiCenterBase {
    private static final Logger logger = LoggerFactory.getLogger(CreateWorkspaceInMainSite.class);
    private ScmSession ss;
    private String wsName = "createWorkspaceTest3";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);
    }

    @Test
    public void test() throws Exception {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setName(wsName);
        conf.setDescription(this.getClass().getName());
        conf.setMetaLocation(new ScmSdbMetaLocation("rootSite", ScmShardingType.MONTH, "domain1"));
        conf.addDataLocation(new ScmSdbDataLocation("rootSite", "domain2", ScmShardingType.YEAR,
                ScmShardingType.QUARTER));

        ScmFactory.Workspace.createWorkspace(ss, conf);
        qeuryAndCheckWorkspace(ss, conf);
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);
        ws.addDataLocation(new ScmSdbDataLocation("branchSite1", "domain2"));

        qeuryAndCheckWorkspace(ss,
                conf.addDataLocation(new ScmSdbDataLocation("branchSite1", "domain2")));

        ScmSession s2 = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        qeuryAndCheckWorkspace(s2, conf);

        ws.removeDataLocation("branchSite1");

        while (true) {
            try {
                ScmFactory.Workspace.getWorkspace(wsName, s2);
                logger.warn("branchSite1 still can get ws:" + wsName + ",try later");
            }
            catch (ScmException e) {
                if (e.getError() != ScmError.SERVER_NOT_IN_WORKSPACE) {
                    throw e;
                }
                break;
            }
        }
        s2.close();

        ScmSdbDataLocation dataLocation = new ScmSdbDataLocation("rootSite", "domain2",
                ScmShardingType.YEAR, ScmShardingType.QUARTER);
        ArrayList<ScmDataLocation> list = new ArrayList<>();
        list.add(dataLocation);
        conf.setDataLocations(list);
        qeuryAndCheckWorkspace(ss, conf);

        ws.updatedDescription("newDesc");
        conf.setDescription("newDesc");
        qeuryAndCheckWorkspace(ss, conf);
    }

    private void qeuryAndCheckWorkspace(ScmSession ss, ScmWorkspaceConf conf) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);
        checkWs(conf, ws);
    }

    private void checkWs(ScmWorkspaceConf conf, ScmWorkspace ws) throws Exception {
        if (!ws.getName().equals(wsName)) {
            throw new Exception("except:" + conf.getName() + ",actual:" + ws.getName());
        }

        if (!ws.getDescription().equals(conf.getDescription())) {
            throw new Exception(
                    "except:" + conf.getDescription() + ",actual:" + ws.getDescription());
        }

        if (!ws.getMetaLocation().equals(conf.getMetaLocation())) {
            throw new Exception(
                    "except:" + conf.getMetaLocation() + ",actual:" + ws.getMetaLocation());
        }

        if (!ws.getDataLocations().equals(conf.getDataLocations())) {
            throw new Exception(
                    "except:" + conf.getDataLocations() + ",actual:" + ws.getDataLocations());
        }

        if (!ws.getCreateUser().equals(ss.getUser())) {
            throw new Exception("except:" + ss.getUser() + ",actual:" + ws.getCreateUser());
        }

        if (!ws.getUpdateUser().equals(ss.getUser())) {
            throw new Exception("except:" + ss.getUser() + ",actual:" + ws.getUpdateUser());
        }
    }

    @AfterClass
    public void cleanUp() throws ScmException {
        try {
            ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);
        }
        finally {
            ss.close();
        }
    }
}
