package com.sequoiacm.tmp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;

public class TestPrivilegeCommon {
    private final static Logger logger = LoggerFactory.getLogger(TestPrivilegeCommon.class);

    public static ScmUser createUser(ScmSession ss, String userName, String passwd)
            throws ScmException {
        return ScmFactory.User.createUser(ss, userName, ScmUserPasswordType.LOCAL, passwd);
    }

    public static void deleteUser(ScmSession ss, String userName) throws ScmException {
        try {
            ScmFactory.User.deleteUser(ss, userName);
        }
        catch (Exception e) {
            logger.error("delete user failed:userName=" + userName, e);
        }
    }

    public static ScmRole createRole(ScmSession ss, String roleName) throws ScmException {
        return ScmFactory.Role.createRole(ss, roleName, "");
    }

    public static ScmUser associateRole(ScmSession ss, ScmUser user, ScmRole role)
            throws ScmException {
        ScmUserModifier m = new ScmUserModifier();
        m.addRole(role);
        return ScmFactory.User.alterUser(ss, user, m);
    }

    public static void deleteRole(ScmSession ss, String roleName) throws ScmException {
        try {
            ScmFactory.Role.deleteRole(ss, roleName);
        }
        catch (Exception e) {
            logger.error("delete role failed:roleName=" + roleName, e);
        }
    }

    public static void grantPrivilege(ScmSession ss, ScmRole role, String resourceType,
            String resource, String privilege) throws ScmException {
        ScmResource r = ScmResourceFactory.createResource(resourceType, resource);
        ScmFactory.Role.grantPrivilege(ss, role, r, privilege);
    };

    public static void revokePrivilege(ScmSession ss, ScmRole role, String resourceType,
            String resource, String privilege) throws ScmException {
        ScmResource r = ScmResourceFactory.createResource(resourceType, resource);
        ScmFactory.Role.revokePrivilege(ss, role, r, privilege);
    }

    public static void createDir(String url, String userName, String passwd, String workspaceName,
            String path) throws ScmException {
        ScmSession tmpSession = null;
        try {
            tmpSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, userName, passwd));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, tmpSession);
            ScmFactory.Directory.createInstance(ws, path);
        }
        finally {
            if (null != tmpSession) {
                tmpSession.close();
            }
        }
    }

    public static void getDir(String url, String userName, String passwd, String workspaceName,
            String path) throws ScmException {
        ScmSession tmpSession = null;
        try {
            tmpSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, userName, passwd));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, tmpSession);
            ScmFactory.Directory.getInstance(ws, path);
        }
        finally {
            if (null != tmpSession) {
                tmpSession.close();
            }
        }
    }

    public static void deleteDir(String url, String userName, String passwd, String workspaceName,
            String path) throws ScmException {
        ScmSession tmpSession = null;
        try {
            tmpSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, userName, passwd));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, tmpSession);
            ScmFactory.Directory.deleteInstance(ws, path);
        }
        finally {
            if (null != tmpSession) {
                tmpSession.close();
            }
        }
    }

    public static void deleteDirSilence(ScmSession adminSS, String workspaceName, String path) {
        ScmWorkspace ws;
        try {
            ws = ScmFactory.Workspace.getWorkspace(workspaceName, adminSS);
            ScmFactory.Directory.deleteInstance(ws, path);
        }
        catch (Exception e) {
            logger.error("delete path failed:workspaceName=" + workspaceName + ",path=" + path, e);
        }
    }

    public static void rename(String url, String userName, String passwd, String workspaceName,
            String srcPath, String newName) throws ScmException {
        ScmSession tmpSession = null;
        try {
            tmpSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, userName, passwd));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, tmpSession);
            ScmDirectory d = ScmFactory.Directory.getInstance(ws, srcPath);
            d.rename(newName);
        }
        finally {
            if (null != tmpSession) {
                tmpSession.close();
            }
        }
    }

    public static void moveDir(String url, String userName, String passwd, String workspaceName,
            String srcPath, String targetParent) throws ScmException {
        ScmSession tmpSession = null;
        try {
            tmpSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, userName, passwd));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, tmpSession);
            ScmDirectory dSrc = ScmFactory.Directory.getInstance(ws, srcPath);
            ScmDirectory dTargetParent = ScmFactory.Directory.getInstance(ws, targetParent);
            dSrc.move(dTargetParent);
        }
        finally {
            if (null != tmpSession) {
                tmpSession.close();
            }
        }
    }
}
