package com.sequoiacm.audit;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
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
import java.util.UUID;

/**
 * @Description: SCM-2343:指定userType和user的用户类型相同，审计类型有空字符串
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2343 extends TestScmBase {
    private String attrName = "2343";
    private String name = "2343";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
        ConfUtil.deleteUserAndRole(name,name);
        ConfUtil.createUser(wsp,name, ScmUserPasswordType.LOCAL,new ScmPrivilegeType[]{ScmPrivilegeType.ALL});
        session = TestScmTools.createSession(site);
        ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void test() throws ScmException {
        test1();
        ConfUtil.deleteAuditConf(site.getSiteServiceName());

        test2();
        ConfUtil.deleteAuditConf(site.getSiteServiceName());

        test3();
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteUserAndRole(name,name);
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
        if (session != null) {
            session.close();
        }
    }

    // scm.audit.userType.LOCAL="" scm.audit.user.test=META_ATTR_DML
    private void test1() throws ScmException {
        Map<String, String> confMap = new HashMap<String, String>();
        confMap.put(ConfigCommonDefind.scm_audit_userType+ScmUserPasswordType.LOCAL.name(), "");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap);

        Map<String, String> confMap1 = new HashMap<String, String>();
        confMap1.put(ConfigCommonDefind.scm_audit_user + name, "META_ATTR_DML");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap1);

        //check
        checkAudit(TestScmBase.scmUserName,TestScmBase.scmPassword,false,false);
        checkAudit(name,name,true,false);
    }

    // scm.audit.userType.LOCAL=META_ATTR_DML scm.audit.user.test=""
    private void test2() throws ScmException {
        Map<String, String> confMap = new HashMap<String, String>();
        confMap.put(ConfigCommonDefind.scm_audit_userType+ScmUserPasswordType.LOCAL.name(), "META_ATTR_DML");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap);

        Map<String, String> confMap1 = new HashMap<String, String>();
        confMap1.put(ConfigCommonDefind.scm_audit_user + name, "");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap1);

        //check
        checkAudit(TestScmBase.scmUserName,TestScmBase.scmPassword,true,false);
        checkAudit(name,name,false,false);
    }

    // scm.audit.userType.LOCAL="" scm.audit.user.test=""
    private void test3() throws ScmException {
        Map<String, String> confMap = new HashMap<String, String>();
        confMap.put(ConfigCommonDefind.scm_audit_userType+ScmUserPasswordType.LOCAL.name(), "");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap);

        Map<String, String> confMap1 = new HashMap<String, String>();
        confMap1.put(ConfigCommonDefind.scm_audit_user + name, "");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap1);

        //check
        checkAudit(TestScmBase.scmUserName,TestScmBase.scmPassword,false,false);
        checkAudit(name,name,false,false);
    }

    private void checkAudit(String username,String password,boolean isLogged1,boolean isLogged2) throws ScmException {
        ScmId attrId = null;
        String attrName1 = attrName + UUID.randomUUID();
        try {
            attrId = craeteAndQueryAttr(username,password,attrName1);
            BSONObject bson1 = new BasicBSONObject().append(ScmAttributeName.Audit.TYPE, "CREATE_META_ATTR")
                    .append(ScmAttributeName.Audit.USERNAME, username);
            BSONObject bson2 = new BasicBSONObject().append(ScmAttributeName.Audit.TYPE, "META_ATTR_DQL")
                    .append(ScmAttributeName.Audit.USERNAME, username);
            Assert.assertEquals(ConfUtil.checkAudit(session,bson1, attrName1), isLogged1, "Has the configuration been updated? attrId = " + attrId.get());
            Assert.assertEquals(ConfUtil.checkAudit(session,bson2, attrId.get()), isLogged2, "Has the configuration been updated? attrId = " + attrId.get());
        } finally {
            if(attrId != null){
               ScmFactory.Attribute.deleteInstance(ws,attrId);
            }
        }
    }

    private ScmId craeteAndQueryAttr(String username,String password,String attrName) throws ScmException {
        ScmSession session = null;
        ScmId attrId = null;
        try {
            session = TestScmTools.createSession(site,username,password);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            //create
             ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, new ScmAttributeConf()
                    .setName(attrName).setType(AttributeType.STRING));
            attrId = attr.getId();
            //query
            ScmFactory.Attribute.getInstance(ws,attr.getId());
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return attrId;
    }
}
