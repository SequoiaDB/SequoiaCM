package org.sequoiacm.om.omserver.test.basic;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sequoiacm.om.omserver.test.ClientRespChecker;
import org.sequoiacm.om.omserver.test.OmServerTest;
import org.sequoiacm.om.omserver.test.ScmOmClient;
import org.sequoiacm.om.omserver.test.ScmOmTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.module.OmWorkspaceBasicInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceDataLocation;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.module.OmWorkspaceInfoWithStatistics;

import feign.Response;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OmServerTest.class)
public class TestWorkspace {
    @Autowired
    ObjectMapper objMapper;

    @Autowired
    ScmOmClient client;

    @Autowired
    ClientRespChecker respChecker;

    @Autowired
    ScmSession session;

    @Autowired
    ScmOmTestConfig config;

    @Test
    public void test() throws Exception {
        Response resp = client.login(config.getScmUser(), config.getScmPassword());
        respChecker.check(resp);

        String sessionId = (String) resp.headers().get(RestParamDefine.X_AUTH_TOKEN).toArray()[0];

        resp = client.getScmWorkspaceDetail(sessionId, config.getWorkspaceName1());
        respChecker.check(resp);
        String wsDetailStr = (String) resp.headers().get(RestParamDefine.WORKSPACE).toArray()[0];
        OmWorkspaceDetail wsDetail = objMapper.readValue(wsDetailStr, OmWorkspaceDetail.class);

        OmWorkspaceInfoWithStatistics wsWithStatistics = client
                .getWorksapceDetailWithStatistics(sessionId, config.getWorkspaceName1());

        ScmWorkspace wsDriver = ScmFactory.Workspace.getWorkspace(config.getWorkspaceName1(),
                session);

        checkWorkspace(wsDetail, wsWithStatistics, wsDriver);

        List<OmWorkspaceBasicInfo> wsList = client.getWorkspaceList(sessionId, 0, 1000);
        List<ScmWorkspaceInfo> driverWsList = new ArrayList<>();
        ScmCursor<ScmWorkspaceInfo> cursor = ScmFactory.Workspace.listWorkspace(session,
                ScmQueryBuilder.start(FieldName.FIELD_CLWORKSPACE_ID).is(1).get(), 0, 1000);
        while (cursor.hasNext()) {
            driverWsList.add(cursor.getNext());
        }
        cursor.close();

        Assert.assertEquals(wsList.size(), driverWsList.size());
        for (int i = 0; i < driverWsList.size(); i++) {
            ScmWorkspaceInfo driverWs = driverWsList.get(i);
            OmWorkspaceBasicInfo ws = wsList.get(i);
            Assert.assertEquals(ws.getCreateUser(), driverWs.getCreateUser());
            Assert.assertEquals(ws.getDescription(), driverWs.getDesc());
            Assert.assertEquals(ws.getName(), driverWs.getName());
            Assert.assertEquals(ws.getCreateTime(), driverWs.getCreateTime());
        }

        wsList = client.getWorkspaceList(sessionId, 1, 1);
        Assert.assertEquals(wsList.size(), 1);
        OmWorkspaceBasicInfo ws = wsList.get(0);
        ScmWorkspaceInfo driverWs = driverWsList.get(1);
        Assert.assertEquals(ws.getCreateUser(), driverWs.getCreateUser());
        Assert.assertEquals(ws.getDescription(), driverWs.getDesc());
        Assert.assertEquals(ws.getName(), driverWs.getName());
        Assert.assertEquals(ws.getCreateTime(), driverWs.getCreateTime());

        client.logout(sessionId);
    }

    private void checkWorkspace(OmWorkspaceDetail ws1, OmWorkspaceInfoWithStatistics ws2,
            ScmWorkspace ws3) {
        Assert.assertEquals(ws1.getCreateUser(), ws2.getCreateUser());
        Assert.assertEquals(ws1.getDescription(), ws2.getDescription());
        Assert.assertEquals(ws1.getName(), ws2.getName());
        Assert.assertEquals(ws1.getUpdateUser(), ws2.getUpdateUser());
        Assert.assertEquals(ws1.getCreateTime(), ws2.getCreateTime());
        Assert.assertEquals(ws1.getMetaOption(), ws2.getMetaOption());
        Assert.assertEquals(ws1.getUpdateTime(), ws2.getUpdateTime());
        List<OmWorkspaceDataLocation> ws2DataLocations = ws2.getDataLocations();
        List<OmWorkspaceDataLocation> ws1DataLocations = ws1.getDataLocations();
        Assert.assertEquals(ws1DataLocations.size(), ws2DataLocations.size());
        for (OmWorkspaceDataLocation ws1DataLocation : ws1DataLocations) {
            if (!ws2DataLocations.contains(ws1DataLocation)) {
                Assert.fail("not dound datalcoation:" + ws1DataLocation);
            }
        }

        Assert.assertEquals(ws1.getCreateUser(), ws3.getCreateUser());
        Assert.assertEquals(ws1.getDescription(), ws3.getDescription());
        Assert.assertEquals(ws1.getName(), ws3.getName());
        Assert.assertEquals(ws1.getUpdateUser(), ws3.getUpdateUser());
        Assert.assertEquals(ws1.getCreateTime(), ws3.getCreateTime());
        Assert.assertEquals(ws1.getUpdateTime(), ws3.getUpdateTime());

        List<ScmDataLocation> driverWsLocations = ws3.getDataLocations();
        List<OmWorkspaceDataLocation> omWsLocations = ws1.getDataLocations();
        Assert.assertEquals(omWsLocations.size(), driverWsLocations.size());
        for (ScmDataLocation driverWsLocation : driverWsLocations) {
            String name = driverWsLocation.getSiteName();
            String type = driverWsLocation.getType().toString();
            BSONObject dataOption = driverWsLocation.getBSONObject();
            dataOption.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
            boolean isSame = false;
            for (OmWorkspaceDataLocation omWsLocation : omWsLocations) {
                if (omWsLocation.getSiteName().equals(name)) {
                    Assert.assertEquals(type, omWsLocation.getSiteType());
                    BSONObject option = omWsLocation.getOptions();
                    Assert.assertEquals(option, dataOption);
                    isSame = true;
                    break;
                }
            }
            Assert.assertTrue("location not found:" + driverWsLocation, isSame);
        }

        ScmMetaLocation driverMeta = ws3.getMetaLocation();
        BSONObject ws1MetaOption = ws1.getMetaOption();
        BSONObject metaOption = driverMeta.getBSONObject();
        metaOption.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
        Assert.assertEquals(metaOption, ws1MetaOption);
    }
}
