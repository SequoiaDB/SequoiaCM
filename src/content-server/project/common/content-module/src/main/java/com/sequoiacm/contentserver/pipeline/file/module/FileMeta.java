package com.sequoiacm.contentserver.pipeline.file.module;

import java.util.Date;

import com.sequoiacm.common.MimeType;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.ScmArgumentChecker;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.common.ServiceDefine;
import com.sequoiacm.contentserver.dao.ScmFileVersionHelper;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.google.common.base.Strings;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.model.ScmVersion;

public abstract class FileMeta implements Cloneable {
    private BucketInfoManager bucketInfoManager;
    private String id;
    private String name;
    private Long bucketId;
    private String dirId;
    private String batchId;
    private int majorVersion;
    private int minorVersion;
    private int type;
    private String classId;
    private BSONObject classProperties;
    private String user;
    private String updateUser;
    private Long createTime;
    private Long updateTime = System.currentTimeMillis();
    private String createMonth;
    private Long dataCreateTime;
    private int dataType;
    private long size;
    private String dataId;
    private BasicBSONList siteList;
    private String title;
    private String author;
    private String mimeType;
    private String md5;
    private String etag;
    private BSONObject externalData;
    private BSONObject customMeta;
    private String versionSerial;
    private boolean deleteMarker;
    private int status;
    private String transId;
    private BasicBSONList accessHistory;

    // 新增字段需要调整如下函数：
    // FileMeta(FileMeta fileMeta) 深拷贝构造
    // toBSONObject()
    // fromUser()
    // fromRecord()

    FileMeta(BucketInfoManager bucketInfoManager) {
        this.bucketInfoManager = bucketInfoManager;
    }

    public FileMeta clone() {
        FileMeta clone;
        try {
            clone = (FileMeta) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new IllegalStateException("FileMeta clone failed: " + this.getSimpleDesc(), e);
        }
        clone.bucketInfoManager = this.bucketInfoManager;
        clone.id = this.id;
        clone.name = this.name;
        clone.bucketId = this.bucketId;
        clone.dirId = this.dirId;
        clone.batchId = this.batchId;
        clone.majorVersion = this.majorVersion;
        clone.minorVersion = this.minorVersion;
        clone.type = this.type;
        clone.classId = this.classId;
        clone.classProperties = BsonUtils.deepCopyRecordBSON(this.classProperties);
        clone.user = this.user;
        clone.updateUser = this.updateUser;
        clone.createTime = this.createTime;
        clone.updateTime = this.updateTime;
        clone.createMonth = this.createMonth;
        clone.dataCreateTime = this.dataCreateTime;
        clone.dataType = this.dataType;
        clone.size = this.size;
        clone.dataId = this.dataId;
        clone.siteList = (BasicBSONList) BsonUtils.deepCopyRecordBSON(this.siteList);
        clone.title = this.title;
        clone.author = this.author;
        clone.mimeType = this.mimeType;
        clone.md5 = this.md5;
        clone.etag = this.etag;
        clone.externalData = BsonUtils.deepCopyRecordBSON(this.externalData);
        clone.customMeta = BsonUtils.deepCopyRecordBSON(this.customMeta);
        clone.versionSerial = this.versionSerial;
        clone.deleteMarker = this.deleteMarker;
        clone.status = this.status;
        clone.transId = this.transId;
        clone.accessHistory = BsonUtils.deepCopyBasicBSONList(this.accessHistory);
        return clone;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBucketId() {
        return bucketId;
    }

    public void setBucketId(Long bucketId) {
        this.bucketId = bucketId;
    }

    public String getDirId() {
        return dirId;
    }

    public void setDirId(String dirId) {
        this.dirId = dirId;
    }

    public String getBatchId() {
        return batchId;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public ScmVersion getVersion() {
        return new ScmVersion(majorVersion, minorVersion);
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public void setVersionSerial(String versionSerial) {
        this.versionSerial = versionSerial;
    }

    public int getType() {
        return type;
    }

    public String getClassId() {
        return classId;
    }

    public BSONObject getClassProperties() {
        return classProperties;
    }

    public String getUser() {
        return user;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public String getCreateMonth() {
        return createMonth;
    }

    public Long getDataCreateTime() {
        return dataCreateTime;
    }

    public int getDataType() {
        return dataType;
    }

    public long getSize() {
        return size;
    }

    public String getDataId() {
        return dataId;
    }

    public BasicBSONList getSiteList() {
        return siteList;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getMd5() {
        return md5;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public BSONObject getExternalData() {
        return externalData;
    }

    public void setExternalData(BSONObject externalData) {
        this.externalData = externalData;
    }

    public BSONObject getCustomMeta() {
        return customMeta;
    }

    public String getVersionSerial() {
        return versionSerial;
    }

    public boolean isDeleteMarker() {
        return deleteMarker;
    }

    public void setDeleteMarker(boolean deleteMarker) {
        this.deleteMarker = deleteMarker;
    }

    public int getStatus() {
        return status;
    }

    public String getTransId() {
        return transId;
    }

    public BasicBSONList getAccessHistory() {
        return accessHistory;
    }

    public boolean isNullVersion() {
        if (majorVersion == CommonDefine.File.NULL_VERSION_MAJOR
                && minorVersion == CommonDefine.File.NULL_VERSION_MINOR) {
            return true;
        }
        return false;
    }

    public void resetFileIdAndFileTime(String id, Date idGenDate) {
        this.id = id;
        this.createTime = idGenDate.getTime();
        this.updateTime = this.createTime;
        this.createMonth = ScmSystemUtils.getCurrentYearMonth(idGenDate);
    }

    public void resetDataInfo(String dataId, long dataCreateTime, int dataType, long dataSize,
            String md5, int dataSite, int wsVersion, String tableName) {
        this.dataId = dataId;
        this.dataCreateTime = dataCreateTime;
        this.dataType = dataType;
        this.size = dataSize;
        this.md5 = md5;

        BSONObject oneSite = new BasicBSONObject();
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID, dataSite);
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TIME, dataCreateTime);
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME, dataCreateTime);
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_WS_VERSION, wsVersion);
        if (!Strings.isNullOrEmpty(tableName)) {
            oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TABLE_NAME, tableName);
        }
        BasicBSONList sites = new BasicBSONList();
        sites.add(oneSite);
        this.siteList = sites;

        BSONObject access = new BasicBSONObject();
        access.put(FieldName.FIELD_CLFILE_ACCESS_HISTORY_ID, dataSite);
        BasicBSONList lastAccessTimeHis = new BasicBSONList();
        lastAccessTimeHis.add(dataCreateTime);
        access.put(FieldName.FIELD_CLFILE_ACCESS_HISTORY_LAST_ACCESS_TIME_HIS, lastAccessTimeHis);
        BasicBSONList accessRecord = new BasicBSONList();
        accessRecord.add(access);
        this.accessHistory = accessRecord;
        this.deleteMarker = false;
    }

    public boolean isFirstVersion() {
        if (getMajorVersion() == 1 && getMinorVersion() == 0) {
            return true;
        }
        if (getVersionSerial() != null && getVersionSerial().equals("1.0")) {
            return true;
        }
        return false;
    }

    protected BSONObject asUserInfoBson() throws ScmServerException {
        BSONObject bson = asRecordBSON();
        if (bucketId != null) {
            ScmBucket bucket = bucketInfoManager.getBucketById(bucketId);
            if (bucket != null) {
                bson.put(CommonDefine.RestArg.BUCKET_NAME, bucket.getName());
            }
            else {
                bson.put(FieldName.FIELD_CLFILE_FILE_BUCKET_ID, null);
            }
        }
        return bson;
    }

    // 根据入参 fileCurrentLatestVersion 重置当前对象的文件全局属性，生成版本号
    public void newVersionFrom(FileMeta fileCurrentLatestVersion) throws ScmServerException {
        BSONObject standardBson = fileCurrentLatestVersion.toRecordBSON();
        BSONObject myBson = toRecordBSON();
        ScmFileVersionHelper.resetNewFileVersion(myBson, standardBson);
        loadInfoFromRecord(myBson);
    }

    protected void loadBasicInfoFromRecord(BSONObject bson) {
        this.id = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_ID);
        this.name = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_NAME);
        Number num = BsonUtils.getNumber(bson, FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        if (num != null) {
            this.bucketId = num.longValue();
        }

        this.dirId = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_DIRECTORY_ID);
        this.batchId = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_BATCH_ID);
        this.minorVersion = BsonUtils.getIntegerChecked(bson, FieldName.FIELD_CLFILE_MINOR_VERSION);
        this.majorVersion = BsonUtils.getIntegerChecked(bson, FieldName.FIELD_CLFILE_MAJOR_VERSION);
        this.type = BsonUtils.getIntegerOrElse(bson, FieldName.FIELD_CLFILE_TYPE, 1);
        this.classId = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_CLASS_ID);
        this.classProperties = BsonUtils.getBSON(bson, FieldName.FIELD_CLFILE_PROPERTIES);
        this.classProperties = BsonUtils.deepCopyRecordBSON(this.classProperties);

        this.user = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_INNER_USER);
        this.updateUser = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_INNER_UPDATE_USER);
        num = BsonUtils.getNumber(bson, FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        if (num != null) {
            this.createTime = num.longValue();
        }
        num = BsonUtils.getNumber(bson, FieldName.FIELD_CLFILE_INNER_UPDATE_TIME);
        if (num != null) {
            this.updateTime = num.longValue();
        }
        this.createMonth = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_INNER_CREATE_MONTH);
        num = BsonUtils.getNumber(bson, FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME);
        if (num != null) {
            this.dataCreateTime = num.longValue();
        }
        this.dataType = BsonUtils.getIntegerOrElse(bson, FieldName.FIELD_CLFILE_FILE_DATA_TYPE,
                ENDataType.Normal.getValue());
        num = BsonUtils.getNumber(bson, FieldName.FIELD_CLFILE_FILE_SIZE);
        if (num != null) {
            this.size = num.longValue();
        }
        this.dataId = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_DATA_ID);
        this.siteList = BsonUtils.getArray(bson, FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        this.siteList = (BasicBSONList) BsonUtils.deepCopyRecordBSON(this.siteList);
        this.title = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_TITLE);
        this.author = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_AUTHOR);
        this.mimeType = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_MIME_TYPE);
        this.md5 = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_MD5);
        this.etag = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_ETAG);
        this.externalData = BsonUtils.getBSON(bson, FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);
        this.externalData = BsonUtils.deepCopyRecordBSON(this.externalData);
        this.customMeta = BsonUtils.getBSON(bson, FieldName.FIELD_CLFILE_CUSTOM_METADATA);
        this.customMeta = BsonUtils.deepCopyRecordBSON(this.customMeta);
        this.versionSerial = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_VERSION_SERIAL);
        this.deleteMarker = BsonUtils.getBooleanOrElse(bson, FieldName.FIELD_CLFILE_DELETE_MARKER,
                false);
        this.status = BsonUtils.getIntegerOrElse(bson, FieldName.FIELD_CLFILE_EXTRA_STATUS,
                ServiceDefine.FileStatus.NORMAL);
        this.transId = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_EXTRA_TRANS_ID);
        this.accessHistory = BsonUtils.getArray(bson, FieldName.FIELD_CLFILE_ACCESS_HISTORY);
        if (this.accessHistory != null) {
            this.accessHistory = (BasicBSONList) BsonUtils.deepCopyRecordBSON(this.accessHistory);
        }
    }

    protected BSONObject asRecordBSON() {
        BasicBSONObject bson = new BasicBSONObject();
        bson.put(FieldName.FIELD_CLFILE_ID, id);
        bson.put(FieldName.FIELD_CLFILE_NAME, name);
        bson.put(FieldName.FIELD_CLFILE_FILE_BUCKET_ID, bucketId);
        bson.put(FieldName.FIELD_CLFILE_DIRECTORY_ID, dirId);
        bson.put(FieldName.FIELD_CLFILE_BATCH_ID, batchId);
        bson.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
        bson.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
        bson.put(FieldName.FIELD_CLFILE_TYPE, type);
        bson.put(FieldName.FIELD_CLFILE_FILE_CLASS_ID, classId);
        bson.put(FieldName.FIELD_CLFILE_PROPERTIES, classProperties);
        bson.put(FieldName.FIELD_CLFILE_INNER_USER, user);
        bson.put(FieldName.FIELD_CLFILE_INNER_UPDATE_USER, updateUser);
        bson.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, createTime);
        bson.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME, updateTime);
        bson.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, createMonth);
        bson.put(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME, dataCreateTime);
        bson.put(FieldName.FIELD_CLFILE_FILE_DATA_TYPE, dataType);
        bson.put(FieldName.FIELD_CLFILE_FILE_SIZE, size);
        bson.put(FieldName.FIELD_CLFILE_FILE_DATA_ID, dataId);
        bson.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST, siteList);
        bson.put(FieldName.FIELD_CLFILE_FILE_TITLE, title);
        bson.put(FieldName.FIELD_CLFILE_FILE_AUTHOR, author);
        bson.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, mimeType);
        bson.put(FieldName.FIELD_CLFILE_FILE_MD5, md5);
        bson.put(FieldName.FIELD_CLFILE_FILE_ETAG, etag);
        bson.put(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA, externalData);
        bson.put(FieldName.FIELD_CLFILE_CUSTOM_METADATA, customMeta);
        bson.put(FieldName.FIELD_CLFILE_VERSION_SERIAL, versionSerial);
        bson.put(FieldName.FIELD_CLFILE_DELETE_MARKER, deleteMarker);
        bson.put(FieldName.FIELD_CLFILE_EXTRA_STATUS, status);
        bson.put(FieldName.FIELD_CLFILE_EXTRA_TRANS_ID, transId);
        if (accessHistory != null) {
            bson.put(FieldName.FIELD_CLFILE_ACCESS_HISTORY, accessHistory);
        }
        return bson;
    }

    public String getSimpleDesc() {
        return "fileId=" + id + ", fileName=" + name + ", size=" + size + ", isDeleteMarker="
                + deleteMarker + ", version=" + majorVersion + "." + minorVersion;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime.getTime();
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public void setClassProperties(BSONObject classProperties) {
        this.classProperties = classProperties;
    }

    public void setCustomMeta(BSONObject customMeta) {
        this.customMeta = customMeta;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setSiteList(BasicBSONList siteList) {
        this.siteList = siteList;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public void setDataCreateTime(Long dataCreateTime) {
        this.dataCreateTime = dataCreateTime;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public void setCreateMonth(String createMonth) {
        this.createMonth = createMonth;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public void setAccessHistory(BasicBSONList accessHistory) {
        this.accessHistory = accessHistory;
    }

    public abstract void loadInfoFromRecord(BSONObject record);

    public abstract void loadInfoFromUserInfo(String ws, BSONObject userFileInfo, String user,
            boolean checkProps) throws ScmServerException;

    protected void loadBasicInfoFromUserInfo(String ws, BSONObject userFileObject, String user,
            boolean checkProps) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfo(ws);
        String fileName = BsonUtils.getString(userFileObject, FieldName.FIELD_CLFILE_NAME);
        ScmFileOperateUtils.checkFileName(wsInfo, fileName);
        this.setName(fileName);

        this.setAuthor(BsonUtils.getString(userFileObject, FieldName.FIELD_CLFILE_FILE_AUTHOR));
        this.setTitle(BsonUtils.getString(userFileObject, FieldName.FIELD_CLFILE_FILE_TITLE));

        this.setMimeType(BsonUtils.getStringOrElse(userFileObject,
                FieldName.FIELD_CLFILE_FILE_MIME_TYPE, MimeType.OCTET_STREAM.getType()));

        this.setClassId(BsonUtils.getString(userFileObject, FieldName.FIELD_CLFILE_FILE_CLASS_ID));
        BSONObject classValue = BsonUtils.getBSON(userFileObject,
                FieldName.FIELD_CLFILE_PROPERTIES);
        BSONObject classProperties = BsonUtils.deepCopyRecordBSON(classValue);
        this.setClassProperties(classProperties);

        if (checkProps) {
            MetaDataManager.getInstence().checkPropeties(wsInfo.getName(), this.getClassId(),
                    classProperties);
            this.setClassProperties(ScmArgumentChecker.checkAndCorrectClass(classProperties,
                    FieldName.FIELD_CLFILE_PROPERTIES));
        }

        BSONObject extData = BsonUtils.getBSON(userFileObject,
                FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);
        extData = BsonUtils.deepCopyRecordBSON(extData);
        BSONObject newExtData = new BasicBSONObject();
        if (extData != null) {
            newExtData.putAll(extData);
        }
        this.setExternalData(extData);

        BSONObject customMeta = BsonUtils.getBSON(userFileObject,
                FieldName.FIELD_CLFILE_CUSTOM_METADATA);
        customMeta = BsonUtils.deepCopyRecordBSON(customMeta);
        BSONObject newCustomMeta = new BasicBSONObject();
        if (customMeta != null) {
            newCustomMeta.putAll(customMeta);
        }
        this.setCustomMeta(newCustomMeta);

        this.setMajorVersion(1);
        this.setMinorVersion(0);
        this.setType(1);
        this.setSiteList(new BasicBSONList());
        this.setDataId(null);
        this.setDataCreateTime(null);

        this.setUser(user);

        this.setId(BsonUtils.getString(userFileObject, FieldName.FIELD_CLFILE_ID));
        Number fileCreateTime = BsonUtils.getNumber(userFileObject,
                FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        if (fileCreateTime != null) {
            this.setCreateTime(fileCreateTime.longValue());
            this.setUpdateTime(this.getCreateTime());
            if (this.getId() != null) {
                ScmIdParser idParser = new ScmIdParser(this.getId());
                if (fileCreateTime.longValue() / 1000 != idParser.getSecond()) {
                    throw new ScmInvalidArgumentException(
                            "The specified fileID and createTime is conflict, creatTime="
                                    + new Date(fileCreateTime.longValue()));
                }
            }
            this.setCreateMonth(ScmSystemUtils.getCurrentYearMonth(new Date(this.getCreateTime())));
        }

        this.setUpdateUser(user);
        this.setStatus(ServiceDefine.FileStatus.NORMAL);
        this.setTransId("");
        this.setDataType(ENDataType.Normal.getValue());
        this.setSize(0);
        this.setMd5(null);
        this.setEtag(null);
        this.setVersionSerial(null);
        this.setDeleteMarker(false);

        this.setBucketId(null);
        this.setDirId(null);
        this.setBatchId("");

        this.setAccessHistory(new BasicBSONList());
    }

    public abstract BSONObject toRecordBSON();

    public abstract BSONObject toUserInfoBSON() throws ScmServerException;
}
