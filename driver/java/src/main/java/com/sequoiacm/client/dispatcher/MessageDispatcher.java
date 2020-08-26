package com.sequoiacm.client.dispatcher;

import java.io.Closeable;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType.StatisticsType;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;

public interface MessageDispatcher extends Closeable {

    String login(String user, String password) throws ScmException;

    void logout() throws ScmException;

    BSONObject getSessionInfo(String sessionId) throws ScmException;

    BsonReader listSessions(String username) throws ScmException;

    void deleteSession(String sessionId) throws ScmException;

    BSONObject createRole(String roleName, String description) throws ScmException;

    BSONObject getRole(String roleName) throws ScmException;

    BSONObject getRoleById(String roleId) throws ScmException;

    BSONObject getResourceById(String resourceId) throws ScmException;

    BsonReader listRoles(BSONObject orderBy, long skip, long limit) throws ScmException;

    BsonReader listPrivilegesByRoleId(String roleId) throws ScmException;

    BsonReader listPrivilegesByResource(String type, String resource) throws ScmException;

    BsonReader listResourceByWorkspace(String workspaceName) throws ScmException;

    void deleteRole(String roleName) throws ScmException;

    BSONObject createUser(String username, ScmUserPasswordType passwordType, String password)
            throws ScmException;

    BSONObject getUser(String username) throws ScmException;

    BSONObject alterUser(String username, ScmUserModifier modifier) throws ScmException;

    BsonReader listUsers(BSONObject filter, long skip, long limit) throws ScmException;

    void deleteUser(String username) throws ScmException;

    BSONObject getWorkspace(String wsName) throws ScmException;

    BsonReader getWorkspaceList(BSONObject condition, BSONObject orderBy, long skip, long limit)
            throws ScmException;

    long countFile(String workspaceName, int scope, BSONObject condition) throws ScmException;

    BsonReader getFileList(String workspaceName, int scope, BSONObject condition,
            BSONObject orderby, long skip, long limit) throws ScmException;

    BsonReader getFileList(String workspaceName, int scope, BSONObject condition,
            BSONObject orderby, long skip, long limit, BSONObject selector) throws ScmException;

    BsonReader getSiteList(BSONObject condition) throws ScmException;

    BSONObject getFileInfo(String workspace_name, String fileId, String path, int majorVersion,
            int minorVersion) throws ScmException;

    BSONObject updateFileInfo(String workspaceName, String fileId, int majorVersion,
            int minorVersion, BSONObject fileInfo) throws ScmException;

    BSONObject uploadFile(String workspaceName, InputStream is, BSONObject fileInfo,
            BSONObject uploadConfig) throws ScmException;

    BSONObject uploadFile(String workspaceName, String breakpointFileName, BSONObject fileInfo,
            BSONObject uploadConfig) throws ScmException;

    HttpURLConnection getFileUploadConnection(String workspaceName, BSONObject fileInfo)
            throws ScmException;

    CloseableFileDataEntity downloadFile(String workspace_name, String fileId, int majorVersion,
            int minorVersion, int readFlag) throws ScmException;

    CloseableFileDataEntity downloadFile(String workspace_name, String fileId, int majorVersion,
            int minorVersion, int readFlag, long offset, int length) throws ScmException;

    void deleteFile(String workspaceName, String fileID, int majorVersion, int minorVersion,
            boolean isPhysical) throws ScmException;

    List<BSONObject> reloadBizConf(int scope, int id) throws ScmException;

    BsonReader getNodeList(BSONObject condition) throws ScmException;

    BsonReader getTaskList(BSONObject condition) throws ScmException;

    ScmId MsgStartTransferTask(String workspaceName, BSONObject condition, int scope,
            long maxExecTime, String targetSite) throws ScmException;

    ScmId MsgStartCleanTask(String workspaceName, BSONObject condition, int scope, long maxExecTime)
            throws ScmException;

    void MsgStopTask(ScmId taskId) throws ScmException;

    BSONObject MsgGetTask(ScmId taskId) throws ScmException;

    void asyncTransferFile(String workspaceName, ScmId fileId, int majorVersion, int minorVersion)
            throws ScmException;

    void asyncCacheFile(String workspaceName, ScmId fileId, int majorVersion, int minorVersion)
            throws ScmException;

    BSONObject getConfProperties(BSONObject keys) throws ScmException;

    BSONObject createBatch(String workspaceName, BSONObject batchInfo) throws ScmException;

    BSONObject getBatchInfo(String workspaceName, String batchId) throws ScmException;

    BsonReader getBatchList(String workspaceName, BSONObject filter, BSONObject orderBy, long skip,
            long limit) throws ScmException;

    void deleteBatch(String workspaceName, String batchID) throws ScmException;

    void updateBatchInfo(String workspaceName, String batchId, BSONObject batchInfo)
            throws ScmException;

    void batchAttachFile(String workspaceName, String batchId, String fileId) throws ScmException;

    void batchDetachFile(String workspaceName, String batchId, String fileId) throws ScmException;

    BSONObject createClass(String workspaceName, BSONObject classInfo) throws ScmException;

    BSONObject getClassInfo(String workspaceName, ScmId classId) throws ScmException;

    BSONObject getClassInfo(String name, String className) throws ScmException;

    BsonReader getClassList(String workspaceName, BSONObject filter) throws ScmException;

    void deleteClass(String workspaceName, ScmId classId) throws ScmException;

    void deleteClass(String workspaceName, String className) throws ScmException;

    BSONObject updateClassInfo(String workspaceName, ScmId classId, BSONObject classInfo)
            throws ScmException;

    void classAttachAttr(String workspaceName, ScmId classId, ScmId attrId) throws ScmException;

    void classDetachAttr(String workspaceName, ScmId classId, ScmId attrId) throws ScmException;

    BSONObject createAttribute(String workspaceName, BSONObject attrInfo) throws ScmException;

    BSONObject getAttributeInfo(String workspaceName, ScmId attrId) throws ScmException;

    BsonReader getAttributeList(String workspaceName, BSONObject filter) throws ScmException;

    void deleteAttribute(String workspaceName, ScmId attrId) throws ScmException;

    BSONObject updateAttributeInfo(String workspaceName, ScmId attrId, BSONObject attrInfo)
            throws ScmException;

    BSONObject createDir(String workspaceName, String dirName, String dirParentId, String path)
            throws ScmException;

    BSONObject getDir(String workspaceName, String dirId, String path) throws ScmException;

    void deleteDir(String workspaceName, String dirId, String path) throws ScmException;

    BsonReader getDirList(String workspaceName, BSONObject condition) throws ScmException;

    long moveDir(String workspaceName, String dirId, String newParentId) throws ScmException;

    long renameDir(String workspaceName, String dirId, String newName) throws ScmException;

    String getPath(String workspaceName, String dirId) throws ScmException;

    BSONObject createSchedule(String workspace, ScheduleType type, String name, String desc,
            BSONObject content, String cron, boolean enable) throws ScmException;

    BsonReader getScheduleList(BSONObject condition) throws ScmException;

    void deleteSchedule(String scheduleId) throws ScmException;

    BSONObject getSchedule(String scheduleId) throws ScmException;

    BSONObject updateSchedule(String scheduleId, BSONObject newValue) throws ScmException;

    BSONObject createBreakpointFile(String workspaceName, String fileName, long createTime,
            ScmChecksumType checksumType, InputStream fileStream, boolean isLastContent,
            boolean isNeedMd5) throws ScmException;

    BSONObject uploadBreakpointFile(String workspaceName, String fileName, InputStream fileStream,
            long offset, boolean isLastContent) throws ScmException;

    BSONObject getBreakpointFile(String workspaceName, String fileName) throws ScmException;

    BsonReader listBreakpointFiles(String workspaceName, BSONObject condition) throws ScmException;

    void deleteBreakpointFile(String workspaceName, String fileName) throws ScmException;

    BSONObject getPrivilegeById(String privilegeId) throws ScmException;

    void grantPrivilege(String roleName, String resourceType, String resource, String privilege)
            throws ScmException;

    void revokePrivilege(String roleName, String resourceType, String resource, String privilege)
            throws ScmException;

    BSONObject getPrivilegeMeta() throws ScmException;

    BSONObject updateFileContent(String workspaceName, String fileId, int majorVersion,
            int minorVersion, InputStream is, BSONObject option) throws ScmException;

    BSONObject updateFileContent(String workspaceName, String fileId, int majorVersion,
            int minorVersion, String breakFileName, BSONObject option) throws ScmException;

    HttpURLConnection getUpdateFileContentConn(String workspaceName, String fileId,
            int majorVersion, int minorVersion) throws ScmException;

    BsonReader getDirFileList(String workspaceName, String parentId, BSONObject condition, int skip,
            int limit, BSONObject orderby) throws ScmException;

    BSONObject createWorkspace(String workspaceName, BSONObject conf) throws ScmException;

    void deleteWorkspace(String wsName, boolean isEnforced) throws ScmException;

    BsonReader getAuditList(BSONObject filter) throws ScmException;

    BSONObject updateWorkspace(String wsName, BSONObject updator) throws ScmException;

    BsonReader listHealth(String serviceName) throws ScmException;

    BsonReader listHostInfo() throws ScmException;

    BsonReader showFlow() throws ScmException;

    BsonReader gaugeResponse() throws ScmException;

    long countSessions() throws ScmException;

    void refreshStatistics(StatisticsType type, String workspace) throws ScmException;

    BsonReader getStatisticsFileDeltaList(BSONObject condition) throws ScmException;

    BsonReader getStatisticsTrafficList(BSONObject condition) throws ScmException;

    BSONObject listServerInstance(String serviceName) throws ScmException;

    BSONObject updateConfProps(String targetType, List<String> targets, Map<String, String> props,
            List<String> deleteProps, boolean isAcceptUnknownProps) throws ScmException;

    void resetRemainUrl(String remainUrl);

    long countDir(String workspaceName, BSONObject condition) throws ScmException;

    long countBatch(String workspaceName, BSONObject condition) throws ScmException;

    String calcBreakpointFileMd5(String wsName, String breakpointFile) throws ScmException;

    String calcScmFileMd5(String wsName, String fileId, int majorVersion, int minorVersion)
            throws ScmException;


    void createFulltextIndex(String wsName, BSONObject fileCondition, ScmFulltextMode mode)
            throws ScmException;

    void dropFulltextIndex(String wsName) throws ScmException;

    void updateFulltextIndex(String wsName, BSONObject newFileCondition, ScmFulltextMode mode)
            throws ScmException;

    ScmFulltexInfo getWsFulltextIdxInfo(String wsName) throws ScmException;

    BsonReader fulltextSearch(String ws, int scope, BSONObject fileCondition,
            BSONObject contentCondition) throws ScmException;

    void rebuildFulltextIdx(String ws, String fileId) throws ScmException;

    void inspectFulltextIndex(String wsName) throws ScmException;

    BsonReader listFileWithFileIdxStatus(String ws, String status) throws ScmException;

    long countFileWithFileIdxStatus(String ws, String status) throws ScmException;
}
