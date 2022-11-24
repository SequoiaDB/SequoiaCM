package com.sequoiacm.contentserver.model;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.common.mapping.ScmWorkspaceObj;
import com.sequoiacm.contentserver.cache.ScmDirCache;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataSourceType;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.site.ScmBizConf;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.datasource.DatalocationFactory;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.metasource.config.MetaSourceLocation;
import com.sequoiacm.metasource.sequoiadb.config.SdbMetaSourceLocation;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ScmWorkspaceItem {
    private static final Logger logger = LoggerFactory.getLogger(ScmWorkspaceItem.class);
    private final String preferred;
    private int version;

    private String name;
    private int id;
    private String description;
    private String createUser;
    private long createTime;
    private long updateTime;
    private String updateUser;
    private MetaSourceLocation metaLocation;
    private ScmLocation dataLocation;
    private Map<Integer, ScmLocation> dataLocations = new HashMap<>();
    private BSONObject wsBson;
    private ScmDirCache dirCache;

    private ScmWorkspaceFulltextExtData fulltextExtData;

    private ScmShardingType batchShardingType;
    private String batchIdTimeRegex;
    private String batchIdTimePattern;
    private boolean batchFileNameUnique;
    private boolean enableDirectory;
    private ScmSiteCacheStrategy siteCacheStrategy;

    public ScmWorkspaceItem(ScmBizConf bizConf, BSONObject workspaceObj, boolean isHistory) throws ScmServerException {
        try {
            ScmWorkspaceObj wsObj = new ScmWorkspaceObj(workspaceObj);
            this.name = wsObj.getName();
            this.id = wsObj.getId();
            this.description = wsObj.getDescriptions();
            this.createUser = wsObj.getCreateUser();
            this.updateTime = wsObj.getUpdateTime();
            this.createTime = wsObj.getCreateTime();
            this.updateUser = wsObj.getUpdateUser();
            this.wsBson = workspaceObj;

            BSONObject metaShardingType = new BasicBSONObject();
            metaShardingType.put(FieldName.FIELD_CLWORKSPACE_META_SHARDING_TYPE,
                    wsObj.getMetaShardingType());
            this.metaLocation = new SdbMetaSourceLocation(wsObj.getMetaLocation(),
                    metaShardingType);

            for (BSONObject dataLocationObj : wsObj.getDataLocation()) {
                if (dataLocationObj == null) {
                    continue;
                }
                int siteId = getSiteId(dataLocationObj);
                ScmLocation location = createDataLocation(bizConf, dataLocationObj,
                        wsObj.getDataShardingType(), wsObj.getDataOption(), isHistory);
                if (location != null) {
                    dataLocations.put(location.getSiteId(), location);
                }
                if (bizConf.getLocateSiteId() == siteId) {
                    this.dataLocation = location;
                }
            }

            checkWorkspace(bizConf);
            if (PropertiesUtils.enableDirCache()) {
                dirCache = new ScmDirCache(name, PropertiesUtils.getDirCacheMaxSize());
            }

            fulltextExtData = new ScmWorkspaceFulltextExtData(name, id, wsObj.getExternalData());
            batchShardingType = ScmShardingType.getShardingType(wsObj.getBatchShardingType());
            batchFileNameUnique = wsObj.isBatchFileNameUnique();
            batchIdTimePattern = wsObj.getBatchIdTimePattern();
            batchIdTimeRegex = wsObj.getBatchIdTimeRegex();
            enableDirectory = wsObj.isEnableDirectory();
            preferred = wsObj.getPreferred();
            siteCacheStrategy = wsObj.getSiteCacheStrategy();

            if (wsObj.getVersion() != null) {
                version = wsObj.getVersion();
            }
            else {
                version = 1;
            }
        }
        catch (ScmServerException e) {
            logger.error("parse workspace info failed:record=" + workspaceObj.toString());
            throw e;
        }
        catch (Exception e) {
            logger.error("parse workspace info failed:record=" + workspaceObj.toString());
            throw new ScmInvalidArgumentException(
                    "parse workspace info failed:record=" + workspaceObj.toString(), e);
        }
    }

    public boolean isBatchSharding() {
        return batchShardingType != ScmShardingType.NONE;
    }

    public ScmShardingType getBatchShardingType() {
        return batchShardingType;
    }

    public String getBatchIdTimeRegex() {
        return batchIdTimeRegex;
    }

    public boolean isBatchUseSystemId() {
        return batchIdTimePattern == null || batchIdTimeRegex == null;
    }

    public String getBatchIdTimePattern() {
        return batchIdTimePattern;
    }

    public boolean isBatchFileNameUnique() {
        return batchFileNameUnique;
    }

    private void checkWorkspace(ScmBizConf bizConf) throws ScmServerException {
        int rootSiteId = bizConf.getRootSiteId();

        if (metaLocation.getSiteId() != rootSiteId) {
            logger.error("meta site must be root site:wsName=" + name + ",metaSite="
                    + metaLocation.getSiteId() + ",rootSite=" + rootSiteId);
            throw new ScmInvalidArgumentException("meta site must be root site:wsName=" + name
                    + ",metaSite=" + metaLocation.getSiteId() + ",rootSite=" + rootSiteId);
        }

        if (null == dataLocation && bizConf.getLocateSiteId() == rootSiteId) {
            // i am root site, this ws does not contain my datalocation
            throw new ScmInvalidArgumentException("root site data location is null:wsName=" + name
                    + ",site=" + bizConf.getLocateSiteId());
        }
    }

    private ScmLocation createSdbDataLocation(ScmSite siteInfo, BSONObject dataLocationObj,
            BSONObject defaultSdbShardingType, BSONObject defaultSdbDataOptions)
            throws ScmDatasourceException {
        // do not modify dataLocation,
        BasicBSONObject newDataLocationObj = new BasicBSONObject();
        newDataLocationObj.putAll(dataLocationObj);

        if (!newDataLocationObj.containsField(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE)) {
            newDataLocationObj.put(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE,
                    defaultSdbShardingType);
        }

        if (!newDataLocationObj.containsField(FieldName.FIELD_CLWORKSPACE_DATA_OPTIONS)) {
            newDataLocationObj.put(FieldName.FIELD_CLWORKSPACE_DATA_OPTIONS, defaultSdbDataOptions);
        }

        return new SdbDataLocation(newDataLocationObj, siteInfo.getName());
    }

    private int getSiteId(BSONObject dataLocationObj) throws ScmServerException {
        Object tmp = dataLocationObj.get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
        if (null == tmp) {
            throw new ScmInvalidArgumentException(
                    "field is not exist:fieldName=" + FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
        }

        int siteId = (int) tmp;
        return siteId;
    }

    private ScmLocation createDataLocation(ScmBizConf bizConf, BSONObject dataLocationObj,
            BSONObject defaultSdbShardingType, BSONObject defaultSdbDataOptions, boolean isHistory)
            throws ScmServerException {
        int siteId = getSiteId(dataLocationObj);
        ScmSite siteInfo = bizConf.getSiteInfo(siteId);
        if (null == siteInfo) {
            if (isHistory && bizConf.getLocateSiteId() != siteId) {
                return null;
            }
            throw new ScmInvalidArgumentException("site is not exist:siteId=" + siteId);
        }

        try {
            ScmSiteUrl siteUrl = siteInfo.getDataUrl();
            ScmLocation scmLocation;
            if (siteUrl.getType().equals(ScmDataSourceType.SEQUOIADB.getName())) {
                scmLocation = createSdbDataLocation(siteInfo, dataLocationObj,
                        defaultSdbShardingType, defaultSdbDataOptions);
            }
            else {
                scmLocation = DatalocationFactory.createDataLocation(siteUrl.getType(),
                        dataLocationObj, siteInfo.getName());
            }
            return scmLocation;
        }
        catch (NoClassDefFoundError e) {
            logger.error("create dataLocation failed:location=" + dataLocationObj);
            throw new ScmSystemException("create dataLocation failed:location=" + dataLocationObj,
                    e);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_ERROR), e.getMessage(), e);
        }
    }

    public MetaSourceLocation getMetaLocation() {
        return metaLocation;
    }

    public ScmLocation getDataLocation() {
        return dataLocation;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public BSONObject getBSONObject() {
        return wsBson;
    }

    public Set<Integer> getDataSiteIds() {
        return dataLocations.keySet();
    }

    public String getDescription() {
        return description;
    }

    public String getCreateUser() {
        return createUser;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public ScmDirCache getDirCache() {
        return dirCache;
    }

    public ScmWorkspaceFulltextExtData getFulltextExtData() {
        return fulltextExtData;
    }

    public boolean isEnableDirectory() {
        return enableDirectory;
    }

    public Map<Integer, ScmLocation> getDataLocations() {
        return dataLocations;
    }

    public String getPreferred() {
        return preferred;
    }

    public ScmSiteCacheStrategy getSiteCacheStrategy() {
        return siteCacheStrategy;
    }

    public int getVersion() {
        return version;
    }
}
