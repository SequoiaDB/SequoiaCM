package com.sequoiacm.infrastructure.config.core.msg.workspace;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import org.bson.types.BasicBSONList;

public class WorkspaceUpdator implements ConfigUpdator {
    private String wsName;
    private Integer removeDataLocationId;
    private BSONObject addDataLocation;
    private BasicBSONList updateDataLocation;
    private Boolean updateMerge;
    private String newDesc;
    private BSONObject externalData;
    private BSONObject matcher;
    private String preferred;
    private String newSiteCacheStrategy;
    private String updateDomain;
    private String addExtraMetaCs;

    private Boolean enableDirectory;

    public WorkspaceUpdator(String wsName) {
        this.wsName = wsName;
    }

    public WorkspaceUpdator(String wsName, BSONObject matcher) {
        this.wsName = wsName;
        this.matcher = matcher;
    }

    public void setExternalData(BSONObject externalData) {
        this.externalData = externalData;
    }

    public BSONObject getExternalData() {
        return externalData;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.WORKSPACE_CONF_WORKSPACENAME, wsName);
        BasicBSONObject updator = new BasicBSONObject();
        if (newDesc != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_CONF_DESCRIPTION, newDesc);
        }
        if (newSiteCacheStrategy != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_CONF_SITE_CACHE_STRATEGY, newSiteCacheStrategy);
        }
        if (addDataLocation != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_CONF_ADD_DATALOCATION, addDataLocation);
        }

        if (removeDataLocationId != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_CONF_REMOVE_DATALOCATION, removeDataLocationId);
        }
        if (updateDataLocation != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_CONF_UPDATE_DATALOCATION, updateDataLocation);
        }
        if (externalData != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_CONF_EXTERNAL_DATA, externalData);
        }
        if (preferred != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_CONF_PREFERRED, preferred);
        }
        if (enableDirectory != null){
            updator.put(ScmRestArgDefine.WORKSPACE_UPDATOR_ENABLE_DIRECTORY, enableDirectory);
        }
        if (updateDomain != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_UPDATOR_META_DOMAIN, updateDomain);
        }
        if (addExtraMetaCs != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_UPDATOR_ADD_EXTRA_META_CS, addExtraMetaCs);
        }
        obj.put(ScmRestArgDefine.WORKSPACE_CONF_UPDATOR, updator);
        obj.put(ScmRestArgDefine.WORKSPACE_CONF_MATCHER, matcher);
        obj.put(ScmRestArgDefine.WORKSPACE_CONF_OLD_WS, matcher);
        return obj;
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
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

    public String getNewDesc() {
        return newDesc;
    }

    public void setNewDesc(String newDesc) {
        this.newDesc = newDesc;
    }

    public BSONObject getMatcher() {
        return matcher;
    }

    public void setMatcher(BSONObject matcher) {
        this.matcher = matcher;
    }

    public void setPreferred(String preferred) {
        this.preferred = preferred;
    }

    public String getPreferred() {
        return preferred;
    }
    public void setNewSiteCacheStrategy(String newSiteCacheStrategy) {
        this.newSiteCacheStrategy = newSiteCacheStrategy;
    }

    public String getNewSiteCacheStrategy() {
        return newSiteCacheStrategy;
    }

    public void setUpdateDataLocation(BasicBSONList updateDataLocation) {
        this.updateDataLocation = updateDataLocation;
    }

    public BasicBSONList getUpdateDataLocation() {
        return updateDataLocation;
    }

    public void setUpdateMerge(Boolean updateMerge) {
        this.updateMerge = updateMerge;
    }

    public Boolean isMerge() {
        return updateMerge;
    }

    public Boolean isEnableDirectory() {
        return enableDirectory;
    }

    public void setEnableDirectory(Boolean enableDirectory) {
        this.enableDirectory = enableDirectory;
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
}
