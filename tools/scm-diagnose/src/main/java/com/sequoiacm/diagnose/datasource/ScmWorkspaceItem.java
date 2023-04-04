package com.sequoiacm.diagnose.datasource;

import java.util.HashMap;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.common.mapping.ScmWorkspaceObj;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;
import com.sequoiacm.datasource.metadata.cephswift.CephSwiftDataLocation;
import com.sequoiacm.datasource.metadata.hbase.HbaseDataLocation;
import com.sequoiacm.datasource.metadata.hdfs.HdfsDataLocation;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.datasource.metadata.sftp.SftpDataLocation;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.config.MetaSourceLocation;
import com.sequoiacm.metasource.sequoiadb.config.SdbMetaSourceLocation;

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
    private Map<Integer, ScmLocation> dataLocations = new HashMap<>();
    private BSONObject wsBson;

    private ScmShardingType batchShardingType;
    private String batchIdTimeRegex;
    private String batchIdTimePattern;
    private boolean batchFileNameUnique;
    private boolean enableDirectory;
    private ScmSiteCacheStrategy siteCacheStrategy;

    public ScmWorkspaceItem(BSONObject workspaceObj, ScmSiteMgr siteMgr) throws ScmServerException {
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
                ScmSiteInfo siteInfo = siteMgr.getSite(siteId);
                ScmLocation dataLocation = createDataLocation(siteInfo.getDataTypeStr(),
                        dataLocationObj, siteInfo.getName());
                dataLocations.put(siteId, dataLocation);
            }

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

    private int getSiteId(BSONObject dataLocationObj) throws ScmServerException {
        Object tmp = dataLocationObj.get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
        if (null == tmp) {
            throw new ScmInvalidArgumentException(
                    "field is not exist:fieldName=" + FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
        }

        int siteId = (int) tmp;
        return siteId;
    }

    public String getPreferred() {
        return preferred;
    }

    public int getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
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

    public MetaSourceLocation getMetaLocation() {
        return metaLocation;
    }

    public Map<Integer, ScmLocation> getDataLocations() {
        return dataLocations;
    }

    public BSONObject getWsBson() {
        return wsBson;
    }

    public ScmShardingType getBatchShardingType() {
        return batchShardingType;
    }

    public String getBatchIdTimeRegex() {
        return batchIdTimeRegex;
    }

    public String getBatchIdTimePattern() {
        return batchIdTimePattern;
    }

    public boolean isBatchFileNameUnique() {
        return batchFileNameUnique;
    }

    public boolean isEnableDirectory() {
        return enableDirectory;
    }

    public ScmSiteCacheStrategy getSiteCacheStrategy() {
        return siteCacheStrategy;
    }

    public ScmLocation createDataLocation(String dataType, BSONObject data, String siteName)
            throws ScmDatasourceException {
        switch (dataType) {
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR:
                return new SdbDataLocation(data, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR:
                return new CephS3DataLocation(data, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHSWIFT_STR:
                return new CephSwiftDataLocation(data, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR:
                return new HbaseDataLocation(data, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR:
                return new HdfsDataLocation(data, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SFTP_STR:
                return new SftpDataLocation(data, siteName);
            default:
                throw new ScmDatasourceException("unknown datasource type:" + dataType);
        }
    }

    public ScmLocation getScmLocation(int siteId) {
        return dataLocations.get(siteId);
    }
}
