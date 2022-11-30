package com.sequoiacm.contentserver.model;

import java.util.*;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.ConcurrentLruMap;
import com.sequoiacm.infrastructure.common.ConcurrentLruMapFactory;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.contentserver.cache.ScmDirCache;
import com.sequoiacm.contentserver.site.ScmBizConf;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.metasource.config.MetaSourceLocation;

public class ScmWorkspaceInfo {
    private static final Logger logger = LoggerFactory.getLogger(ScmWorkspaceInfo.class);

    private ScmWorkspaceItem currentWorkspace;
    private ConcurrentLruMap</* versionId */Integer, ScmWorkspaceItem> workspaceHistories = ConcurrentLruMapFactory.create(10);

    public ScmWorkspaceInfo(ScmBizConf bizConf, BSONObject workspaceObj) throws ScmServerException {
        currentWorkspace = new ScmWorkspaceItem(bizConf, workspaceObj, false);
    }

    public boolean isBatchSharding() {
        return currentWorkspace.isBatchSharding();
    }

    public ScmShardingType getBatchShardingType() {
        return currentWorkspace.getBatchShardingType();
    }

    public String getBatchIdTimeRegex() {
        return currentWorkspace.getBatchIdTimeRegex();
    }

    public boolean isBatchUseSystemId() {
        return currentWorkspace.isBatchUseSystemId();
    }

    public String getBatchIdTimePattern() {
        return currentWorkspace.getBatchIdTimePattern();
    }

    public boolean isBatchFileNameUnique() {
        return currentWorkspace.isBatchFileNameUnique();
    }

    public MetaSourceLocation getMetaLocation() {
        return currentWorkspace.getMetaLocation();
    }

    public ScmLocation getDataLocation() {
        return currentWorkspace.getDataLocation();
    }

    public ScmLocation getDataLocation(int versionId) throws ScmServerException {
        ScmWorkspaceItem workspaceItem = getWorkspaceVersion(versionId);
        return workspaceItem.getDataLocation();
    }

    public ScmLocation getSiteDataLocation(int siteId) {
        return currentWorkspace.getDataLocations().get(siteId);
    }

    public ScmLocation getSiteDataLocation(int siteId, int version) throws ScmServerException {
        ScmWorkspaceItem workspaceItem = getWorkspaceVersion(version);
        return workspaceItem.getDataLocations().get(siteId);
    }

    private ScmWorkspaceItem getWorkspaceVersion(int version) throws ScmServerException {
        if (version == currentWorkspace.getVersion()) {
            return currentWorkspace;
        }

        if (workspaceHistories.get(version) != null) {
            return workspaceHistories.get(version);
        }

        ScmWorkspaceItem workspaceItem = loadHistoryWorkspace(getName(), version);
        if (workspaceItem == null) {
            throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                    "the workspace of the specified version does not exist. wsName" + getName()
                            + ", versionId:" + version);
        }
        return workspaceItem;
    }

    public ScmWorkspaceItem loadHistoryWorkspace(String wsName, int versionId)
            throws ScmServerException {
        MetaAccessor accessor = ScmContentModule.getInstance().getMetaService().getMetaSource()
                .getWorkspaceHistoryAccessor();

        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLWORKSPACE_NAME, wsName);
        matcher.put(FieldName.FIELD_CLWORKSPACE_VERSION, versionId);

        BSONObject wsObj = ScmMetaSourceHelper.queryOne(accessor, matcher);

        if (wsObj != null) {
            ScmWorkspaceItem hisWsItem = new ScmWorkspaceItem(ScmContentModule.getInstance().getBizConf(), wsObj, true);
            ScmContentModule.getInstance().workspaceMapAddHistory(hisWsItem);
            return hisWsItem;
        }

        return null;
    }

    public Map<Integer, ScmLocation> getDataLocationAllVersions() throws ScmServerException {
        Map<Integer, ScmLocation> scmLocations = new HashMap<>();
        boolean queryAllHistory = false;

        int currentVersion = currentWorkspace.getVersion();
        ScmLocation currentLocation = currentWorkspace.getDataLocation();

        int needVersion = currentVersion - 1;
        while (needVersion > 0) {
            ScmWorkspaceItem item = workspaceHistories.get(needVersion);
            if (item == null) {
                queryAllHistory = true;
                scmLocations.clear();
                break;
            }
            scmLocations.put(item.getVersion(), item.getDataLocation());
            --needVersion;
        }

        scmLocations.put(currentVersion, currentLocation);

        if (queryAllHistory) {
            List<ScmWorkspaceItem> workspaceItems = getHistoryWorkspaces(getName());
            for (ScmWorkspaceItem item : workspaceItems) {
                scmLocations.put(item.getVersion(), item.getDataLocation());
            }
        }

        return scmLocations;
    }

    public List<ScmWorkspaceItem> getHistoryWorkspaces(String wsName) throws ScmServerException {
        MetaCursor cursor = null;
        try {
            List<ScmWorkspaceItem> workspaceItems = new ArrayList<>();
            MetaAccessor accessor = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .getWorkspaceHistoryAccessor();

            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.FIELD_CLWORKSPACE_NAME, wsName);

            cursor = accessor.query(matcher, null, null);
            while (cursor.hasNext()) {
                workspaceItems.add(new ScmWorkspaceItem(ScmContentModule.getInstance().getBizConf(), cursor.getNext(), true));
            }

            return workspaceItems;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to query history workspace: " + wsName, e);
        }
        finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    public String getName() {
        return currentWorkspace.getName();
    }

    public int getId() {
        return currentWorkspace.getId();
    }

    public BSONObject getBSONObject() {
        return currentWorkspace.getBSONObject();
    }

    public Set<Integer> getDataSiteIds() {
        return currentWorkspace.getDataSiteIds();
    }

    public String getDescription() {
        return currentWorkspace.getDescription();
    }

    public String getCreateUser() {
        return currentWorkspace.getCreateUser();
    }

    public long getCreateTime() {
        return currentWorkspace.getCreateTime();
    }

    public long getUpdateTime() {
        return currentWorkspace.getUpdateTime();
    }

    public String getUpdateUser() {
        return currentWorkspace.getUpdateUser();
    }

    public ScmDirCache getDirCache() {
        return currentWorkspace.getDirCache();
    }

    public ScmWorkspaceFulltextExtData getFulltextExtData() {
        return currentWorkspace.getFulltextExtData();
    }

    public boolean isEnableDirectory() {
        return currentWorkspace.isEnableDirectory();
    }

    public Map<Integer, ScmLocation> getDataLocations() {
        return currentWorkspace.getDataLocations();
    }

    public String getPreferred() {
        return currentWorkspace.getPreferred();
    }

    public ScmSiteCacheStrategy getSiteCacheStrategy() {
        return currentWorkspace.getSiteCacheStrategy();
    }

    public int getVersion() {
        return currentWorkspace.getVersion();
    }

    public void addHistoryWsItem(ScmWorkspaceItem item) {
        workspaceHistories.put(item.getVersion(), item);
    }
}
