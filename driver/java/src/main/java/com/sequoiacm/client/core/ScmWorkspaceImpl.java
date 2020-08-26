package com.sequoiacm.client.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.element.bizconf.ScmCephS3DataLocation;
import com.sequoiacm.client.element.bizconf.ScmCephSwiftDataLocation;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHbaseDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHdfsDataLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;

/**
 * The implement of ScmWorkspace.
 */
class ScmWorkspaceImpl extends ScmWorkspace {

    private String name;
    private int id;
    private ScmSession session;
    private ScmMetaLocation metaLocation;
    private List<ScmDataLocation> dataLocations;
    private String description;
    private String createUser;
    private String updateUser;
    private Date createTime;
    private Date updateTime;
    private BSONObject extData;

    public ScmWorkspaceImpl(ScmSession s, BSONObject wsInfo) throws ScmException {
        this.session = s;
        refresh(wsInfo);
    }

    private void refresh(BSONObject newWsInfo) throws ScmException {
        this.name = (String) newWsInfo.get(FieldName.FIELD_CLWORKSPACE_NAME);
        this.id = (Integer) newWsInfo.get(FieldName.FIELD_CLWORKSPACE_ID);
        this.description = BsonUtils.getStringOrElse(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_DESCRIPTION, "");
        this.createTime = new Date(
                BsonUtils.getNumberOrElse(newWsInfo, FieldName.FIELD_CLWORKSPACE_CREATETIME, 0L)
                        .longValue());
        this.createUser = BsonUtils.getStringOrElse(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_CREATEUSER, "");
        this.updateTime = new Date(
                BsonUtils.getNumberOrElse(newWsInfo, FieldName.FIELD_CLWORKSPACE_UPDATETIME, 0L)
                        .longValue());
        this.updateUser = BsonUtils.getStringOrElse(newWsInfo,
                FieldName.FIELD_CLWORKSPACE_UPDATEUSER, "");
        BSONObject metaLocationBSON = (BSONObject) newWsInfo
                .get(FieldName.FIELD_CLWORKSPACE_META_LOCATION);
        this.metaLocation = createMetaLocation(metaLocationBSON);
        BasicBSONList dataBsonlocatios = (BasicBSONList) newWsInfo
                .get(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION);
        this.dataLocations = new ArrayList<ScmDataLocation>();
        for (Object dataBSON : dataBsonlocatios) {
            dataLocations.add(createDataLocation((BSONObject) dataBSON));
        }
        extData = BsonUtils.getBSON(newWsInfo, FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);
    }

    private ScmMetaLocation createMetaLocation(BSONObject metaBSON) throws ScmException {
        String type = (String) metaBSON.get(CommonDefine.RestArg.WORKSPACE_LOCATION_TYPE);
        DatasourceType metasourceType = DatasourceType.getDatasourceType(type);
        switch (metasourceType) {
            case SEQUOIADB:
                return new ScmSdbMetaLocation(metaBSON);
            default:
                throw new ScmSystemException("unknown location type:" + metaBSON);
        }
    }

    private ScmDataLocation createDataLocation(BSONObject dataBSON) throws ScmException {
        String type = (String) dataBSON.get(CommonDefine.RestArg.WORKSPACE_LOCATION_TYPE);
        DatasourceType dataType = DatasourceType.getDatasourceType(type);
        switch (dataType) {
            case SEQUOIADB:
                return new ScmSdbDataLocation(dataBSON);
            case HDFS:
                return new ScmHdfsDataLocation(dataBSON);
            case HBASE:
                return new ScmHbaseDataLocation(dataBSON);
            case CEPH_S3:
                return new ScmCephS3DataLocation(dataBSON);
            case CEPH_SWIFT:
                return new ScmCephSwiftDataLocation(dataBSON);

            default:
                throw new ScmSystemException("unknown location type:" + dataBSON);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    ScmSession getSession() {
        return session;
    }

    @Override
    public ScmMetaLocation getMetaLocation() {
        return metaLocation;
    }

    @Override
    public List<ScmDataLocation> getDataLocations() {
        return dataLocations;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCreateUser() {
        return createUser;
    }

    @Override
    public String getUpdateUser() {
        return updateUser;
    }

    @Override
    public Date getUpdateTime() {
        return updateTime;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public String toString() {
        return "ScmWorkspaceImpl [name=" + name + ", id=" + id + ", session=" + session
                + ", metaLocation=" + metaLocation + ", dataLocations=" + dataLocations
                + ", description=" + description + ", createUser=" + createUser + ", updateUser="
                + updateUser + ", createTime=" + createTime + ", updateTime=" + updateTime + "]";
    }

    @Override
    public void updatedDescription(String newDescription) throws ScmException {
        if (newDescription == null) {
            throw new ScmInvalidArgumentException("description is null");
        }

        BasicBSONObject descUpdator = new BasicBSONObject(
                CommonDefine.RestArg.WORKSPACE_UPDATOR_DESCRIPTION, newDescription);
        _update(descUpdator);
    }

    @Override
    public void addDataLocation(ScmDataLocation dataLocation) throws ScmException {
        if (dataLocation == null) {
            throw new ScmInvalidArgumentException("datalocation is null");
        }
        BasicBSONObject updator = new BasicBSONObject(
                CommonDefine.RestArg.WORKSPACE_UPDATOR_ADD_DATA_LOCATION,
                dataLocation.getBSONObject());
        _update(updator);
    }

    @Override
    public void removeDataLocation(String siteName) throws ScmException {
        if (siteName == null) {
            throw new ScmInvalidArgumentException("site name is null");
        }
        BasicBSONObject updator = new BasicBSONObject(
                CommonDefine.RestArg.WORKSPACE_UPDATOR_REMOVE_DATA_LOCATION, siteName);
        _update(updator);
    }

    private void _update(BSONObject updator) throws ScmException {
        BSONObject newWsObj = session.getDispatcher().updateWorkspace(name, updator);
        refresh(newWsObj);
    }

    @Override
    BSONObject getExtData() {
        return extData;
    }

}