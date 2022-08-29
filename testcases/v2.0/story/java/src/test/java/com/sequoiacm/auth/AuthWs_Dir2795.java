package com.sequoiacm.auth;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @Description: SCM-2795:对角色授权，目录资源非格式化
 * @author fanyu
 * @Date:2018年6月15日
 * @version:1.0
 */
public class AuthWs_Dir2795 extends TestScmBase {
    private boolean runSuccess;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private ScmWorkspace wsA;
    private String username = "user2795";
    private String rolename = "role2795";
    private String passwd = "2795";
    private ScmUser user;
    private ScmRole role;
    private String[] dirPaths = { "/2795a", "/2795a/b" };
    // 目录资源列表 Rs = Resource
    private String[] dirRsList = { "//2795a", "/2795a//b", "/2795a/b//",
            "//2795a/b/", "//", "///" };
    // 目录资源dirRSList对应的目录，方便后面测试
    private String[] dirRsToPath = { "/2795a/", "/2795a/b/", "/2795a/b/",
            "/2795a/b/", "/", "/" };

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        sessionA = TestScmTools.createSession( site );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        prepareUser();
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        // 创建目录
        for ( String dir : dirPaths ) {
            if ( !ScmFactory.Directory.isInstanceExist( wsA, dir ) ) {
                ScmFactory.Directory.createInstance( wsA, dir );
            }
        }
        for ( int i = 0; i < dirRsList.length; i++ ) {
            ScmResource resource = ScmResourceFactory
                    .createDirectoryResource( wsp.getName(), dirRsList[ i ] );
            ScmFactory.Role.grantPrivilege( sessionA, role, resource,
                    ScmPrivilegeType.READ );
            ScmAuthUtils.checkPriority( site, username, passwd, role,
                    wsp.getName() );
            // 检查权限
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site, username, passwd );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // 有权限读取目录
                ScmDirectory directory = ScmFactory.Directory.getInstance( ws,
                        dirRsToPath[ i ] );
                // 简单校验
                Assert.assertEquals( directory.getPath(), dirRsToPath[ i ] );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
            ScmFactory.Role.revokePrivilege( sessionA, role, resource,
                    ScmPrivilegeType.READ );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ScmFactory.Role.deleteRole( sessionA, role );
                ScmFactory.User.deleteUser( sessionA, user );
                for ( int i = dirPaths.length - 1; i >= 0; i-- ) {
                    ScmFactory.Directory.deleteInstance( wsA, dirPaths[ i ] );
                }
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void prepareUser() throws ScmException {
        try {
            ScmFactory.Role.deleteRole( sessionA, rolename );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
        try {
            ScmFactory.User.deleteUser( sessionA, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
        user = ScmFactory.User.createUser( sessionA, username,
                ScmUserPasswordType.LOCAL, passwd );
        role = ScmFactory.Role.createRole( sessionA, rolename, null );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( role );
        ScmFactory.User.alterUser( sessionA, user, modifier );
    }
}
