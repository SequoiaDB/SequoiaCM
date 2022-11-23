package com.sequoiacm.infrastructure.config.core.msg.workspace;

import com.sequoiacm.common.ScmSiteCacheStrategy;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;

public class WorkspaceConfig implements Config {
    private String wsName;
    private int wsId;
    private String createUser;
    private String updateUser;
    private String desc = "";
    private Long createTime;
    private Long updateTime;
    private BSONObject metalocation;
    private BasicBSONList dataLocations;
    private BSONObject externalData;
    private String batchShardingType;
    private String batchIdTimeRegex;
    private String batchIdTimePattern;
    private boolean batchFileNameUnique;
    private boolean enableDirectory = false;
    private String preferred;
    private String siteCacheStrategy = ScmSiteCacheStrategy.ALWAYS.name();
    private int version;

    public WorkspaceConfig() {
    }

    public BSONObject getExternalData() {
        return externalData;
    }

    public void setExternalData(BSONObject externalData) {
        this.externalData = externalData;
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public BSONObject getMetalocation() {
        return metalocation;
    }

    public void setMetalocation(BSONObject metalocation) {
        this.metalocation = metalocation;
    }

    public BasicBSONList getDataLocations() {
        return dataLocations;
    }

    public void setDataLocations(BasicBSONList dataLocations) {
        this.dataLocations = dataLocations;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public int getWsId() {
        return wsId;
    }

    public void setWsId(int wsId) {
        this.wsId = wsId;
    }

    public String getBatchShardingType() {
        return batchShardingType;
    }

    public void setBatchShardingType(String batchShardingType) {
        this.batchShardingType = batchShardingType;
    }

    public String getBatchIdTimeRegex() {
        return batchIdTimeRegex;
    }

    public void setBatchIdTimeRegex(String batchIdTimeRegex) {
        this.batchIdTimeRegex = batchIdTimeRegex;
    }

    public String getBatchIdTimePattern() {
        return batchIdTimePattern;
    }

    public void setBatchIdTimePattern(String batchIdTimePattern) {
        this.batchIdTimePattern = batchIdTimePattern;
    }

    public boolean isBatchFileNameUnique() {
        return batchFileNameUnique;
    }

    public void setBatchFileNameUnique(boolean batchFileNameUnique) {
        this.batchFileNameUnique = batchFileNameUnique;
    }

    public void setEnableDirectory(boolean enableDirectory) {
        this.enableDirectory = enableDirectory;
    }

    public boolean isEnableDirectory() {
        return enableDirectory;
    }

    public String getPreferred() {
        return preferred;
    }

    public void setPreferred(String preferred) {
        this.preferred = preferred;
    }

    public String getSiteCacheStrategy() {
        return siteCacheStrategy;
    }

    public void setSiteCacheStrategy(String siteCacheStrategy) {
        this.siteCacheStrategy = siteCacheStrategy;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public BSONObject toBSONObject() {
        BSONObject wsConfigObj = new BasicBSONObject();
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_NAME, wsName);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_ID, wsId);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_DESCRIPTION, desc);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_CREATEUSER, createUser);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_UPDATEUSER, updateUser);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_CREATETIME, createTime);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_UPDATETIME, updateTime);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_META_LOCATION, metalocation);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION, dataLocations);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_EXT_DATA, externalData);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_PATTERN, batchIdTimePattern);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_REGEX, batchIdTimeRegex);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_BATCH_FILE_NAME_UNIQUE, batchFileNameUnique);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_BATCH_SHARDING_TYPE, batchShardingType);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY, enableDirectory);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_PREFERRED, preferred);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_SITE_CACHE_STRATEGY, siteCacheStrategy);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_VERSION, version);
        return wsConfigObj;
    }

}
