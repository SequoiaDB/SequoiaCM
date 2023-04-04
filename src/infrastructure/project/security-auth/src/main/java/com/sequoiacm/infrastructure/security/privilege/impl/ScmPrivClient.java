package com.sequoiacm.infrastructure.security.privilege.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.infrastructrue.security.core.*;
import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScmPrivClient {
    private static final Logger logger = LoggerFactory.getLogger(ScmPrivClient.class);

    private Map<String, IResourceBuilder> resourceBuilderMap = new HashMap<>();

    private ScmPrivService privService;

    private ScmPrivMgr privilegeMgr = new ScmPrivMgr();

    private ScmTimer timer = null;
    private long heartbeatInterval = 3600 * 1000;
    private static final long MIN_HEARTBEAT_INTERVAL = 1000;

    @Autowired
    public ScmPrivClient(ScmFeignClient feignClient) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ScmRole.class, new ScmRoleJsonDeserializer());
        module.addDeserializer(ScmUser.class, new ScmUserJsonDeserializer());
        module.addDeserializer(ScmPrivilege.class, new ScmPrivilegeDeserilizer());
        module.addDeserializer(ScmPrivMeta.class, new ScmPrivMetaDeSerializer());
        module.addDeserializer(ScmResource.class, new ScmResourceDeserializer());
        mapper.registerModule(module);

        privService = feignClient.builder().objectMapper(mapper).serviceTarget(ScmPrivService.class,
                "auth-server");

        addResourceBuilder(new WsResourceBuilder());
        addResourceBuilder(new WsAllResourceBuilder());
    }

    @PreDestroy
    public void destroy() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public synchronized void loadAuth() {
        try {
            reload();
            logger.info("privielege version={}", getVersion());
        }
        catch (Exception e) {
            logger.warn("init privilege from auth-server failed", e);
        }
    }

    public boolean check(String user, IResource resource, int op) {
        return privilegeMgr.check(user, resource, op);
    }

    public boolean hasAllPriority(String user, ScmWorkspaceResource wsResource) {
        return privilegeMgr.hasAllPriority(user, wsResource);
    }

    public void addResourceBuilder(IResourceBuilder builder) {
        resourceBuilderMap.put(builder.getResourceType(), builder);
    }

    public IResourceBuilder getResourceBuilder(String resourceType) {
        return resourceBuilderMap.get(resourceType);
    }

    public int getVersion() {
        return privilegeMgr.getVersion();
    }

    int getVersionFromAuthServer() {
        return privService.getPrivMeta().getVersion();
    }

    Map<String, List<String>> getRoleUserRelFromAuthServer() {
        Map<String, List<String>> roleUserMap = new HashMap<>();

        List<ScmUser> userList = privService.findAllUsers(null, null, null);
        if (null != userList) {
            for (ScmUser user : userList) {
                if (null != user.getAuthorities() && user.isEnabled()) {
                    for (ScmRole role : user.getAuthorities()) {
                        String roleId = role.getRoleId();
                        List<String> userNameList = roleUserMap.get(roleId);
                        if (null == userNameList) {
                            userNameList = new ArrayList<>();
                            roleUserMap.put(roleId, userNameList);
                        }

                        userNameList.add(user.getUsername());
                    }
                }
            }
        }

        return roleUserMap;
    }

    public Map<String, ScmResource> getResourceMapFromAuthServer(String workspace) {
        Map<String, ScmResource> resourceMap = new HashMap<>();

        List<ScmResource> resourceList = privService.listResources(workspace);
        for (ScmResource resource : resourceList) {
            resourceMap.put(resource.getId(), resource);
        }

        return resourceMap;
    }

    void reload() {
        int version = getVersionFromAuthServer();
        ScmPrivMgr tmp = createNewPrivMgr(version);

        int newVersion = getVersionFromAuthServer();
        int oldVersion = tmp.getVersion();
        while (newVersion != oldVersion) {
            tmp.clear();
            tmp = createNewPrivMgr(newVersion);
            newVersion = getVersionFromAuthServer();
            oldVersion = tmp.getVersion();
        }

        privilegeMgr = tmp;
    }

    private ScmPrivMgr createNewPrivMgr(int version) {
        ScmPrivMgr tmp = new ScmPrivMgr();
        // role_id <-> userNameList
        Map<String, List<String>> roleIdUserMap = getRoleUserRelFromAuthServer();
        // resource_id <-> resource
        Map<String, ScmResource> resourceMap = getResourceMapFromAuthServer(null);

        List<ScmPrivilege> privilegeList = privService.listPrivileges(null, null, null, null);

        for (ScmPrivilege privilege : privilegeList) {
            if (!privilege.getRoleType().equals("role")) {
                logger.warn(
                        "ignore privilege due to roleType:id={},roleType={},roleId={},resourceId={}",
                        privilege.getId(), privilege.getRoleType(), privilege.getRoleId(),
                        privilege.getResourceId());
                continue;
            }

            List<String> userList = roleIdUserMap.get(privilege.getRoleId());
            if (null == userList || userList.size() == 0) {
                logger.warn(
                        "ignore privilege due to no user being attached roleId:id={},roleType={},roleId={},resourceId={}",
                        privilege.getId(), privilege.getRoleType(), privilege.getRoleId(),
                        privilege.getResourceId());
                continue;
            }
            for (String userName : userList) {
                ScmResource resource = resourceMap.get(privilege.getResourceId());
                if (null == resource) {
                    logger.warn(
                            "ignore privilege due to resourceId:id={},roleType={},roleId={},resourceId={}",
                            privilege.getId(), privilege.getRoleType(), privilege.getRoleId(),
                            privilege.getResourceId());
                    continue;
                }

                IResourceBuilder builder = resourceBuilderMap.get(resource.getType());
                if (null == builder) {
                    logger.warn("ignore privilege due to resourceType:id={},type={},resource={}",
                            resource.getId(), resource.getType(), resource.getResource());
                    continue;
                }

                IResource iresource = builder.fromStringFormat(resource.getResource());
                int intPriv = privToInt(privilege.getPrivilege());
                if (0 != intPriv) {
                    tmp.addPrivilege(userName, iresource, intPriv, builder);
                }
            }
        }

        tmp.setVersion(version);
        return tmp;
    }

    private int privToInt(String privilege) {
        int intPriv = 0;
        String[] privArray = privilege.split("\\|");
        for (String onePriv : privArray) {
            onePriv = onePriv.trim();
            if (onePriv.length() == 0) {
                continue;
            }

            ScmPrivilegeDefine enPriv = ScmPrivilegeDefine.getEnum(onePriv);
            if (null != enPriv) {
                intPriv = intPriv | enPriv.getFlag();
            }
            else {
                logger.warn("unreconigzed privilege:onePriv={}", onePriv);
            }
        }

        return intPriv;
    }

    public synchronized void updateHeartbeatInterval(long interval) {
        if (interval < MIN_HEARTBEAT_INTERVAL) {
            logger.warn("interval is too small, reset to min:interval={},min={}", interval,
                    MIN_HEARTBEAT_INTERVAL);
            interval = MIN_HEARTBEAT_INTERVAL;
        }

        ScmTimer t = createNewTimer(interval);
        heartbeatInterval = interval;

        if (null != timer) {
            timer.cancel();
        }

        timer = t;
        logger.info("privilege's heartbeat is started:interval={}", heartbeatInterval);
    }

    private ScmTimer createNewTimer(long interval) {
        ScmTimer t = ScmTimerFactory.createScmTimer();
        ScmPrivHeartbeatTask task = new ScmPrivHeartbeatTask(this);
        t.schedule(task, 0, interval);
        return t;
    }

    private boolean isAdminRole(ScmUser user) {
        for (ScmRole role : user.getAuthorities()) {
            if (role.isAuthAdmin()) {
                return true;
            }
        }

        return false;
    }

    public void grant(String token, ScmUser user, String roleName, IResource resource,
            String privilege) throws Exception {
        String formatedPriv = checkPrivilege(privilege);
        if (null == formatedPriv) {
            throw new Exception("privilege is invalid:privilege=" + privilege);
        }

        if (!isAdminRole(user)) {
            ScmWorkspaceResource wsResource = getWorkspaceResource(resource);
            if (!check(user.getUsername(), wsResource, ScmPrivilegeDefine.ALL.getFlag())) {
                throw new Exception(
                        "user has not priority to grant:user=" + user.getUsername() + ",resource="
                                + wsResource + ",expected privilege=" + ScmPrivilegeDefine.ALL);
            }
        }

        privService.grant(token, roleName, resource.getType(), resource.toStringFormat(),
                formatedPriv);
    }

    private ScmWorkspaceResource getWorkspaceResource(IResource resource) {
        if (ScmWorkspaceResource.RESOURCE_TYPE.equals(resource.getType())) {
            return (ScmWorkspaceResource) resource;
        }
        else {
            return new ScmWorkspaceResource(resource.getWorkspace());
        }
    }

    public void revoke(String token, ScmUser user, String roleName, IResource resource,
            String privilege) throws Exception {
        String formatedPriv = checkPrivilege(privilege);
        if (null == formatedPriv) {
            throw new Exception("privilege is invalid:privilege=" + privilege);
        }

        if (!isAdminRole(user)) {
            if (!check(user.getUsername(), resource, ScmPrivilegeDefine.ALL.getFlag())) {
                throw new Exception(
                        "user has not priority to grant:user=" + user.getUsername() + ",resource="
                                + resource + ",expected privilege=" + ScmPrivilegeDefine.ALL);
            }
        }

        privService.revoke(token, roleName, resource.getType(), resource.toStringFormat(),
                formatedPriv);
    }

    private String checkPrivilege(String privilege) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        boolean existAll = false;
        String[] privArray = privilege.split("\\|");
        for (String onePriv : privArray) {
            onePriv = onePriv.trim();
            if (onePriv.length() == 0) {
                continue;
            }

            ScmPrivilegeDefine enPriv = ScmPrivilegeDefine.getEnum(onePriv);
            if (null != enPriv) {
                if (enPriv.equals(ScmPrivilegeDefine.ALL)) {
                    existAll = true;
                }

                if (count > 0) {
                    sb.append("|").append(onePriv);
                }
                else {
                    sb.append(onePriv);
                }

                count++;
            }
            else {
                logger.warn("unrecognized privilege:onePriv={},privilege={}", onePriv, privilege);
                return null;
            }
        }

        if (0 == count) {
            logger.warn("unrecognized privilege:privilege={}", privilege);
            return null;
        }

        if (count > 1 && existAll) {
            logger.warn("can't mix privilege with ALL and others:privilege={}", privilege);
            return null;
        }

        return sb.toString();
    }

    public List<ScmPrivilege> listPrivilegeFromAuth(String roleId, String roleName,
            String resourceType, String resource) {
        return privService.listPrivileges(roleId, roleName, resourceType, resource);
    }

    public ScmRole findRoleById(String roleId) {
        return privService.findRoleById(roleId);
    }
}
