package com.sequoiacm.client.dispatcher;

import java.io.Closeable;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.element.ScmCheckConnTarget;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import org.bson.BSONObject;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType.StatisticsType;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import org.bson.types.BasicBSONList;

public interface MessageDispatcher extends Closeable {

    String login(String user, String password) throws ScmException;

    void logout() throws ScmException;

    BSONObject getSessionInfo(String sessionId) throws ScmException;

    BsonReader listSessions(String username) throws ScmException;

    void deleteSession(String sessionId) throws ScmException;

    BSONObject createRole(String roleName, String description) throws ScmException;

    BSONObject getRole(String roleName) throws ScmException;

    BSONObject getRoleById(String roleId) throws ScmException;

    long countRole(BSONObject condition) throws ScmException;

    BSONObject getResourceById(String resourceId) throws ScmException;

    BsonReader listRoles(BSONObject filter, BSONObject orderBy, long skip, long limit)
            throws ScmException;

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


    BsonReader getSiteList(BSONObject condition, long skip, long limit) throws ScmException;

    long countSite(BSONObject condition) throws ScmException;

    BSONObject getFileInfo(String workspace_name, String fileId, String path, int majorVersion,
            int minorVersion) throws ScmException;

    BSONObject updateFileInfo(String workspaceName, String fileId, int majorVersion,
            int minorVersion, BSONObject fileInfo) throws ScmException;

    BSONObject uploadFile(String workspaceName, InputStream is, BSONObject fileInfo,
            BSONObject uploadConfig) throws ScmException;

    BSONObject uploadFile(String workspaceName, String breakpointFileName, BSONObject fileInfo,
            BSONObject uploadConfig) throws ScmException;

    HttpURLConnection getFileUploadConnection(String workspaceName, BSONObject fileInfo,
            BSONObject uploadConf) throws ScmException;

    CloseableFileDataEntity downloadFile(String workspace_name, String fileId, int majorVersion,
            int minorVersion, int readFlag) throws ScmException;

    CloseableFileDataEntity downloadFile(String workspace_name, String fileId, int majorVersion,
            int minorVersion, int readFlag, long offset, int length) throws ScmException;

    void deleteFile(String workspaceName, String fileID, int majorVersion, int minorVersion,
            boolean isPhysical) throws ScmException;

    void deleteFileByPath(String workspaceName, String filePath, int majorVersion, int minorVersion,
            boolean isPhysical) throws ScmException;

    List<BSONObject> reloadBizConf(int scope, int id) throws ScmException;

    BsonReader getNodeList(BSONObject condition) throws ScmException;

    BsonReader getTaskList(BSONObject condition) throws ScmException;

    BsonReader getTaskList(BSONObject condition, BSONObject orderby, BSONObject selector, long skip,
            long limit) throws ScmException;

    ScmId MsgStartTransferTask(String workspaceName, BSONObject condition, int scope,
            long maxExecTime, String targetSite, String dataCheckLevel, boolean quickStart)
            throws ScmException;

    ScmId MsgStartCleanTask(String workspaceName, BSONObject condition, int scope, long maxExecTime,
            String dataCheckLevel, boolean quickStart, boolean isRecycleSpace)
            throws ScmException;

    ScmId MsgStartMoveTask(String workspaceName, BSONObject condition, int scope, long maxExecTime,
            String targetSite, String dataCheckLevel, boolean quickStart, boolean isRecycleSpace)
            throws ScmException;

    ScmId MsgStartSpaceRecyclingTask(String workspaceName, long maxExecTime, String recycleScope)
            throws ScmException;

    void MsgStopTask(ScmId taskId) throws ScmException;

    BSONObject MsgGetTask(ScmId taskId) throws ScmException;

    void asyncTransferFile(String workspaceName, ScmId fileId, int majorVersion, int minorVersion,
            String targetSite) throws ScmException;

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

    BsonReader getClassList(String workspaceName, BSONObject filter, BSONObject orderby, int skip,
            int limit) throws ScmException;

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

    BsonReader getDirList(String workspaceName, BSONObject condition, BSONObject orderby, int skip,
            int limit) throws ScmException;

    long moveDir(String workspaceName, String dirId, String newParentId) throws ScmException;

    long renameDir(String workspaceName, String dirId, String newName) throws ScmException;

    String getPath(String workspaceName, String dirId) throws ScmException;

    BSONObject createSchedule(String workspace, ScheduleType type, String name, String desc,
                              BSONObject content, String cron, boolean enable, String preferredRegion,
                              String preferredZone) throws ScmException;

    BsonReader getScheduleList(BSONObject condition, BSONObject orderby, long skip, long limit)
            throws ScmException;

    void deleteSchedule(String scheduleId) throws ScmException;

    BSONObject getSchedule(String scheduleId) throws ScmException;

    BSONObject updateSchedule(String scheduleId, BSONObject newValue) throws ScmException;

    BSONObject createBreakpointFile(String workspaceName, String fileName, long createTime,
            ScmChecksumType checksumType, InputStream fileStream, boolean isLastContent,
            boolean isNeedMd5, boolean hasSetTime) throws ScmException;

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

    BasicBSONList getCheckConnResult(String node, ScmCheckConnTarget target) throws ScmException;

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

    String getRemainUrl();

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

    BSONObject getStatisticsData(String type, BSONObject condition) throws ScmException;

    long countWorkspace(BSONObject condition) throws ScmException;

    long countSchedule(BSONObject condition) throws ScmException;

    BSONObject getSiteStrategy() throws ScmException;

    long countTask(BSONObject condition) throws ScmException;

    long countClass(String name, BSONObject condition) throws ScmException;

    BasicBSONList getContentLocations(String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmException;

    BSONObject createBucket(String wsName, String name) throws ScmException;

    BSONObject getBucket(String name) throws ScmException;

    BSONObject getBucket(long id) throws ScmException;

    void deleteBucket(String name) throws ScmException;

    BsonReader listBucket(BSONObject condition, BSONObject orderby, long skip, long limit)
            throws ScmException;

    long countBucket(BSONObject condition) throws ScmException;

    BSONObject createFileInBucket(String bucketName, InputStream is, BSONObject fileInfo,
            BSONObject uploadConf) throws ScmException;

    HttpURLConnection createFileInBucketConn(String bucketName, BSONObject fileInfo,
            BSONObject uploadConf) throws ScmException;

    BSONObject createFileInBucket(String bucketName, String breakpointFileName, BSONObject fileInfo,
            BSONObject uploadConf) throws ScmException;

    List<BSONObject> bucketAttachFile(String bucketName, List<String> fileId,
            ScmBucketAttachKeyType type) throws ScmException;

    void bucketDetachFile(String bucketName, String fileName) throws ScmException;

    BSONObject bucketGetFile(String bucketName, String fileName, int majorVersion, int minorVersion)
            throws ScmException;

    BSONObject bucketGetFileNullVersion(String bucketName, String fileName)
            throws ScmException;

    BsonReader bucketListFile(String bucketName, ScopeType scope, BSONObject condition,
            BSONObject orderby, long skip, long limit) throws ScmException;

    long bucketCountFile(String bucketName, ScopeType scope, BSONObject condition)
            throws ScmException;

    void bucketDeleteFile(String bucketName, String fileName, boolean isPhysical)
            throws ScmException;

    void bucketDeleteFileVersion(String bucketName, String fileName, int majorVersion,
            int minorVersion) throws ScmException;

    void setBucketVersionStatus(String bucketName, ScmBucketVersionStatus status)
            throws ScmException;

    void deleteFileVersion(String wsName, String fileId, int majorVersion, int minorVersion)
            throws ScmException;

    void setDefaultRegion(String s3ServiceName, String wsName) throws ScmException;
    String getDefaultRegion(String s3ServiceName) throws ScmException;

    BSONObject refreshAccesskey(String targetUser, String targetPassword, String accesskey,
            String secretkey) throws ScmException;

    String getHealthStatus(String url, String healthPath) throws ScmException;

    BSONObject listTrace(Long minDuration, String serviceName, Date startTime, Date endTime,
            Map<String, String> queryCondition, int limit) throws ScmException;

    BSONObject getTrace(String traceId) throws ScmException;

    void setBucketTag(String bucketName, Map<String, String> customTag) throws ScmException;

    void deleteBucketTag(String bucketName) throws ScmException;
    void setGlobalLifeCycleConfig(BSONObject lifeCycleConfigBSON) throws ScmException;

    void deleteGlobalLifeCycleConfig() throws ScmException;

    BSONObject getGlobalLifeCycleConfig() throws ScmException;

    void addGlobalStageTag(String stageTagName,String stageTagDesc) throws ScmException;

    void removeGlobalStageTag(String stageTagName) throws ScmException;

    void updateGlobalTransition(String transitionName, BSONObject transitionBSON) throws ScmException;

    void addGlobalTransition(BSONObject transitionBSON) throws ScmException;

    void removeGlobalTransition(String transitionName) throws ScmException;

    BSONObject getGlobalTransition(String transitionName) throws ScmException;

    BSONObject listWorkspaceByTransitionName(String transitionName) throws ScmException;

    BSONObject listTransition(String stageTagName) throws ScmException;

    BSONObject listStageTag() throws ScmException;

    void setSiteStageTag(String siteName,String stageTagName) throws ScmException;

    void alterSiteStageTag(String siteName, String stageTagName) throws ScmException;

    void unsetSiteStageTag(String siteName) throws ScmException;

    BSONObject getSiteStageTag(String siteName) throws ScmException;

    BSONObject applyTransition(String workspace, String transitionName,
            ScmLifeCycleTransition transition, String preferredRegion, String preferredZone)
            throws ScmException;

    void removeWsTransition(String workspace, String transitionName) throws ScmException;

    BSONObject updateWsTransition(String workspace, String transitionName,
            BSONObject transitionBSON, String preferredRegion, String preferredZone)
            throws ScmException;

    BSONObject getWsTransition(String workspace, String transitionName) throws ScmException;

    void updateWsTransitionStatus(String workspace, String transitionName, Boolean status) throws ScmException;

    BSONObject listWsTransition(String workspace) throws ScmException;

    ScmId startOnceTransition(String workspaceName, BSONObject condition, int scope,
                              long maxExecTime, String source, String dest, String dataCheckLevel, boolean quickStart,
                              boolean isRecycleSpace, String type, String preferredRegion,
                              String preferredZone) throws ScmException;

    BSONObject enableBucketQuota(String bucketName, long maxObjects, long maxSize, Long usedObjects,
            Long usedSize)
            throws ScmException;

    BSONObject updateBucketQuota(String bucketName, long maxObjects, long maxSize)
            throws ScmException;

    void disableBucketQuota(String bucketName) throws ScmException;

    BSONObject getBucketQuota(String bucketName) throws ScmException;

    void syncBucketQuota(String bucketName) throws ScmException;

    void cancelSyncBucketQuota(String bucketName) throws ScmException;

    BsonReader getStatisticsObjectDeltaList(BSONObject condition) throws ScmException;

    void refreshObjectDeltaStatistics(String bucketName) throws ScmException;

    BSONObject updateBucketUsedQuota(String bucketName, Long usedObjects, Long usedSize) throws ScmException;
}
