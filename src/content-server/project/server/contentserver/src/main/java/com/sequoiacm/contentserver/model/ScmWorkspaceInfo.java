package com.sequoiacm.contentserver.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.mapping.ScmWorkspaceObj;
import com.sequoiacm.contentserver.cache.ScmDirCache;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataSourceType;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.site.ScmBizConf;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.datasource.DatalocationFactory;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.config.MetaSourceLocation;
import com.sequoiacm.metasource.sequoiadb.config.SdbMetaSourceLocation;

public class ScmWorkspaceInfo {
    private static final Logger logger = LoggerFactory.getLogger(ScmWorkspaceInfo.class);

    private String name;
    private int id;
    private String description;
    private String createUser;
    private long createTime;
    private long updateTime;
    private String updateUser;
    private MetaSourceLocation metaLocation;
    private ScmLocation dataLocation;
    private Map<Integer, BSONObject> dataInfoMap = new LinkedHashMap<>();
    private BSONObject wsBson;
    private ScmDirCache dirCache;

    public ScmWorkspaceInfo(ScmBizConf bizConf, BSONObject workspaceObj) throws ScmServerException {
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
                int siteId = getSiteId(dataLocationObj);
                dataInfoMap.put(siteId, dataLocationObj);
                ScmLocation location = createDataLocation(bizConf, dataLocationObj,
                        wsObj.getDataShardingType(), wsObj.getDataOption());
                if (bizConf.getLocateSiteId() == siteId) {
                    this.dataLocation = location;
                }
            }

            checkWorkspace(bizConf);
            if (PropertiesUtils.enableDirCache()) {
                dirCache = new ScmDirCache(name, PropertiesUtils.getDirCacheMaxSize());
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

        return new SdbDataLocation(newDataLocationObj);

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
            BSONObject defaultSdbShardingType, BSONObject defaultSdbDataOptions)
            throws ScmServerException {
        int siteId = getSiteId(dataLocationObj);
        ScmSite siteInfo = bizConf.getSiteInfo(siteId);
        if (null == siteInfo) {
            throw new ScmInvalidArgumentException("site is not exist:siteId=" + siteId);
        }

        try {
            ScmSiteUrl siteUrl = siteInfo.getDataUrl();
            if (siteUrl.getType().equals(ScmDataSourceType.SEQUOIADB.getName())) {
                return createSdbDataLocation(siteInfo, dataLocationObj, defaultSdbShardingType,
                        defaultSdbDataOptions);
            }
            else {
                return DatalocationFactory.createDataLocation(siteUrl.getType(), dataLocationObj);
            }
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

    public BSONObject getLocationObj(int siteId) {
        return dataInfoMap.get(siteId);
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
        return dataInfoMap.keySet();
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
}
