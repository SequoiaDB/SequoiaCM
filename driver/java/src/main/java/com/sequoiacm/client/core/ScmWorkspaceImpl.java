package com.sequoiacm.client.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.element.lifecycle.ScmWorkspaceLifeCycleConfig;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.IOUtils;
import com.sequoiacm.infrastructure.common.XMLUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.infrastructure.common.BsonUtils;

/**
 * The implement of ScmWorkspace.
 */
class ScmWorkspaceImpl extends ScmWorkspace {

    private String name;
    private int id;
    private ScmSession session;
    private ScmMetaLocation metaLocation;
    private List<ScmDataLocation> dataLocations;
    private String description;
    private String createUser;
    private String updateUser;
    private Date createTime;
    private Date updateTime;
    private BSONObject extData;
    private ScmShardingType batchShardingType;
    private String batchIdTimeRegex;
    private String batchIdTimePattern;
    private boolean batchFileNameUnique;
    private boolean enableDirectory;
    private String preferred;
    private ScmSiteCacheStrategy siteCacheStrategy;
    private ScmWorkspaceTagRetrievalStatus tagRetrievalStatus;
    private ScmTagLibMetaOption tagLibMetaOption;

    public ScmWorkspaceImpl(ScmSession s, BSONObject wsInfo) throws ScmException {
        this.session = s;
        refresh(wsInfo);
    }

    private void refresh(BSONObject newWsInfo) throws ScmException {
        this.name = (String) newWsInfo.get(FieldName.FIELD_CLWORKSPACE_NAME);
        this.id = (Integer) newWsInfo.get(FieldName.FIELD_CLWORKSPACE_ID);
        this.description = BsonUtils.getStringOrElse(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_DESCRIPTION, "");
        this.createTime = new Date(
                BsonUtils.getNumberOrElse(newWsInfo, FieldName.FIELD_CLWORKSPACE_CREATETIME, 0L)
                        .longValue());
        this.createUser = BsonUtils.getStringOrElse(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_CREATEUSER, "");
        this.updateTime = new Date(
                BsonUtils.getNumberOrElse(newWsInfo, FieldName.FIELD_CLWORKSPACE_UPDATETIME, 0L)
                        .longValue());
        this.updateUser = BsonUtils.getStringOrElse(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_UPDATEUSER, "");
        BSONObject metaLocationBSON = (BSONObject) newWsInfo
                .get(FieldName.FIELD_CLWORKSPACE_META_LOCATION);
        this.metaLocation = createMetaLocation(metaLocationBSON);
        BasicBSONList dataBsonlocatios = (BasicBSONList) newWsInfo
                .get(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION);
        this.dataLocations = new ArrayList<ScmDataLocation>();
        for (Object dataBSON : dataBsonlocatios) {
            dataLocations.add(createDataLocation((BSONObject) dataBSON));
        }
        extData = BsonUtils.getBSON(newWsInfo, FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);

        batchFileNameUnique = BsonUtils.getBooleanOrElse(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_BATCH_FILE_NAME_UNIQUE, false);
        batchIdTimePattern = BsonUtils.getString(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_PATTERN);
        batchIdTimeRegex = BsonUtils.getString(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_REGEX);
        String batchShardingTypeStr = BsonUtils.getStringOrElse(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_BATCH_SHARDING_TYPE, ScmShardingType.NONE.getName());
        batchShardingType = ScmShardingType.getShardingType(batchShardingTypeStr);

        enableDirectory = BsonUtils.getBooleanOrElse(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY, false);
        preferred = BsonUtils.getString(newWsInfo, FieldName.FIELD_CLWORKSPACE_PREFERRED);
        siteCacheStrategy = ScmSiteCacheStrategy.getStrategy(BsonUtils.getStringOrElse(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_SITE_CACHE_STRATEGY,
                ScmSiteCacheStrategy.ALWAYS.name()));
        String tagRetrievalStr = BsonUtils.getStringOrElse(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_TAG_RETRIEVAL_STATUS,
                ScmWorkspaceTagRetrievalStatus.DISABLED.getValue());
        tagRetrievalStatus = ScmWorkspaceTagRetrievalStatus.fromValue(tagRetrievalStr);
        BSONObject tagLibMetaOptionBSON = BsonUtils.getBSON(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_TAG_LIB_META_OPTION);
        if (tagLibMetaOptionBSON != null) {
            tagLibMetaOption = new ScmTagLibMetaOption(BsonUtils.getString(tagLibMetaOptionBSON,
                    FieldName.FIELD_CLWORKSPACE_TAG_LIB_META_OPTION_DOMAIN));
        }
    }

    private ScmMetaLocation createMetaLocation(BSONObject metaBSON) throws ScmException {
        String type = (String) metaBSON.get(CommonDefine.RestArg.WORKSPACE_LOCATION_TYPE);
        DatasourceType metasourceType = DatasourceType.getDatasourceType(type);
        switch (metasourceType) {
            case SEQUOIADB:
                return new ScmSdbMetaLocation(metaBSON);
            default:
                throw new ScmSystemException("unknown location type:" + metaBSON);
        }
    }

    private ScmDataLocation createDataLocation(BSONObject dataBSON) throws ScmException {
        String type = (String) dataBSON.get(CommonDefine.RestArg.WORKSPACE_LOCATION_TYPE);
        DatasourceType dataType = DatasourceType.getDatasourceType(type);
        switch (dataType) {
            case SEQUOIADB:
                return new ScmSdbDataLocation(dataBSON);
            case HDFS:
                return new ScmHdfsDataLocation(dataBSON);
            case HBASE:
                return new ScmHbaseDataLocation(dataBSON);
            case CEPH_S3:
                return new ScmCephS3DataLocation(dataBSON);
            case CEPH_SWIFT:
                return new ScmCephSwiftDataLocation(dataBSON);
            case SFTP:
                return new ScmSftpDataLocation(dataBSON);
            default:
                throw new ScmSystemException("unknown location type:" + dataBSON);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    ScmSession getSession() {
        return session;
    }

    @Override
    public ScmMetaLocation getMetaLocation() {
        return metaLocation;
    }

    @Override
    public List<ScmDataLocation> getDataLocations() {
        return dataLocations;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCreateUser() {
        return createUser;
    }

    @Override
    public String getUpdateUser() {
        return updateUser;
    }

    @Override
    public Date getUpdateTime() {
        return updateTime;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public String toString() {
        return "ScmWorkspaceImpl{" + "name='" + name + '\'' + ", id=" + id + ", session=" + session
                + ", metaLocation=" + metaLocation + ", dataLocations=" + dataLocations
                + ", description='" + description + '\'' + ", createUser='" + createUser + '\''
                + ", updateUser='" + updateUser + '\'' + ", createTime=" + createTime
                + ", updateTime=" + updateTime + ", extData=" + extData + ", batchShardingType="
                + batchShardingType + ", batchIdTimeRegex='" + batchIdTimeRegex + '\''
                + ", batchIdTimePattern='" + batchIdTimePattern + '\'' + ", batchFileNameUnique="
                + batchFileNameUnique + ", enableDirectory=" + enableDirectory + ", preferred='"
                + preferred + '\'' + ", tagRetrieval=" + tagRetrievalStatus + '}';
    }

    @Override
    public void updatedDescription(String newDescription) throws ScmException {
        if (newDescription == null) {
            throw new ScmInvalidArgumentException("description is null");
        }

        BasicBSONObject descUpdator = new BasicBSONObject(
                CommonDefine.RestArg.WORKSPACE_UPDATOR_DESCRIPTION, newDescription);
        _update(descUpdator);
    }

    @Override
    public void addDataLocation(ScmDataLocation dataLocation) throws ScmException {
        if (dataLocation == null) {
            throw new ScmInvalidArgumentException("datalocation is null");
        }
        BasicBSONObject updator = new BasicBSONObject(
                CommonDefine.RestArg.WORKSPACE_UPDATOR_ADD_DATA_LOCATION,
                dataLocation.getBSONObject());
        _update(updator);
    }

    @Override
    public void removeDataLocation(String siteName) throws ScmException {
        if (siteName == null) {
            throw new ScmInvalidArgumentException("site name is null");
        }
        BasicBSONObject updator = new BasicBSONObject(
                CommonDefine.RestArg.WORKSPACE_UPDATOR_REMOVE_DATA_LOCATION, siteName);
        _update(updator);
    }

    @Override
    public void updateDataLocation(List<ScmDataLocation> dataLocations) throws ScmException {
        updateDataLocation(dataLocations, true);
    }

    @Override
    public void updateDataLocation(List<ScmDataLocation> dataLocations, boolean mergeTo) throws ScmException {
        if (dataLocations == null || 0 == dataLocations.size()) {
            throw new ScmInvalidArgumentException("dataLocations is null");
        }

        BasicBSONList dataLocationList = new BasicBSONList();
        for (ScmDataLocation dataLocation : dataLocations){
            dataLocationList.add(dataLocation.getBSONObject());
        }
        BasicBSONObject updator = new BasicBSONObject(
                CommonDefine.RestArg.WORKSPACE_UPDATOR_UPDATE_DATA_LOCATION,
                dataLocationList);
        updator.put(CommonDefine.RestArg.WORKSPACE_UPDATOR_MERGE, mergeTo);
        _update(updator);
    }

    @Override
    public ScmTransitionSchedule applyTransition(String transitionName) throws ScmException {
        return applyTransition(transitionName, null);
    }

    @Override
    public ScmTransitionSchedule applyTransition(String transitionName,
            ScmLifeCycleTransition transition) throws ScmException {
        return applyTransition(transitionName, transition, session.getPreferredRegion(),
                session.getPreferredZone());
    }

    @Override
    public ScmTransitionSchedule applyTransition(String transitionName, String preferredRegion,
            String preferredZone) throws ScmException {
        return applyTransition(transitionName, null, preferredRegion, preferredZone);
    }

    @Override
    public ScmTransitionSchedule applyTransition(String transitionName,
            ScmLifeCycleTransition transition, String preferredRegion, String preferredZone)
            throws ScmException {
        if (transitionName == null) {
            throw new ScmInvalidArgumentException("transition name is null");
        }
        if (preferredRegion == null) {
            throw new ScmInvalidArgumentException("preferred region is null");
        }
        if (preferredZone == null) {
            throw new ScmInvalidArgumentException("preferred zone is null");
        }
        BSONObject transitionSchedule = session.getDispatcher().applyTransition(name, transitionName,
                transition, preferredRegion, preferredZone);
        return new ScmTransitionScheduleImpl(session, transitionSchedule);
    }

    @Override
    public void setTransitionConfig(String xmlPath) throws ScmException {
        InputStream in = null;
        try {
            in = new FileInputStream(xmlPath);
            setTransitionConfig(in);
        }
        catch (IOException e) {
            throw new ScmException(ScmError.FILE_IO,
                    "failed to read the xml file,xmlPath=" + xmlPath, e);
        }
        finally {
            IOUtils.close(in);
        }
    }

    @Override
    public void setTransitionConfig(InputStream xmlInputStream) throws ScmException {
        ScmWorkspaceLifeCycleConfig workspaceLifeCycleConfig = null;
        BSONObject obj = null;
        try {
            obj = XMLUtils.xmlToBSONObj(xmlInputStream);
        }
        catch (Exception e) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "unable to parse xml life cycle config content", e);
        }
        if (obj != null) {
            workspaceLifeCycleConfig = ScmWorkspaceLifeCycleConfig.fromUser(obj);
            for (ScmLifeCycleTransition transition : workspaceLifeCycleConfig
                    .getTransitionConfig()) {
                String transitionName = transition.getName();
                applyTransition(transitionName, transition, session.getPreferredRegion(),
                        session.getPreferredZone());
            }
        }
    }

    @Override
    public void removeTransition(String transitionName) throws ScmException {
        if (transitionName == null) {
            throw new ScmInvalidArgumentException("transition name is null");
        }
        session.getDispatcher().removeWsTransition(name, transitionName);
    }

    @Override
    public ScmTransitionSchedule updateTransition(String transitionName,
            ScmLifeCycleTransition transition) throws ScmException {
        return updateTransition(transitionName, transition, null, null);
    }

    @Override
    public ScmTransitionSchedule updateTransition(String transitionName,
            ScmLifeCycleTransition transition, String preferredRegion, String preferredZone)
            throws ScmException {
        if (transitionName == null) {
            throw new ScmInvalidArgumentException("transition name is null");
        }
        if (transition == null) {
            throw new ScmInvalidArgumentException("new transition is null");
        }
        BSONObject transitionSchedule = session.getDispatcher().updateWsTransition(name,
                transitionName, transition.toBSONObject(), preferredRegion, preferredZone);
        return new ScmTransitionScheduleImpl(session, transitionSchedule);
    }

    @Override
    public ScmTransitionSchedule getTransition(String transitionName) throws ScmException {
        if (transitionName == null) {
            throw new ScmInvalidArgumentException("transition name is null");
        }
        BSONObject transitionSchedule = session.getDispatcher().getWsTransition(name,
                transitionName);
        return new ScmTransitionScheduleImpl(session, transitionSchedule);
    }

    @Override
    public List<ScmTransitionSchedule> listTransition() throws ScmException {
        BasicBSONList list = (BasicBSONList) session.getDispatcher().listWsTransition(name);
        List<ScmTransitionSchedule> transitionScheduleList = new ArrayList<ScmTransitionSchedule>();
        for (Object o : list) {
            transitionScheduleList.add(new ScmTransitionScheduleImpl(session, (BSONObject) o));
        }
        return transitionScheduleList;
    }

    private void _update(BSONObject updator) throws ScmException {
        BSONObject newWsObj = session.getDispatcher().updateWorkspace(name, updator);
        refresh(newWsObj);
    }

    @Override
    BSONObject getExtData() {
        return extData;
    }

    @Override
    public String getBatchIdTimePattern() {
        return batchIdTimePattern;
    }

    @Override
    public String getBatchIdTimeRegex() {
        return batchIdTimeRegex;
    }

    @Override
    public ScmShardingType getBatchShardingType() {
        return batchShardingType;
    }

    @Override
    public boolean isBatchFileNameUnique() {
        return batchFileNameUnique;
    }

    @Override
    public boolean isEnableDirectory() {
        return enableDirectory;
    }
    @Override
    public void disableDirectory() throws ScmException {
        BasicBSONObject updater = new BasicBSONObject(
                CommonDefine.RestArg.WORKSPACE_UPDATOR_ENABLE_DIRECTORY, false);
        _update(updater);
    }

    @Override
    public String getPreferred() {
        return preferred;
    }

    @Override
    public void updatePreferred(String preferred) throws ScmException {
        if (preferred == null) {
            throw new ScmInvalidArgumentException("preferred is null");
        }

        BasicBSONObject updater = new BasicBSONObject(
                CommonDefine.RestArg.WORKSPACE_UPDATOR_PREFERRED, preferred);
        _update(updater);
    }

    @Override
    public ScmSiteCacheStrategy getSiteCacheStrategy() {
        return siteCacheStrategy;
    }

    @Override
    public void updateSiteCacheStrategy(ScmSiteCacheStrategy newSiteCacheStrategy)
            throws ScmException {
        if (newSiteCacheStrategy == null) {
            throw new ScmInvalidArgumentException("new site cache strategy is null");
        }

        BasicBSONObject updator = new BasicBSONObject(
                CommonDefine.RestArg.WORKSPACE_UPDATOR_SITE_CACHE_STRATEGY,
                newSiteCacheStrategy.name());
        _update(updator);
    }

    @Override
    public void updateMetaDomain(String domainName) throws ScmException {
        if (null == domainName) {
            throw new ScmInvalidArgumentException("new domain name is null");
        }

        BasicBSONObject updator = new BasicBSONObject(
                CommonDefine.RestArg.WORKSPACE_UPDATOR_META_DOMAIN, domainName);
        _update(updator);
    }

    @Override
    public void setEnableTagRetrieval(boolean enableTagRetrieval) throws ScmException {
        BSONObject newBson = session.getDispatcher().enableWsTagRetrieval(name, enableTagRetrieval);
        refresh(newBson);
    }

    @Override
    public ScmWorkspaceTagRetrievalStatus getTagRetrievalStatus() throws ScmException {
        return tagRetrievalStatus;
    }

    @Override
    public String getTagLibIndexErrorMsg() throws ScmException {
        // 没有错误返回空字符串
        if (extData == null) {
            return "";
        }
        return BsonUtils.getStringOrElse(extData,
                FieldName.FIELD_CLWORKSPACE_EXT_DATA_TAG_IDX_TASK_ERROR, "");
    }

    public ScmTagLibMetaOption getTagLibMetaOption() {
        return tagLibMetaOption;
    }
}