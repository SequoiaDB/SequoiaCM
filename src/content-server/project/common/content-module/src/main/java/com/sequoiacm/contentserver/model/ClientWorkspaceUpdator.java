package com.sequoiacm.contentserver.model;

import com.sequoiacm.common.ScmSiteCacheStrategy;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;

public class ClientWorkspaceUpdator {
    private String description;
    private String removeDataLocation;
    private ClientLocationOutline addDataLocation;
    private List<ClientLocationOutline> updateDataLocation;
    private String preferred;
    private ScmSiteCacheStrategy siteCacheStrategy;
    private Boolean updateMerge = true;
    private String domainName;
    
    private Boolean enableDirectory;

    public ClientWorkspaceUpdator() {
    }

    public void setPreferred(String preferred) {
        this.preferred = preferred;
    }

    public String getPreferred() {
        return preferred;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRemoveDataLocation() {
        return removeDataLocation;
    }

    public void setRemoveDataLocation(String removeDataLocation) {
        this.removeDataLocation = removeDataLocation;
    }

    public ClientLocationOutline getAddDataLocation() {
        return addDataLocation;
    }

    public void setAddDataLocation(ClientLocationOutline addDataLocation) {
        this.addDataLocation = addDataLocation;
    }

    public ScmSiteCacheStrategy getSiteCacheStrategy() {
        return siteCacheStrategy;
    }

    public void setSiteCacheStrategy(ScmSiteCacheStrategy siteCacheStrategy) {
        this.siteCacheStrategy = siteCacheStrategy;
    }

    public List<ClientLocationOutline> getUpdateDataLocation() {
        return updateDataLocation;
    }

    public void setUpdateDataLocation(List<ClientLocationOutline> updateDataLocation) {
        this.updateDataLocation = updateDataLocation;
    }

    public void setUpdateMerge(Boolean updateMerge) {
        this.updateMerge = updateMerge;
    }

    public Boolean getUpdateMerge() {
        return updateMerge;
    }

    public Boolean getEnableDirectory() {
        return enableDirectory;
    }

    public void setEnableDirectory(Boolean enableDirectory) {
        this.enableDirectory = enableDirectory;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public String toString() {
        return "ClientWorkspaceUpdator{" + "description='" + description + '\''
                + ", removeDataLocation='" + removeDataLocation + '\'' + ", addDataLocation="
                + addDataLocation + ", updateDataLocation=" + updateDataLocation + ", preferred='"
                + preferred + '\'' + ", siteCacheStrategy=" + siteCacheStrategy + ", updateMerge="
                + updateMerge + ", domainName='" + domainName + '\'' + ", enableDirectory="
                + enableDirectory + '}';
    }

    public static ClientWorkspaceUpdator fromBSONObject(BSONObject obj)
            throws ScmInvalidArgumentException {
        BSONObject objCopy = new BasicBSONObject(obj.toMap());
        ClientWorkspaceUpdator updator = new ClientWorkspaceUpdator();
        String desc = (String) objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_DESCRIPTION);
        String siteCacheStrategyStr = (String) objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_SITE_CACHE_STRATEGY);
        String domain = (String) objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_META_DOMAIN);
        if (siteCacheStrategyStr != null) {
            ScmSiteCacheStrategy siteCacheStrategyEnum = ScmSiteCacheStrategy
                    .getStrategy(siteCacheStrategyStr);
            if (siteCacheStrategyEnum == ScmSiteCacheStrategy.UNKNOWN) {
                throw new ScmInvalidArgumentException(
                        "failed to update workspace, invalid site cache strategy:"
                                + siteCacheStrategyStr);
            }
            updator.setSiteCacheStrategy(siteCacheStrategyEnum);
        }
        updator.setDescription(desc);
        updator.setDomainName(domain);
        String removeDataLocation = (String) objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_REMOVE_DATA_LOCATION);
        updator.setRemoveDataLocation(removeDataLocation);
        BSONObject addDataLocation = (BSONObject) objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_ADD_DATA_LOCATION);
        if (addDataLocation != null) {
            updator.setAddDataLocation(new ClientLocationOutline(addDataLocation));
        }
        BasicBSONList updateDataLocation = (BasicBSONList) objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_UPDATE_DATA_LOCATION);
        if (updateDataLocation != null) {
            List<ClientLocationOutline> dataLocation = new ArrayList<>();
            for (Object location : updateDataLocation) {
                dataLocation.add(new ClientLocationOutline((BasicBSONObject)location));
            }
            updator.setUpdateDataLocation(dataLocation);
        }

        Object updateMerge = objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_MERGE);
        if (updateMerge != null){
            updator.setUpdateMerge((Boolean) updateMerge);
        }
        else {
            updator.setUpdateMerge(true);
        }

        String preferred = (String) objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_PREFERRED);
        updator.setPreferred(preferred);
        Object enableDirectory = objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_ENABLE_DIRECTORY);
        if (enableDirectory != null) {
            updator.setEnableDirectory((Boolean) enableDirectory);
        }
        if (!objCopy.isEmpty()) {
            throw new ScmInvalidArgumentException(
                    "failed to update workspace, updator contain invalid key:" + objCopy.keySet());
        }
        return updator;
    }
}
