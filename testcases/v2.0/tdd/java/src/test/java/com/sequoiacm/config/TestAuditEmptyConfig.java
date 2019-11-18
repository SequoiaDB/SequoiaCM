package com.sequoiacm.config;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmAuditInfo;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.TestAuditConfigBase;

public class TestAuditEmptyConfig extends TestAuditConfigBase {
    private ScmSession session;

    @BeforeClass
    public void setUp() throws ScmException {
        session = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
    }
    /*  测试步骤：
     * （1）清空与该测试用户有关所有配置                
     * （2）创建新用户（LOCAL类型），并给予工作区和ADMIN权限 ，给予权限要sleep
     * （3）进行所有审计类型操作                      （4）根据用户名和用户类型查询，查询用户审计日志是否为空 
     * （5） 用户类型修改为TOKEN     （6）再次执行步骤（3）（4）
     * （7）清除该用户配置，删除用户和角色
     */
    @Test
    public void test() throws ScmException, InterruptedException {
        String usernamepre = "audit_User";
        String password = "audit_Password";
      
        // 空配置无效 （LOCAL）
        long time = System.currentTimeMillis();
        String username = usernamepre + time;
        clearAudit(session, username);
        createUser(session, username, ScmUserPasswordType.LOCAL);
        sleep();
        ScmSession sessionTmp = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(sessionTmp, time);
        ScmCursor<ScmAuditInfo> cursor = ScmFactory.Audit.listInstance(session,
                new BasicBSONObject(ScmAttributeName.Audit.USERNAME, username)
                        .append(ScmAttributeName.Audit.USERTYPE, "LOCAL"));
        Assert.assertEquals(cursor.getNext(), null);
        cursor.close();

        // 空配置无效 （TOKEN）
        ScmUser user = ScmFactory.User.getUser(session, username);
        ScmFactory.User.alterUser(session, user, new ScmUserModifier()
                .setPasswordType(ScmUserPasswordType.TOKEN).setPassword(password, password));
        sessionTmp = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(sessionTmp, time);
        cursor = ScmFactory.Audit.listInstance(session,
                new BasicBSONObject(ScmAttributeName.Audit.USERNAME, username)
                        .append(ScmAttributeName.Audit.USERTYPE, "TOKEN"));
        Assert.assertEquals(cursor.getNext(), null);
        cursor.close();
        deleteUser(session, username);
        clearAuditUser(session, username);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        clearAudit(session, getScmUser());
        session.close();
    }
}
