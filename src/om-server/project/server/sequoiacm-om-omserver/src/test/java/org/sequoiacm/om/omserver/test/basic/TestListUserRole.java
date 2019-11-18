package org.sequoiacm.om.omserver.test.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.bson.BasicBSONObject;
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
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.module.OmRoleBasicInfo;
import com.sequoiacm.om.omserver.module.OmUserInfo;

import feign.Response;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OmServerTest.class)
public class TestListUserRole {
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

        // listUser
        List<OmUserInfo> users = client.listUsers(sessionId, new BasicBSONObject(), 0, 1000);
        List<ScmUser> driverUsers = new ArrayList<ScmUser>();
        ScmCursor<ScmUser> cursor = ScmFactory.User.listUsers(session, new BasicBSONObject(), 0,
                1000);
        while (cursor.hasNext()) {
            driverUsers.add(cursor.getNext());
        }
        cursor.close();
        Assert.assertEquals(users.size(), driverUsers.size());
        for (int i = 0; i < users.size(); i++) {
            OmUserInfo omUser = users.get(i);
            ScmUser driverUser = driverUsers.get(i);
            Assert.assertEquals(omUser.getUserName(), driverUser.getUsername());
            Assert.assertEquals(omUser.getUserType(), driverUser.getPasswordType().name());
            Assert.assertEquals(omUser.getUserId(), driverUser.getUserId());
            Assert.assertEquals(omUser.isEnable(), driverUser.isEnabled());
            List<OmRoleBasicInfo> omRoles = omUser.getRoles();
            Collection<ScmRole> driverRoles = driverUser.getRoles();
            Assert.assertEquals(omRoles.size(), driverRoles.size());
            Iterator<ScmRole> it = driverRoles.iterator();
            for (OmRoleBasicInfo omRole : omRoles) {
                ScmRole driverRole = it.next();
                Assert.assertEquals(omRole.getDescription(), driverRole.getDescription());
                Assert.assertEquals(omRole.getRoleId(), driverRole.getRoleId());
                Assert.assertEquals(omRole.getRoleName(), driverRole.getRoleName());
            }
        }

        // list Role
        List<OmRoleBasicInfo> omRoles = client.listRoles(sessionId, 0, 1000);
        List<ScmRole> driverRoles = new ArrayList<>();
        ScmCursor<ScmRole> driverRolesCursor = ScmFactory.Role.listRoles(session,
                new BasicBSONObject().append(FieldName.FIELD_ALL_OBJECTID, 1), 0, 1000);
        while (driverRolesCursor.hasNext()) {
            driverRoles.add(driverRolesCursor.getNext());
        }
        driverRolesCursor.close();

        Assert.assertEquals(omRoles.size(), driverRoles.size());
        for (int i = 0; i < omRoles.size(); i++) {
            OmRoleBasicInfo omRole = omRoles.get(i);
            ScmRole driverRole = driverRoles.get(i);
            Assert.assertEquals(omRole.getDescription(), driverRole.getDescription());
            Assert.assertEquals(omRole.getRoleId(), driverRole.getRoleId());
            Assert.assertEquals(omRole.getRoleName(), driverRole.getRoleName());
        }

        client.logout(sessionId);
    }
}
