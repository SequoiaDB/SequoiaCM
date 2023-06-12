package com.sequoiacm.infrastructure.config.core.msg.workspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.WORKSPACE)
public class WorkspaceConfig implements Config {

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_NAME)
    private String wsName;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_ID)
    private int wsId;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_CREATEUSER)
    private String createUser;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_UPDATEUSER)
    private String updateUser;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_DESCRIPTION)
    private String desc = "";

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_CREATETIME)
    private Long createTime;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_UPDATETIME)
    private Long updateTime;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_META_LOCATION)
    private BSONObject metalocation;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION)
    private BasicBSONList dataLocations;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_EXT_DATA)
    private BSONObject externalData;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_BATCH_SHARDING_TYPE)
    private String batchShardingType;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_REGEX)
    private String batchIdTimeRegex;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_PATTERN)
    private String batchIdTimePattern;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_BATCH_FILE_NAME_UNIQUE)
    private boolean batchFileNameUnique;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY)
    private boolean enableDirectory = false;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_PREFERRED)
    private String preferred;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_SITE_CACHE_STRATEGY)
    private String siteCacheStrategy = ScmSiteCacheStrategy.ALWAYS.name();

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_VERSION)
    private int version;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_TAG_LIB_META_OPTION)
    private BSONObject tagLibMetaOption;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_TAG_LIB_TABLE)
    private String tagLibTableName;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_TAG_RETRIEVAL_STATUS)
    private String tagRetrievalStatus;

    @JsonProperty(FieldName.FIELD_CLWORKSPACE_TAG_UPGRADING)
    private boolean tagUpgrading = false;

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


    public BSONObject getTagLibMetaOption() {
        return tagLibMetaOption;
    }

    public void setTagLibMetaOption(BSONObject tagLibMetaOption) {
        this.tagLibMetaOption = tagLibMetaOption;
    }

    public void setTagLibTableName(String tagLibTableName) {
        this.tagLibTableName = tagLibTableName;
    }

    public String getTagLibTableName() {
        return tagLibTableName;
    }

    public void setTagRetrievalStatus(String tagRetrievalStatus) {
        this.tagRetrievalStatus = tagRetrievalStatus;
    }

    public String getTagRetrievalStatus() {
        return tagRetrievalStatus;
    }

    public void setTagUpgrading(boolean tagUpgrading) {
        this.tagUpgrading = tagUpgrading;
    }

    public boolean isTagUpgrading() {
        return tagUpgrading;
    }

    @Override
    public String getBusinessName() {
        return wsName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkspaceConfig that = (WorkspaceConfig) o;
        return wsId == that.wsId && batchFileNameUnique == that.batchFileNameUnique && enableDirectory == that.enableDirectory && version == that.version && tagUpgrading == that.tagUpgrading && Objects.equals(wsName, that.wsName) && Objects.equals(createUser, that.createUser) && Objects.equals(updateUser, that.updateUser) && Objects.equals(desc, that.desc) && Objects.equals(createTime, that.createTime) && Objects.equals(updateTime, that.updateTime) && Objects.equals(metalocation, that.metalocation) && Objects.equals(dataLocations, that.dataLocations) && Objects.equals(externalData, that.externalData) && Objects.equals(batchShardingType, that.batchShardingType) && Objects.equals(batchIdTimeRegex, that.batchIdTimeRegex) && Objects.equals(batchIdTimePattern, that.batchIdTimePattern) && Objects.equals(preferred, that.preferred) && Objects.equals(siteCacheStrategy, that.siteCacheStrategy) && Objects.equals(tagLibMetaOption, that.tagLibMetaOption) && Objects.equals(tagLibTableName, that.tagLibTableName) && Objects.equals(tagRetrievalStatus, that.tagRetrievalStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wsName, wsId, createUser, updateUser, desc, createTime, updateTime, metalocation, dataLocations, externalData, batchShardingType, batchIdTimeRegex, batchIdTimePattern, batchFileNameUnique, enableDirectory, preferred, siteCacheStrategy, version, tagLibMetaOption, tagLibTableName, tagRetrievalStatus, tagUpgrading);
    }
}
