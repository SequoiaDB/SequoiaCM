package com.sequoiacm.infrastructure.config.core.msg.workspace;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import org.bson.BSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdater;
import org.bson.types.BasicBSONList;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.WORKSPACE)
public class WorkspaceUpdater implements ConfigUpdater {
    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_WORKSPACENAME)
    private String wsName;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_MATCHER)
    private BSONObject matcher;

    // 老版本消息的字段，现与 matcher 字段一致，为了兼容老版本，保留此字段
    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_OLD_WS)
    private BSONObject oldWs;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_UPDATOR)
    private Updater updater = new Updater();

    public WorkspaceUpdater(String wsName) {
        this.wsName = wsName;
    }

    public WorkspaceUpdater(String wsName, BSONObject matcher) {
        this.wsName = wsName;
        this.matcher = matcher;
        this.oldWs = matcher;
    }

    public WorkspaceUpdater() {
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public BSONObject getMatcher() {
        return matcher;
    }

    public void setMatcher(BSONObject matcher) {
        this.matcher = matcher;
        this.oldWs = matcher;
    }

    public BSONObject getOldWs() {
        return matcher;
    }

    public void setOldWs(BSONObject oldWs) {
        this.matcher = oldWs;
        this.oldWs = oldWs;
    }

    public Updater getUpdater() {
        return updater;
    }

    public void setUpdater(Updater updater) {
        this.updater = updater;
    }

    public Integer getRemoveDataLocationId() {
        return updater.getRemoveDataLocationId();
    }

    public void setRemoveDataLocationId(Integer removeDataLocationId) {
        updater.setRemoveDataLocationId(removeDataLocationId);
    }

    public BSONObject getAddDataLocation() {
        return updater.getAddDataLocation();
    }

    public void setAddDataLocation(BSONObject addDataLocation) {
        updater.setAddDataLocation(addDataLocation);
    }

    public BasicBSONList getUpdateDataLocation() {
        return updater.getUpdateDataLocation();
    }

    public void setUpdateDataLocation(BasicBSONList updateDataLocation) {
        updater.setUpdateDataLocation(updateDataLocation);
    }

    public String getNewDesc() {
        return updater.getNewDesc();
    }

    public void setNewDesc(String newDesc) {
        updater.setNewDesc(newDesc);
    }

    public BSONObject getExternalData() {
        return updater.getExternalData();
    }

    public void setExternalData(BSONObject externalData) {
        updater.setExternalData(externalData);
    }

    public String getPreferred() {
        return updater.getPreferred();
    }

    public void setPreferred(String preferred) {
        updater.setPreferred(preferred);
    }

    public String getNewSiteCacheStrategy() {
        return updater.getNewSiteCacheStrategy();
    }

    public void setNewSiteCacheStrategy(String newSiteCacheStrategy) {
        updater.setNewSiteCacheStrategy(newSiteCacheStrategy);
    }

    public String getUpdateDomain() {
        return updater.getUpdateDomain();
    }

    public void setUpdateDomain(String updateDomain) {
        updater.setUpdateDomain(updateDomain);
    }

    public String getAddExtraMetaCs() {
        return updater.getAddExtraMetaCs();
    }

    public void setAddExtraMetaCs(String addExtraMetaCs) {
        updater.setAddExtraMetaCs(addExtraMetaCs);
    }

    public Boolean getEnableDirectory() {
        return updater.getEnableDirectory();
    }

    public void setEnableDirectory(Boolean enableDirectory) {
        updater.setEnableDirectory(enableDirectory);
    }

    public String getTagRetrievalStatus() {
        return updater.getTagRetrievalStatus();
    }

    public void setTagRetrievalStatus(String tagRetrievalStatus) {
        updater.setTagRetrievalStatus(tagRetrievalStatus);
    }

    public Boolean getTagUpgrading() {
        return updater.getTagUpgrading();
    }

    public void setTagUpgrading(Boolean tagUpgrading) {
        updater.setTagUpgrading(tagUpgrading);
    }

    public String getTagLibTable() {
        return updater.getTagLibTable();
    }

    public void setTagLibTable(String tagLibTable) {
        updater.setTagLibTable(tagLibTable);
    }

    public String getUpdateUser() {
        return updater.getUpdateUser();
    }

    public void setUpdateUser(String updateUser) {
        updater.setUpdateUser(updateUser);
    }

    public Long getUpdateTime() {
        return updater.getUpdateTime();
    }

    public void setUpdateTime(Long updateTime) {
        updater.setUpdateTime(updateTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WorkspaceUpdater that = (WorkspaceUpdater) o;
        return Objects.equals(wsName, that.wsName) && Objects.equals(matcher, that.matcher)
                && Objects.equals(oldWs, that.oldWs) && Objects.equals(updater, that.updater);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wsName, matcher, oldWs, updater);
    }
}

class Updater {
    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_REMOVE_DATALOCATION)
    private Integer removeDataLocationId;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_ADD_DATALOCATION)
    private BSONObject addDataLocation;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_UPDATE_DATALOCATION)
    private BasicBSONList updateDataLocation;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_DESCRIPTION)
    private String newDesc;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_EXTERNAL_DATA)
    private BSONObject externalData;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_PREFERRED)
    private String preferred;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_SITE_CACHE_STRATEGY)
    private String newSiteCacheStrategy;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_UPDATOR_META_DOMAIN)
    private String updateDomain;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_UPDATOR_ADD_EXTRA_META_CS)
    private String addExtraMetaCs;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_UPDATOR_ENABLE_DIRECTORY)

    private Boolean enableDirectory;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_TAG_RETRIEVAL_STATUS)

    private String tagRetrievalStatus;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_TAG_UPGRADING)
    private Boolean tagUpgrading;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_TAG_LIB_TABLE)
    private String tagLibTable;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_UPDATE_USER)
    private String updateUser;
    
    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_UPDATE_TIME)
    private Long updateTime;
    public Updater() {
    }

    public Integer getRemoveDataLocationId() {
        return removeDataLocationId;
    }

    public void setRemoveDataLocationId(Integer removeDataLocationId) {
        this.removeDataLocationId = removeDataLocationId;
    }

    public BSONObject getAddDataLocation() {
        return addDataLocation;
    }

    public void setAddDataLocation(BSONObject addDataLocation) {
        this.addDataLocation = addDataLocation;
    }

    public BasicBSONList getUpdateDataLocation() {
        return updateDataLocation;
    }

    public void setUpdateDataLocation(BasicBSONList updateDataLocation) {
        this.updateDataLocation = updateDataLocation;
    }

    public String getNewDesc() {
        return newDesc;
    }

    public void setNewDesc(String newDesc) {
        this.newDesc = newDesc;
    }

    public BSONObject getExternalData() {
        return externalData;
    }

    public void setExternalData(BSONObject externalData) {
        this.externalData = externalData;
    }

    public String getPreferred() {
        return preferred;
    }

    public void setPreferred(String preferred) {
        this.preferred = preferred;
    }

    public String getNewSiteCacheStrategy() {
        return newSiteCacheStrategy;
    }

    public void setNewSiteCacheStrategy(String newSiteCacheStrategy) {
        this.newSiteCacheStrategy = newSiteCacheStrategy;
    }

    public String getUpdateDomain() {
        return updateDomain;
    }

    public void setUpdateDomain(String updateDomain) {
        this.updateDomain = updateDomain;
    }

    public String getAddExtraMetaCs() {
        return addExtraMetaCs;
    }

    public void setAddExtraMetaCs(String addExtraMetaCs) {
        this.addExtraMetaCs = addExtraMetaCs;
    }

    public Boolean getEnableDirectory() {
        return enableDirectory;
    }

    public void setEnableDirectory(Boolean enableDirectory) {
        this.enableDirectory = enableDirectory;
    }

    public String getTagRetrievalStatus() {
        return tagRetrievalStatus;
    }

    public void setTagRetrievalStatus(String tagRetrievalStatus) {
        this.tagRetrievalStatus = tagRetrievalStatus;
    }

    public Boolean getTagUpgrading() {
        return tagUpgrading;
    }

    public void setTagUpgrading(Boolean tagUpgrading) {
        this.tagUpgrading = tagUpgrading;
    }

    public String getTagLibTable() {
        return tagLibTable;
    }

    public void setTagLibTable(String tagLibTable) {
        this.tagLibTable = tagLibTable;
    }


    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Updater updater = (Updater) o;
        return Objects.equals(removeDataLocationId, updater.removeDataLocationId)
                && Objects.equals(addDataLocation, updater.addDataLocation)
                && Objects.equals(updateDataLocation, updater.updateDataLocation)
                && Objects.equals(newDesc, updater.newDesc)
                && Objects.equals(externalData, updater.externalData)
                && Objects.equals(preferred, updater.preferred)
                && Objects.equals(newSiteCacheStrategy, updater.newSiteCacheStrategy)
                && Objects.equals(updateDomain, updater.updateDomain)
                && Objects.equals(addExtraMetaCs, updater.addExtraMetaCs)
                && Objects.equals(enableDirectory, updater.enableDirectory)
                && Objects.equals(tagRetrievalStatus, updater.tagRetrievalStatus)
                && Objects.equals(tagUpgrading, updater.tagUpgrading)
                && Objects.equals(tagLibTable, updater.tagLibTable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(removeDataLocationId, addDataLocation, updateDataLocation, newDesc,
                externalData, preferred, newSiteCacheStrategy, updateDomain, addExtraMetaCs,
                enableDirectory, tagRetrievalStatus, tagUpgrading, tagLibTable);
    }
}
