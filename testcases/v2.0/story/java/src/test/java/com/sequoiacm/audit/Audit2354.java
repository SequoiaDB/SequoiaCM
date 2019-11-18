package com.sequoiacm.audit;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: SCM-2355:所有有关于审计类型的配置项都配置，服务为authserver
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2354 extends TestScmBase {
    private String serviceName = "schedule-server";
    private String username1 = "token2355A";
    private String username2 = "token2355B";
    private String username3 = "local2355A";
    private String scheduleName = "schedule2354";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf(serviceName);
        ConfUtil.deleteUserAndRole(username1, username1);
        ConfUtil.deleteUserAndRole(username2, username2);
        ConfUtil.deleteUserAndRole(username3, username3);
        ConfUtil.createUser(wsp, username1, ScmUserPasswordType.TOKEN, new ScmPrivilegeType[]{ScmPrivilegeType.ALL});
        ConfUtil.createUser(wsp, username2, ScmUserPasswordType.TOKEN, new ScmPrivilegeType[]{ScmPrivilegeType.ALL});
        ConfUtil.createUser(wsp, username3, ScmUserPasswordType.LOCAL, new ScmPrivilegeType[]{ScmPrivilegeType.ALL});
        session = TestScmTools.createSession(site);
    }

    @Test(groups = {"twoSite", "fourSite"})
    private void test() throws ScmException, InterruptedException {
        Map<String, String> confMap = new HashMap<String, String>();
        confMap.put(ConfigCommonDefind.scm_audit_user+username1, "SCHEDULE_DML");
        confMap.put(ConfigCommonDefind.scm_audit_userType+ScmUserPasswordType.TOKEN.name(), "SCHEDULE_DQL");
        confMap.put(ConfigCommonDefind.scm_audit_userMask, "LOCAL");
        confMap.put(ConfigCommonDefind.scm_audit_mask, "SCHEDULE_DQL|SCHEDULE_DML");
        ConfUtil.updateConf(serviceName, confMap);

        //check
        checkAudit(username1,username1,true, false);
        checkAudit(username2,username2,false, true);
        checkAudit(username3,username3,true, true);
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteUserAndRole(username1,username1);
        ConfUtil.deleteUserAndRole(username2,username2);
        ConfUtil.deleteUserAndRole(username3,username3);
        ConfUtil.deleteAuditConf(serviceName);
        if (session != null) {
            session.close();
        }
    }

    private void checkAudit(String username,String password,boolean isLogged1, boolean isLogged2) throws ScmException, InterruptedException {
        ScmSchedule schedule = null;
        try {
            schedule = createAndQuerySche(username,password);
            BSONObject bson1 = new BasicBSONObject().append(ScmAttributeName.Audit.TYPE, "CREATE_SCHEDULE")
                    .append(ScmAttributeName.Audit.USERNAME, username);
            BSONObject bson2 = new BasicBSONObject().append(ScmAttributeName.Audit.TYPE, "SCHEDULE_DQL")
                    .append(ScmAttributeName.Audit.USERNAME, username);
            Assert.assertEquals(ConfUtil.checkAudit(session, bson1, scheduleName), isLogged1, "Has the configuration been updated? scheduleName = " + scheduleName);
            Assert.assertEquals(ConfUtil.checkAudit(session, bson2, schedule.getId().get()), isLogged2, "Has the configuration been updated? scheduleName = " + scheduleName);
        } finally {
            if(schedule != null) {
                ScmSystem.Schedule.delete(session,schedule.getId());
            }
        }
    }

    private ScmSchedule createAndQuerySche(String username,String password) throws ScmException {
        SiteWrapper rootSite = ScmInfo.getRootSite();
        SiteWrapper branchSite = ScmInfo.getBranchSite();
        ScmSession session = null;
        ScmSchedule schedule = null;
        try {
            session = TestScmTools.createSession(site,username,password);
            ScmScheduleContent copyContent = new ScmScheduleCopyFileContent(branchSite.getSiteName(), rootSite.getSiteName(),
                    "3650d", new BasicBSONObject(), ScmType.ScopeType.SCOPE_CURRENT);
            schedule = ScmSystem.Schedule.create(session, wsp.getName(),
                    ScheduleType.COPY_FILE, scheduleName, "desc", copyContent,
                    "* * * *,* * ?");
            ScmSystem.Schedule.get(session,schedule.getId());
        }finally{
            if(session != null){
                session.close();
            }
        }
        return schedule;
    }
}
