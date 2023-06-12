package com.sequoiacm.cloud.authentication.serviceimpl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.cloud.authentication.dao.IPrivVersionDao;
import com.sequoiacm.cloud.authentication.dao.IResourceDao;
import com.sequoiacm.cloud.authentication.dao.IResourcePrivRelDao;
import com.sequoiacm.cloud.authentication.exception.ForbiddenException;
import com.sequoiacm.cloud.authentication.service.IPrivilegeService;
import com.sequoiacm.infrastructrue.security.core.ITransaction;
import com.sequoiacm.infrastructrue.security.core.ScmPrivMeta;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;
import com.sequoiacm.infrastructrue.security.core.ScmResource;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserRoleRepository;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.msg.role.RoleFilter;
import com.sequoiacm.infrastructure.config.core.msg.role.RoleUpdater;

@Service
public class ScmPrivilegeService implements IPrivilegeService {
    private static final Logger logger = LoggerFactory.getLogger(ScmPrivilegeService.class);

    @Autowired
    private ScmUserRoleRepository repository;

    @Autowired
    private IPrivVersionDao versionDao;

    @Autowired
    private IResourcePrivRelDao resourcePrivRelDao;

    @Autowired
    private IResourceDao resourceDao;

    @Autowired
    private ITransaction transactionFactory;

    @Autowired
    private ScmConfClient confClient;

    @Override
    public void grantPrivilege(String roleType, ScmRole role, String resourceType,
            String resource, String privilege) throws Exception {
        ITransaction t = transactionFactory.createTransation();
        boolean isModified = false;
        try {
            t.begin();
            ScmResource r = resourceDao.getResource(resourceType, resource);
            if (null == r) {
                r = new ScmResource(resourceDao.generateResourceId(), resourceType, resource);
                resourceDao.insertResource(r, t);
                isModified = true;
            }

            ScmPrivilege p = resourcePrivRelDao.getPrivilege(roleType, role.getRoleId(), r.getId());
            if (null == p) {
                p = new ScmPrivilege(resourcePrivRelDao.generatePrivilegeId(), roleType,
                        role.getRoleId(), r.getId(), privilege);
                resourcePrivRelDao.insertPrivilege(p, t);
                isModified = true;
            }
            else {
                if (!p.getPrivilege().equals(ScmPrivilegeDefine.ALL.getName())) {
                    // do not have all privilege yet
                    if (privilege.equals(ScmPrivilegeDefine.ALL.getName())) {
                        // grant all privilege
                        resourcePrivRelDao.updatePrivilegeValue(p.getId(),
                                ScmPrivilegeDefine.ALL.getName(), t);
                        isModified = true;
                    }
                    else {
                        String additionalPrivilege = getDifferentSet(privilege, p.getPrivilege());
                        if (null != additionalPrivilege) {
                            String newPrivilege = p.getPrivilege() + "|" + additionalPrivilege;
                            resourcePrivRelDao.updatePrivilegeValue(p.getId(), newPrivilege, t);
                            isModified = true;
                        }
                    }
                }
                else {
                    // already have all privilege. do nothing
                }
            }

            if (isModified) {
                versionDao.incVersion(t);
            }

            t.commit();
        }
        catch (Exception e) {
            t.rollback();
            throw e;
        }
        if (isModified) {
            sendRoleChangeEvents(role.getRoleName(), EventType.UPDATE);
        }
    }

    private boolean isInArray(String[] array, String fetching) {
        for (String item : array) {
            if (fetching.equals(item)) {
                return true;
            }
        }

        return false;
    }

    private String getDifferentSet(String left, String right) {
        String[] leftArray = left.split("\\|");
        String[] rightArray = right.split("\\|");

        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (String leftItem : leftArray) {
            if (!isInArray(rightArray, leftItem)) {
                if (0 == count) {
                    sb.append(leftItem);
                }
                else {
                    sb.append("|").append(leftItem);
                }

                count++;
            }
        }

        if (count > 0) {
            return sb.toString();
        }

        return null;
    }

    @Override
    public void revokePrivilege(String roleType, ScmRole role, String resourceType,
            String resource, String privilege) throws Exception {
        ScmResource r = resourceDao.getResource(resourceType, resource);
        if (null == r) {
            return;
        }

        ScmPrivilege p = resourcePrivRelDao.getPrivilege(roleType, role.getRoleId(), r.getId());
        if (null == p) {
            return;
        }

        // can't revoke privilege when privilege is ALL
        if (p.getPrivilege().equals(ScmPrivilegeDefine.ALL.getName())
                && !privilege.equals(ScmPrivilegeDefine.ALL.getName())) {
            throw new ForbiddenException(
                    "can't revoke sub privilege from All privilege:subprivilege=" + privilege);
        }

        ITransaction t = transactionFactory.createTransation();
        try {
            t.begin();
            if (privilege.equals(ScmPrivilegeDefine.ALL.getName())) {
                // revoke all privilege
                resourcePrivRelDao.deletePrivilege(p, t);
                removeResourceIfNoRelation(r, t);
                versionDao.incVersion(t);
            }
            else {
                String newPrivilege = getDifferentSet(p.getPrivilege(), privilege);
                if (null == newPrivilege) {
                    // privilege is empty, delete the privilege
                    resourcePrivRelDao.deletePrivilege(p, t);
                    removeResourceIfNoRelation(r, t);
                    versionDao.incVersion(t);
                }
                else if (!p.getPrivilege().equals(newPrivilege)) {
                    resourcePrivRelDao.updatePrivilegeValue(p.getId(), newPrivilege, t);
                    versionDao.incVersion(t);
                }
                else {
                    // p.getPrivilege().equals(newPrivilege)
                    // revoke not-exist privilege, do nothing
                }
            }
            t.commit();
        }
        catch (Exception e) {
            t.rollback();
            throw e;
        }
        sendRoleChangeEvents(role.getRoleName(), EventType.UPDATE);
    }

    private void removeResourceIfNoRelation(ScmResource r, ITransaction t) {
        List<ScmPrivilege> privList = resourcePrivRelDao.listPrivilegesByResourceId(r.getId());
        if (null == privList || privList.size() == 0) {
            resourceDao.deleteResource(r, t);
        }
    }

    @Override
    public ScmPrivMeta getMeta() {
        return new ScmPrivMeta(versionDao.getVersion());
    }

    @Override
    public List<ScmResource> getResourceList() {
        return resourceDao.listResources();
    }

    @Override
    public List<ScmResource> getResourceListByWorkspace(String workspaceName) {
        return resourceDao.listResourcesByWorkspace(workspaceName);
    }

    @Override
    public ScmResource getResourceById(String resourceId) {
        return resourceDao.getResourceById(resourceId);
    }

    @Override
    public List<ScmPrivilege> getPrivilegeList() {
        return resourcePrivRelDao.listPrivileges();
    }

    @Override
    public List<ScmPrivilege> getPrivilegeListByRoleId(String roleId) {
        return resourcePrivRelDao.listPrivilegesByRoleId(roleId);
    }

    @Override
    public ScmPrivilege getPrivilegeById(String privilegeId) {
        return resourcePrivRelDao.getPrivilegeById(privilegeId);
    }

    @Override
    public List<ScmPrivilege> getPrivilegeListByRoleName(String roleName) {
        String innerRoleName = roleName;
        if (!roleName.startsWith(ScmRole.ROLE_NAME_PREFIX)) {
            innerRoleName = ScmRole.ROLE_NAME_PREFIX + roleName;
        }

        ScmRole role = repository.findRoleByName(innerRoleName);
        if (role == null) {
            return new ArrayList<ScmPrivilege>();
        }

        return resourcePrivRelDao.listPrivilegesByRoleId(role.getRoleId());
    }

    @Override
    public List<ScmPrivilege> getPrivilegeListByResourceId(String resourceId) {
        return resourcePrivRelDao.listPrivilegesByResourceId(resourceId);
    }

    @Override
    public List<ScmPrivilege> getPrivilegeListByResource(String resourceType, String resource) {
        ScmResource r = resourceDao.getResource(resourceType, resource);
        if (r == null) {
            return new ArrayList<ScmPrivilege>();
        }
        return getPrivilegeListByResourceId(r.getId());
    }

    @Override
    public void deleteRole(ScmRole role) throws Exception {
        ITransaction t = transactionFactory.createTransation();
        try {
            t.begin();

            List<ScmUser> users = repository.findUsersByRoleName(role.getRoleName());
            for (ScmUser user : users) {
                repository.deleteRoleFromUser(user, role.getRoleId(), t);
            }

            repository.deleteRole(role, t);
            // delete privilege
            List<ScmPrivilege> privList = resourcePrivRelDao.listPrivilegesByRoleId(role
                    .getRoleId());
            for (ScmPrivilege priv : privList) {
                resourcePrivRelDao.deletePrivilege(priv, t);

                // check and delete resource
                List<ScmPrivilege> tmpList = resourcePrivRelDao.listPrivilegesByResourceId(priv
                        .getResourceId());
                if (null == tmpList || tmpList.size() == 0) {
                    resourceDao
                            .deleteResource(new ScmResource(priv.getResourceId(), null, null), t);
                }
            }

            versionDao.incVersion(t);

            t.commit();
        }
        catch (Exception e) {
            t.rollback();
            throw e;
        }
        sendRoleChangeEvents(role.getRoleName(), EventType.DELTE);
    }

    private void sendRoleChangeEvents(String roleName, EventType eventType) {
        try {
            if (eventType == EventType.UPDATE) {
                confClient.updateConfig(ScmBusinessTypeDefine.ROLE, new RoleUpdater(roleName), false);
            }
            else if (eventType == EventType.DELTE) {
                confClient.deleteConf(ScmBusinessTypeDefine.ROLE, new RoleFilter(roleName), false);
            }
        }
        catch (Exception e) {
            logger.warn("Failed to send to the config server of role " + eventType + " event", e);
        }
    }
}
