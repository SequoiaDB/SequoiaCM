package com.sequoiacm.contentserver.privilege;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.exception.ScmOperationUnauthorizedException;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;
import com.sequoiacm.infrastructrue.security.core.ScmResource;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmWorkspaceResource;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ScmFileServicePriv {
    private static final Logger logger = LoggerFactory.getLogger(ScmFileServicePriv.class);

    private static ScmFileServicePriv instance = new ScmFileServicePriv();

    private ScmPrivClient client;
    private BucketInfoManager bucketInfoMgr;
    private IDirService dirService;

    private ScmFileServicePriv() {
    }

    public static ScmFileServicePriv getInstance() {
        return instance;
    }

    public void init(BucketInfoManager bucketInfoMgr, IDirService dirService, ScmPrivClient client,
            long hbInterval) {
        this.bucketInfoMgr = bucketInfoMgr;
        this.dirService = dirService;
        this.client = client;
        this.client.addResourceBuilder(new DirResourceBuilder());
        this.client.addResourceBuilder(new BucketResourceBuilder());
        this.client.loadAuth();
        this.client.updateHeartbeatInterval(hbInterval);
    }

    private boolean hasPriority(String user, String resourceType, String resource, int opFlag) {
        IResource r = createResource(resourceType, resource);
        if (null == r) {
            return false;
        }
        return client.check(user, r, opFlag);
    }

    public IResource createResource(String resourceType, String resource) {
        IResourceBuilder b = getResourceBuilder(resourceType);
        if (null != b) {
            return b.fromStringFormat(resource);
        }

        return null;
    }

    public IResourceBuilder getResourceBuilder(String resourceType) {
        return client.getResourceBuilder(resourceType);
    }

    public void grant(String token, ScmUser user, String roleName, IResource resource,
            String privilege) throws Exception {
        client.grant(token, user, roleName, resource, privilege);
    }

    public void revoke(String token, ScmUser user, String roleName, IResource resource,
            String privilege) throws Exception {
        client.revoke(token, user, roleName, resource, privilege);
    }

    public void checkWsPriority(ScmUser user, String wsName, ScmPrivilegeDefine op, String opDesc)
            throws ScmServerException {
        checkWsPriority(user, wsName, op.getFlag(), opDesc);
    }

    public void checkWsPriority(ScmUser user, String wsName, int op, String opDesc)
            throws ScmServerException {
        if (!hasWsPriority(user.getUsername(), wsName, op, opDesc)) {
            logger.error("do not have priority to {}:user={},ws={}", opDesc, user.getUsername(),
                    wsName);
            throw new ScmOperationUnauthorizedException(opDesc
                    + " failed, do not have priority:user=" + user.getUsername() + ",ws=" + wsName);
        }
    }

    private void checkWs(String wsName, String opDesc) throws ScmServerException {
        if (ScmContentModule.getInstance().getWorkspaceInfo(wsName) == null) {
            logger.error("workspace is not exist:ws={}", wsName);
            throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                    opDesc + " failed, workspace is not exist:ws=" + wsName);
        }
    }

    public void checkFilePriority(ScmUser user, String ws, BSONObject file, ScmPrivilegeDefine op,
            String opdesc) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfo(ws);
        if (wsInfo == null) {
            logger.error("workspace is not exist:ws={}", ws);
            throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                    opdesc + " failed, workspace is not exist:ws=" + ws);
        }
        Number bucketId = BsonUtils.getNumber(file, FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        String bucketName = null;
        if (bucketId != null) {
            ScmBucket bucket = bucketInfoMgr.getBucketById(bucketId.longValue());
            if (bucket != null) {
                bucketName = bucket.getName();
                if (hasBucketPriority(user.getUsername(), ws, bucket.getName(), op.getFlag())) {
                    return;
                }
            }
        }
        String dirPath = null;
        if (wsInfo.isEnableDirectory()) {
            String dirId = BsonUtils.getString(file, FieldName.FIELD_CLFILE_DIRECTORY_ID);
            dirPath = "/";
            if (null != dirId && !"".equals(dirId)) {
                dirPath = dirService.getDirPathById(ws, dirId);
            }
            if (hasDirPriority(user.getUsername(), ws, dirPath, op.getFlag())) {
                return;
            }
        }
        if (hasWsPriority(user.getUsername(), ws, op.getFlag(), opdesc)) {
            return;
        }
        throw new ScmOperationUnauthorizedException(opdesc + " failed, do not have priority:user="
                + user.getUsername() + ",ws=" + ws + ", bucket=" + bucketName + ", dir=" + dirPath);

    }

    public void checkBucketPriority(ScmUser user, String ws, String bucket, ScmPrivilegeDefine op,
            String opdesc) throws ScmServerException {
        checkWs(ws, opdesc);
        if (!hasBucketPriority(user.getUsername(), ws, bucket, op.getFlag())) {
            logger.error("do not have priority to {}:user={},ws={},bucket={}", opdesc,
                    user.getUsername(), ws, bucket);
            throw new ScmOperationUnauthorizedException(
                    opdesc + " failed, do not have priority:user=" + user.getUsername() + ",ws="
                            + ws + ",bucket=" + bucket);
        }
    }

    private boolean hasBucketPriority(String userName, String wsName, String bucket, int opFlag) {
        BucketResource bucketResource = new BucketResource(wsName, bucket);
        return hasPriority(userName, bucketResource.getType(), bucketResource.toStringFormat(),
                opFlag);
    }

    public void checkDirPriority(ScmUser user, String wsName, String dir, ScmPrivilegeDefine op,
            String opDesc) throws ScmServerException {
        checkWs(wsName, opDesc);
        checkDirPriority(user.getUsername(), wsName, dir, op.getFlag(), opDesc);
    }

    private void checkDirPriority(String userName, String wsName, String dir, int opFlag,
            String opDesc) throws ScmServerException {
        if (null == dir || dir.isEmpty()) {
            dir = "/";
        }

        if (!hasDirPriority(userName, wsName, dir, opFlag)) {
            logger.error("do not have priority to {}:user={},ws={},dir={}", opDesc, userName,
                    wsName, dir);
            throw new ScmOperationUnauthorizedException(
                    opDesc + " failed, do not have priority:user=" + userName + ",ws=" + wsName
                            + ",dir=" + dir);
        }
    }

    public void checkDirPriorityById(ScmUser user, String wsName,
            String dirId, ScmPrivilegeDefine op, String opDesc) throws ScmServerException {
        checkDirPriorityById(user, wsName, dirId, op.getFlag(), opDesc);
    }

    public void checkDirPriorityById(ScmUser user, String wsName,
            String dirId, int opFlag, String opDesc) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(wsName);
        if (!wsInfo.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to check priority of directory, directory feature is disable:ws="
                            + wsInfo.getName() + ", dirId=" + dirId);
        }

        // check workspace priority first
        if (hasWsPriority(user.getUsername(), wsName, opFlag, opDesc)) {
            return;
        }

        String dir = "/";
        if (null != dirId && !"".equals(dirId)) {
            dir = dirService.getDirPathById(wsName, dirId);
        }

        checkDirPriority(user.getUsername(), wsName, dir, opFlag, opDesc);
    }

    public void checkDirPriorityByOldDirAndNewParentId(ScmUser user, String wsName,
            IDirService dirService, String oldDir, String newParentId, ScmPrivilegeDefine op,
            String opDesc) throws ScmServerException {
        // check workspace priority first
        if (hasWsPriority(user.getUsername(), wsName, op.getFlag(), opDesc)) {
            return;
        }

        if (null == oldDir || "".equals(oldDir)) {
            oldDir = "/";
        }

        String name = ScmSystemUtils.basename(oldDir);

        String newParentDir = "/";
        if (null != newParentId && !"".equals(newParentId)) {
            newParentDir = dirService.getDirPathById(wsName, newParentId);
        }

        String newDir = ScmSystemUtils.generatePath(newParentDir, name);
        checkDirPriority(user.getUsername(), wsName, newDir, op.getFlag(), opDesc);
    }

    public void checkDirPriorityByOldDirAndNewParentDir(ScmUser user, String wsName, String oldDir,
            String newParentDir, ScmPrivilegeDefine op, String opDesc) throws ScmServerException {
        // check workspace priority first
        if (hasWsPriority(user.getUsername(), wsName, op.getFlag(), opDesc)) {
            return;
        }

        if (null == oldDir || "".equals(oldDir)) {
            oldDir = "/";
        }
        String name = ScmSystemUtils.basename(oldDir);

        if (null == newParentDir || "".equals(newParentDir)) {
            newParentDir = "/";
        }

        String newDir = ScmSystemUtils.generatePath(newParentDir, name);
        checkDirPriority(user.getUsername(), wsName, newDir, op.getFlag(), opDesc);
    }

    public void checkDirPriorityByOldIdAndNewParentId(ScmUser user, String wsName,
            IDirService dirService, String oldId, String newParentId, ScmPrivilegeDefine op,
            String opDesc) throws ScmServerException {
        // check workspace priority first
        if (hasWsPriority(user.getUsername(), wsName, op.getFlag(), opDesc)) {
            return;
        }

        String oldDir = "/";
        if (null != oldId && !"".equals(oldId)) {
            oldDir = dirService.getDirPathById(wsName, oldId);
        }

        String name = ScmSystemUtils.basename(oldDir);

        String newParentDir = "/";
        if (null != newParentId && !"".equals(newParentId)) {
            newParentDir = dirService.getDirPathById(wsName, newParentId);
        }

        String newDir = ScmSystemUtils.generatePath(newParentDir, name);
        checkDirPriority(user.getUsername(), wsName, newDir, op.getFlag(), opDesc);
    }

    public void checkDirPriorityByOldIdAndNewParentDir(ScmUser user, String wsName,
            IDirService dirService, String oldId, String newParentDir, ScmPrivilegeDefine op,
            String opDesc) throws ScmServerException {
        // check workspace priority first
        if (hasWsPriority(user.getUsername(), wsName, op.getFlag(), opDesc)) {
            return;
        }

        String oldDir = "/";
        if (null != oldId && !"".equals(oldId)) {
            oldDir = dirService.getDirPathById(wsName, oldId);
        }

        String name = ScmSystemUtils.basename(oldDir);

        if (null == newParentDir || "".equals(newParentDir)) {
            newParentDir = "/";
        }

        String newDir = ScmSystemUtils.generatePath(newParentDir, name);
        checkDirPriority(user.getUsername(), wsName, newDir, op.getFlag(), opDesc);
    }

    public void checkDirPriorityByOldDirAndNewName(ScmUser user, String wsName, String oldDir,
            String newName, ScmPrivilegeDefine op, String opDesc) throws ScmServerException {
        // check workspace priority first
        if (hasWsPriority(user.getUsername(), wsName, op.getFlag(), opDesc)) {
            return;
        }

        if (null == oldDir || oldDir.isEmpty()) {
            oldDir = "/";
        }

        String newDir = ScmSystemUtils.generatePath(ScmSystemUtils.dirname(oldDir), newName);
        checkDirPriority(user.getUsername(), wsName, newDir, op.getFlag(), opDesc);
    }

    public void checkDirPriorityByOldIdAndNewName(ScmUser user, String wsName,
            IDirService dirService, String oldId, String newName, ScmPrivilegeDefine op,
            String opDesc) throws ScmServerException {
        // check workspace priority first
        if (hasWsPriority(user.getUsername(), wsName, op.getFlag(), opDesc)) {
            return;
        }

        String oldDir = "/";
        if (null != oldId && !"".equals(oldId)) {
            oldDir = dirService.getDirPathById(wsName, oldId);
        }

        String newDir = ScmSystemUtils.generatePath(ScmSystemUtils.dirname(oldDir), newName);
        checkDirPriority(user.getUsername(), wsName, newDir, op.getFlag(), opDesc);
    }

    public void checkFilePriorityByFileId(ScmUser user, String wsName, IFileService fileService,
            String fileId, int majorVerion, int minorVersion, ScmPrivilegeDefine op, String opDesc)
            throws ScmServerException {
        // check workspace priority first
        if (hasWsPriority(user.getUsername(), wsName, op.getFlag(), opDesc)) {
            return;
        }
        BSONObject fileInfo = fileService.getFileInfoById(wsName, fileId, majorVerion, minorVersion,
                true);
        checkFilePriority(user, wsName, fileInfo, op, opDesc);
    }

    private boolean hasDirPriority(String userName, String wsName, String dir, int opFlag)
            throws ScmServerException {
        return hasPriority(userName, DirResource.RESOURCE_TYPE, wsName + ":" + dir, opFlag);
    }

    private boolean hasWsPriority(String userName, String wsName, int opFlag, String opDesc)
            throws ScmServerException {
        checkWs(wsName, opDesc);
        return hasPriority(userName, ScmWorkspaceResource.RESOURCE_TYPE, wsName, opFlag);
    }

    public Map<String, ScmResource> getResourceMapFromAuthServer(String workspace) {
        return client.getResourceMapFromAuthServer(workspace);
    }

    public List<ScmPrivilege> listPrivilegeByResource(String resourceType, String resource) {
        return client.listPrivilegeFromAuth(null, null, resourceType, resource);
    }

    public ScmRole findRoleById(String roleId) {
        return client.findRoleById(roleId);
    }

}
