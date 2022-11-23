package com.sequoiacm.client.element;

import java.util.Date;
import java.util.List;

import com.sequoiacm.common.ScmSiteCacheStrategy;
import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.common.mapping.ScmMappingException;
import com.sequoiacm.common.mapping.ScmWorkspaceObj;

/**
 * The information of workspace.
 */
public class ScmWorkspaceInfo {

    private int id;
    private String name;
    private BSONObject metaLocation;
    private List<BSONObject> dataLocation;
    private BSONObject dataOption;
    private BSONObject dataShardingType;
    private String metaShardingType;
    private String desc;
    private Date createTime;
    private String updateUser;
    private String createUser;
    private Date updateTime;
    private ScmShardingType batchShardingType;
    private String batchIdTimeRegex;
    private String batchIdTimePattern;
    private boolean batchFileNameUnique;
    private boolean enableDirectory;
    private String preferred;
    private ScmSiteCacheStrategy siteCacheStrategy;
    private int version;

    /**
     * Create a instance of ScmWorkspaceInfo.
     *
     * @param info
     *            a bson containing basic information about workspace.
     * @throws ScmException
     *             If error happens
     *
     */
    public ScmWorkspaceInfo(BSONObject info) throws ScmException {
        try {
            ScmWorkspaceObj wsObj = new ScmWorkspaceObj(info);
            this.id = wsObj.getId();
            this.name = wsObj.getName();
            this.metaLocation = wsObj.getMetaLocation();
            this.metaShardingType = wsObj.getMetaShardingType();
            this.dataLocation = wsObj.getDataLocation();
            this.dataShardingType = wsObj.getDataShardingType();
            this.dataOption = wsObj.getDataOption();
            this.desc = wsObj.getDescriptions();
            this.createTime = new Date(wsObj.getCreateTime());
            this.createUser = wsObj.getCreateUser();
            this.updateUser = wsObj.getUpdateUser();
            this.updateTime = new Date(wsObj.getUpdateTime());
            this.batchFileNameUnique = wsObj.isBatchFileNameUnique();
            this.batchIdTimePattern = wsObj.getBatchIdTimePattern();
            this.batchIdTimeRegex = wsObj.getBatchIdTimeRegex();
            this.batchShardingType = ScmShardingType.getShardingType(wsObj.getBatchShardingType());
            this.enableDirectory = wsObj.isEnableDirectory();
            this.preferred = wsObj.getPreferred();
            this.siteCacheStrategy = wsObj.getSiteCacheStrategy();
            this.version = 1;
            if (wsObj.getVersion() != null) {
                this.version = wsObj.getVersion();
            }
        }
        catch (ScmMappingException e) {
            throw new ScmInvalidArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Return the value of id property
     *
     * @return Workspace id.
     * @since 2.1
     */
    public int getId() {
        return id;
    }

    /**
     * Return the value of name property.
     *
     * @return Workspace name
     * @since 2.1
     */
    public String getName() {
        return name;
    }

    /**
     * Return the value of metaLocation property
     *
     * @return Meta location
     * @since 2.1
     */
    public BSONObject getMetaLocation() {
        return metaLocation;
    }

    /**
     * Return the value of dataLocation property.
     *
     * @return Data location
     * @since 2.1
     */
    public List<BSONObject> getDataLocation() {
        return dataLocation;
    }

    /**
     * Return the value of dataOption property,return null if dataOption is
     * default.
     *
     * @return Data option
     * @since 2.1
     */
    @Deprecated
    public BSONObject getDataOption() {
        return dataOption;
    }

    /**
     * Return the value of dataShardingType property,return null if
     * dataShardingType is default.
     *
     * @return Data sharding type.
     * @since 2.1
     */
    @Deprecated
    public BSONObject getDataShardingType() {
        return dataShardingType;
    }

    /**
     * Return the value of metaSharidngType property.
     *
     * @return Meta sharding type.
     * @since 2.1
     */
    @Deprecated
    public String getMetaShardingType() {
        return metaShardingType;
    }

    /**
     * Returns the sharding type of batch.
     * 
     * @return batch sharding type.
     */
    public ScmShardingType getBatchShardingType() {
        return batchShardingType;
    }

    /**
     * Returns the time regex of batch id.
     * 
     * @return regex.
     */
    public String getBatchIdTimeRegex() {
        return batchIdTimeRegex;
    }

    /**
     * Returns the time pattern of batch id.
     * 
     * @return time pattern.
     */
    public String getBatchIdTimePattern() {
        return batchIdTimePattern;
    }

    /**
     * Return true if the file name is unique in a batch, elase return false.
     * 
     * @return return true if the file name is unique in a batch.
     */
    public boolean isBatchFileNameUnique() {
        return batchFileNameUnique;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id:" + id).append(",").append("name:" + name).append(",")
                .append("metaLocation:" + metaLocation).append(",")
                .append("metaShardinType:" + metaShardingType).append(",");

        if (dataLocation != null) {
            sb.append("dataLocation:" + dataLocation.toString()).append(",");
        }
        else {
            sb.append("dataLocation:null").append(",");
        }

        if (dataShardingType != null) {
            sb.append("dataShardingType:" + dataShardingType).append(",");
        }
        else {
            sb.append("dataShardingType:null").append(",");
        }

        if (dataOption != null) {
            sb.append("dataOption:" + dataOption).append(",");
        }
        else {
            sb.append("dataOption:null").append(",");
        }
        sb.append("description:" + desc).append(",");
        sb.append("createUser:" + createUser).append(",");
        sb.append("updateUser:" + updateUser).append(",");
        sb.append("createTime:" + createTime).append(",");
        sb.append("updateTime:" + updateTime).append(",");
        sb.append("batchShardingType:" + batchShardingType).append(",");
        sb.append("batchIdTimeRegex:" + batchIdTimeRegex).append(",");
        sb.append("batchIdTimePattern:" + batchIdTimePattern).append(",");
        sb.append("batchFileNameUnique:" + batchFileNameUnique).append(",");
        sb.append("siteCacheStrategy:" + siteCacheStrategy).append(",");
        sb.append("version:" + version);
        return sb.toString();

    }

    /**
     * Gets the description of the workspace.
     *
     * @return description.
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Gets the created time of the workspace.
     *
     * @return created time.
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * Gets the updated user of the workspace.
     *
     * @return last updated user.
     */
    public String getUpdateUser() {
        return updateUser;
    }

    /**
     * Gets the created user of the workspace.
     *
     * @return created user.
     */
    public String getCreateUser() {
        return createUser;
    }

    /**
     * Gets the updated time of the workspace.
     *
     * @return last updated time.
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * Return true if the workspace enable directory feature, else return false.
     * 
     * @return return true if the workspace enable directory feature.
     */
    public boolean isEnableDirectory() {
        return enableDirectory;
    }

    /**
     * Return the strategy to choose site.
     * 
     * @return
     */
    public String getPreferred() {
        return preferred;
    }

    /**
     * Return the site cache strategy.
     *
     * @return site cache strategy.
     */
    public ScmSiteCacheStrategy getSiteCacheStrategy() {
        return siteCacheStrategy;
    }

    /**
     * Return the version of workspace.
     *
     * @return version.
     */
    public int getVersion() {
        return version;
    }
}
