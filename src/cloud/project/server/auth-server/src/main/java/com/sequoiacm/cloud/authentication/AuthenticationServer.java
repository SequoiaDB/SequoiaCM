package com.sequoiacm.cloud.authentication;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import com.sequoiacm.cloud.authentication.service.IPrivilegeService;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserPasswordType;
import com.sequoiacm.infrastructrue.security.core.ScmUserRoleRepository;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructrue.security.privilege.ScmResourceTypeDefine;
import com.sequoiacm.infrastructure.audit.EnableAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditPropsVerifier;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.verifier.PreventingModificationVerifier;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

@EnableDiscoveryClient
@SpringBootApplication
@EnableScmMonitorServer
@EnableConfClient
@EnableAudit
public class AuthenticationServer implements ApplicationRunner {

    @Autowired
    private ScmConfClient confClient;

    @Autowired
    private ScmUserRoleRepository userRoleDao;

    @Autowired
    private IPrivilegeService privService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public static void main(String[] args) {
        new SpringApplicationBuilder(AuthenticationServer.class).web(true).run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // first register have higher priority.
        confClient.registerConfigPropVerifier(new ScmAuditPropsVerifier());
        confClient.registerConfigPropVerifier(new PreventingModificationVerifier("scm."));

        initDefaultUserRole();
    }

    private void initDefaultUserRole() throws Exception {
        final String DEFAULT_ADMIN_USER = "admin";
        final String DEFAULT_MONITOR_USER = "monitor";

        List<ScmUser> users = userRoleDao.findAllUsers(null, null, null, null, 0, -1);
        if (!users.isEmpty()) {
            return;
        }

        ScmRole authAdminRole = ScmRole.withRoleName(ScmRole.AUTH_ADMIN_ROLE_NAME)
                .roleId(userRoleDao.generateRoleId()).description("authentication administrator")
                .build();
        initDefaultRole(authAdminRole);

        ScmUser authAdminUser = ScmUser.withUsername(DEFAULT_ADMIN_USER)
                .userId(userRoleDao.generateUserId()).passwordType(ScmUserPasswordType.LOCAL)
                .disabled(false).roles(authAdminRole)
                .password(passwordEncoder.encodePassword(DEFAULT_ADMIN_USER, null)).build();
        initDefaultUser(authAdminUser);

        ScmRole monitorRole = ScmRole.withRoleName(ScmRole.AUTH_MONITOR_ROLE_NAME)
                .roleId(userRoleDao.generateRoleId()).description("authentication monitor").build();
        initDefaultRole(monitorRole);

        ScmUser monitorUser = ScmUser.withUsername(DEFAULT_MONITOR_USER)
                .userId(userRoleDao.generateUserId()).passwordType(ScmUserPasswordType.LOCAL)
                .disabled(false).roles(monitorRole)
                .password(passwordEncoder.encodePassword(DEFAULT_MONITOR_USER, null)).build();
        initDefaultUser(monitorUser);

        privService.grantPrivilege(ScmPrivilege.JSON_VALUE_ROLE_TYPE_ROLE, monitorRole.getRoleId(),
                ScmResourceTypeDefine.TYPE_WS_ALL, ScmResourceTypeDefine.TYPE_WS_ALL,
                ScmPrivilegeDefine.LOW_LEVEL_READ.getName());
    }

    private void initDefaultUser(ScmUser user) {
        try {
            userRoleDao.insertUser(user);
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                throw e;
            }
        }
    }

    private void initDefaultRole(ScmRole role) {
        try {
            userRoleDao.insertRole(role);
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                throw e;
            }
        }
    }
}
