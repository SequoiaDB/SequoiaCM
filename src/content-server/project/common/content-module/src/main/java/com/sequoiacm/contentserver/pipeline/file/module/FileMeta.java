package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.contentserver.common.ScmArgumentChecker;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.common.ServiceDefine;
import com.sequoiacm.contentserver.dao.ScmFileVersionHelper;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.Date;
import java.util.Set;

public class FileMeta implements Cloneable {
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
    private BSONObject tags;
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
    // 新增字段需要调整如下函数：
    // FileMeta(FileMeta fileMeta) 深拷贝构造
    // toBSONObject()
    // fromUser()
    // fromRecord()

    // 深拷贝
    private FileMeta(FileMeta fileMeta) {
        this.id = fileMeta.id;
        this.name = fileMeta.name;
        this.bucketId = fileMeta.bucketId;
        this.dirId = fileMeta.dirId;
        this.batchId = fileMeta.batchId;
        this.majorVersion = fileMeta.majorVersion;
        this.minorVersion = fileMeta.minorVersion;
        this.type = fileMeta.type;
        this.classId = fileMeta.classId;
        this.classProperties = BsonUtils.deepCopyRecordBSON(fileMeta.classProperties);
        this.tags = BsonUtils.deepCopyRecordBSON(fileMeta.tags);
        this.user = fileMeta.user;
        this.updateUser = fileMeta.updateUser;
        this.createTime = fileMeta.createTime;
        this.updateTime = fileMeta.updateTime;
        this.createMonth = fileMeta.createMonth;
        this.dataCreateTime = fileMeta.dataCreateTime;
        this.dataType = fileMeta.dataType;
        this.size = fileMeta.size;
        this.dataId = fileMeta.dataId;
        this.siteList = (BasicBSONList) BsonUtils.deepCopyRecordBSON(fileMeta.siteList);
        this.title = fileMeta.title;
        this.author = fileMeta.author;
        this.mimeType = fileMeta.mimeType;
        this.md5 = fileMeta.md5;
        this.etag = fileMeta.etag;
        this.externalData = BsonUtils.deepCopyRecordBSON(fileMeta.externalData);
        this.customMeta = BsonUtils.deepCopyRecordBSON(fileMeta.customMeta);
        this.versionSerial = fileMeta.versionSerial;
        this.deleteMarker = fileMeta.deleteMarker;
        this.status = fileMeta.status;
        this.transId = fileMeta.transId;
    }

    private FileMeta() {
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

    public BSONObject getTags() {
        return tags;
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

    public ScmDataInfo getDataInfo() {
        return new ScmDataInfo(dataType, dataId, new Date(dataCreateTime), 0);
    }

    public static FileMeta deleteMarkerMeta(String ws, String fileName, String user, long bucketId)
            throws ScmServerException {
        BSONObject obj = new BasicBSONObject(FieldName.FIELD_CLFILE_NAME, fileName);
        FileMeta ret = fromUser(ws, obj, user);
        ret.setBucketId(bucketId);
        ret.setDeleteMarker(true);
        Date createTime = new Date();
        ret.resetFileIdAndFileTime(ScmIdGenerator.FileId.get(createTime), createTime);
        return ret;
    }

    public static FileMeta fromUser(String ws, BSONObject userFileObject, String user)
            throws ScmServerException {
        FileMeta fileMeta = new FileMeta();
        if (userFileObject == null) {
            userFileObject = new BasicBSONObject();
        }

        String fieldName = FieldName.FIELD_CLFILE_NAME;

        String fileName = BsonUtils.getString(userFileObject, FieldName.FIELD_CLFILE_NAME);
        ScmFileOperateUtils.checkFileName(
                ScmContentModule.getInstance().getWorkspaceInfoCheckExist(ws), fileName);
        fileMeta.name = fileName;

        fileMeta.author = BsonUtils.getString(userFileObject, FieldName.FIELD_CLFILE_FILE_AUTHOR);

        fileMeta.title = BsonUtils.getString(userFileObject, FieldName.FIELD_CLFILE_FILE_TITLE);

        fileMeta.mimeType = BsonUtils.getStringOrElse(userFileObject,
                FieldName.FIELD_CLFILE_FILE_MIME_TYPE, MimeType.OCTET_STREAM.getType());

        fileMeta.classId = BsonUtils.getString(userFileObject,
                FieldName.FIELD_CLFILE_FILE_CLASS_ID);

        BSONObject classValue = BsonUtils.getBSON(userFileObject,
                FieldName.FIELD_CLFILE_PROPERTIES);
        classValue = BsonUtils.deepCopyRecordBSON(classValue);
        MetaDataManager.getInstence().checkPropeties(ws, fileMeta.classId, classValue);
        fileMeta.classProperties = ScmArgumentChecker.checkAndCorrectClass(classValue, fieldName);


        BSONObject tagsValue = BsonUtils.getBSON(userFileObject, FieldName.FIELD_CLFILE_TAGS);
        tagsValue = BsonUtils.deepCopyRecordBSON(tagsValue);
        Set<Object> tagsSet = ScmArgumentChecker.checkAndCorrectTags(tagsValue);
        BasicBSONList tagsBson = new BasicBSONList();
        tagsBson.addAll(tagsSet);
        fileMeta.tags = tagsBson;

        BSONObject extData = BsonUtils.getBSON(userFileObject,
                FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);
        extData = BsonUtils.deepCopyRecordBSON(extData);
        BSONObject newExtData = new BasicBSONObject();
        if (extData != null) {
            newExtData.putAll(extData);
        }
        fileMeta.externalData = extData;

        BSONObject customMeta = BsonUtils.getBSON(userFileObject,
                FieldName.FIELD_CLFILE_CUSTOM_METADATA);
        customMeta = BsonUtils.deepCopyRecordBSON(customMeta);
        BSONObject newCustomMeta = new BasicBSONObject();
        if (customMeta != null) {
            newCustomMeta.putAll(customMeta);
        }
        fileMeta.customMeta = newCustomMeta;

        fileMeta.majorVersion = 1;
        fileMeta.minorVersion = 0;
        fileMeta.type = 1;
        fileMeta.siteList = new BasicBSONList();
        fileMeta.dataId = null;
        fileMeta.dataCreateTime = null;

        fileMeta.user = user;

        fileMeta.id = BsonUtils.getString(userFileObject, FieldName.FIELD_CLFILE_ID);
        Number fileCreateTime = BsonUtils.getNumber(userFileObject,
                FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        if (fileCreateTime != null) {
            fileMeta.createTime = fileCreateTime.longValue();
            fileMeta.updateTime = fileMeta.createTime;
            if (fileMeta.id != null) {
                ScmIdParser idParser = new ScmIdParser(fileMeta.id);
                if (fileCreateTime.longValue() / 1000 != idParser.getSecond()) {
                    throw new ScmInvalidArgumentException(
                            "The specified fileID and createTime is conflict, creatTime="
                                    + new Date(fileCreateTime.longValue()));
                }
            }
            fileMeta.createMonth = ScmSystemUtils
                    .getCurrentYearMonth(new Date(fileMeta.createTime));
        }

        // Date fileCreateDate = new Date(fileCreateTime);
        //
        // fileMeta.id = ScmIdGenerator.FileId.get(fileCreateDate);
        // fileMeta.createTime = fileCreateTime;
        // fileMeta.createMonth = ScmSystemUtils.getCurrentYearMonth(fileCreateDate);

        fileMeta.updateUser = user;
        fileMeta.status = ServiceDefine.FileStatus.NORMAL;
        fileMeta.transId = "";
        fileMeta.dataType = ENDataType.Normal.getValue();
        fileMeta.size = 0;
        fileMeta.md5 = null;
        fileMeta.etag = null;
        fileMeta.versionSerial = null;
        fileMeta.deleteMarker = false;

        fileMeta.bucketId = null;
        fileMeta.dirId = null;
        fileMeta.batchId = "";

        return fileMeta;

    }

    public void resetDataInfo(String dataId, long dataCreateTime, int dataType, long dataSize,
            String md5, int dataSite, int wsVersion) {
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
        BasicBSONList sites = new BasicBSONList();
        sites.add(oneSite);
        this.siteList = sites;
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

    @Override
    public FileMeta clone() {
        return new FileMeta(this);
    }

    public BSONObject toBSONObject() {
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
        bson.put(FieldName.FIELD_CLFILE_TAGS, tags);
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
        return bson;
    }

    // 根据入参 fileCurrentLatestVersion 重置当前对象的文件全局属性，生成版本号
    public void newVersionFrom(FileMeta fileCurrentLatestVersion) throws ScmServerException {
        BSONObject standardBson = fileCurrentLatestVersion.toBSONObject();
        BSONObject myBson = toBSONObject();
        ScmFileVersionHelper.resetNewFileVersion(myBson, standardBson);
        setValueFromBSON(this, myBson);
    }

    public static FileMeta fromRecord(BSONObject fileRecord) throws ScmServerException {
        FileMeta fileMeta = new FileMeta();
        setValueFromBSON(fileMeta, fileRecord);
        return fileMeta;
    }

    private static void setValueFromBSON(FileMeta fileMeta, BSONObject bson)
            throws ScmServerException {
        fileMeta.id = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_ID);
        fileMeta.name = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_NAME);
        Number num = BsonUtils.getNumber(bson, FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        if (num != null) {
            fileMeta.bucketId = num.longValue();
        }

        fileMeta.dirId = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_DIRECTORY_ID);
        fileMeta.batchId = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_BATCH_ID);
        fileMeta.minorVersion = BsonUtils.getIntegerChecked(bson,
                FieldName.FIELD_CLFILE_MINOR_VERSION);
        fileMeta.majorVersion = BsonUtils.getIntegerChecked(bson,
                FieldName.FIELD_CLFILE_MAJOR_VERSION);
        fileMeta.type = BsonUtils.getIntegerOrElse(bson, FieldName.FIELD_CLFILE_TYPE, 1);
        fileMeta.classId = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_CLASS_ID);
        fileMeta.classProperties = BsonUtils.getBSON(bson, FieldName.FIELD_CLFILE_PROPERTIES);
        fileMeta.classProperties = BsonUtils.deepCopyRecordBSON(fileMeta.classProperties);
        fileMeta.tags = BsonUtils.getBSON(bson, FieldName.FIELD_CLFILE_TAGS);
        fileMeta.tags = BsonUtils.deepCopyRecordBSON(fileMeta.tags);
        fileMeta.user = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_INNER_USER);
        fileMeta.updateUser = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_INNER_UPDATE_USER);
        num = BsonUtils.getNumber(bson, FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        if (num != null) {
            fileMeta.createTime = num.longValue();
        }
        num = BsonUtils.getNumber(bson, FieldName.FIELD_CLFILE_INNER_UPDATE_TIME);
        if (num != null) {
            fileMeta.updateTime = num.longValue();
        }
        fileMeta.createMonth = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_INNER_CREATE_MONTH);
        num = BsonUtils.getNumber(bson, FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME);
        if (num != null) {
            fileMeta.dataCreateTime = num.longValue();
        }
        fileMeta.dataType = BsonUtils.getIntegerOrElse(bson, FieldName.FIELD_CLFILE_FILE_DATA_TYPE,
                ENDataType.Normal.getValue());
        num = BsonUtils.getNumber(bson, FieldName.FIELD_CLFILE_FILE_SIZE);
        if (num != null) {
            fileMeta.size = num.longValue();
        }
        fileMeta.dataId = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_DATA_ID);
        fileMeta.siteList = BsonUtils.getArray(bson, FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        fileMeta.siteList = (BasicBSONList) BsonUtils.deepCopyRecordBSON(fileMeta.siteList);
        fileMeta.title = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_TITLE);
        fileMeta.author = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_AUTHOR);
        fileMeta.mimeType = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_MIME_TYPE);
        fileMeta.md5 = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_MD5);
        fileMeta.etag = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_FILE_ETAG);
        fileMeta.externalData = BsonUtils.getBSON(bson, FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);
        fileMeta.externalData = BsonUtils.deepCopyRecordBSON(fileMeta.externalData);
        fileMeta.customMeta = BsonUtils.getBSON(bson, FieldName.FIELD_CLFILE_CUSTOM_METADATA);
        fileMeta.customMeta = BsonUtils.deepCopyRecordBSON(fileMeta.customMeta);
        fileMeta.versionSerial = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_VERSION_SERIAL);
        fileMeta.deleteMarker = BsonUtils.getBooleanOrElse(bson,
                FieldName.FIELD_CLFILE_DELETE_MARKER, false);
        fileMeta.status = BsonUtils.getIntegerOrElse(bson, FieldName.FIELD_CLFILE_EXTRA_STATUS,
                ServiceDefine.FileStatus.NORMAL);
        fileMeta.transId = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_EXTRA_TRANS_ID);
    }

    public String getSimpleDesc() {
        return "fileId=" + id + ", fileName=" + name + ", size=" + size + ", isDeleteMarker="
                + deleteMarker + ", version=" + majorVersion + "." + minorVersion;
    }
}
