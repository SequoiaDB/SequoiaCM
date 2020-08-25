package com.sequoiacm.client.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ClientDefine;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.dispatcher.CloseableFileDataEntity;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.client.util.ScmHelper;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.exception.ScmError;

/**
 * The implement of ScmFile.
 */
class ScmFileImpl extends ScmFile {

    private ScmFileBasicInfo basicInfo;
    // private PropertyType propertyType;
    private ScmId batchId;
    private String directoryId;

    private String title;
    // private String mimeType;

    private ScmId classId;
    private ScmClassProperties classProperties;
    private ScmTags tags;

    private String author;
    // private Date createTime;
    // private String user;
    private String updateUser;
    private Date updateTime;

    private List<ScmFileLocation> locationList;

    private ScmId dataId;
    private Date dataCreateTime;

    private long size;
    private ScmWorkspace ws;

    private boolean exist;
    private boolean isDeleted = false;

    private String inputPath = null;
    private InputStream inputStream = null;
    private ScmBreakpointFile breakpointFile;
    private String md5;

    private static final Logger logger = LoggerFactory.getLogger(ScmFileImpl.class);

    public ScmFileImpl() {
        basicInfo = new ScmFileBasicInfo();
        exist = false;
        tags = new ScmTags();
    }

    /* getter and setter */
    @Override
    public ScmId getFileId() {
        return basicInfo.getFileId();
    }

    @Override
    void setFileId(ScmId fileId) {
        this.basicInfo.setFileId(fileId);
    }

    @Override
    public int getMajorVersion() {
        return basicInfo.getMajorVersion();
    }

    @Override
    void setMajorVersion(int majorVersion) {
        this.basicInfo.setMajorVersion(majorVersion);
    }

    @Override
    public int getMinorVersion() {
        return basicInfo.getMinorVersion();
    }

    @Override
    void setMinorVersion(int minorVersion) {
        this.basicInfo.setMinorVersion(minorVersion);
    }

    /*
     * TODO
     *
     * @Override public PropertyType getPropertyType() { return propertyType; }
     *
     * @Override public void setPropertyType(PropertyType type) throws
     * ScmException { if (isExist()) { BSONObject fileInfo = new
     * BasicBSONObject(); fileInfo.put(FieldName.FIELD_CLFILE_FILE_CLASS_ID,
     * type.getNum()); updateFileInfo(fileInfo); } this.propertyType = type; }
     */

    @Override
    public ScmId getBatchId() {
        return batchId;
    }

    public void setBatchId(ScmId batchId) {
        this.batchId = batchId;
    }

    @Override
    public ScmDirectory getDirectory() throws ScmException {
        return ScmFactory.Directory.getInstance(ws, directoryId, null);
    }

    void setDirectoryId(String id) {
        this.directoryId = id;
    }

    @Override
    public void setDirectory(ScmDirectory directory) throws ScmException {
        setDirectory(directory.getId());
    }

    @Override
    public String getWorkspaceName() {
        return ws.getName();
    }

    @Override
    public String getFileName() {
        return basicInfo.getFileName();
    }

    @Override
    public void setFileName(String fileName) throws ScmException {
        if (!ScmArgChecker.File.checkFileName(fileName)) {
            throw new ScmInvalidArgumentException("invlid fileName:fileName=" + fileName);
        }
        if (isExist()) {
            BSONObject fileInfo = new BasicBSONObject();
            fileInfo.put(FieldName.FIELD_CLFILE_NAME, fileName);
            updateFileInfo(fileInfo);
        }
        this.basicInfo.setFileName(fileName);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) throws ScmException {
        if (isExist()) {
            BSONObject fileInfo = new BasicBSONObject();
            fileInfo.put(FieldName.FIELD_CLFILE_FILE_TITLE, title);
            updateFileInfo(fileInfo);
        }
        this.title = title;
    }

    @Override
    public MimeType getMimeTypeEnum() {
        return MimeType.get(this.basicInfo.getMimeType());
    }

    @Override
    public String getMimeType() {
        return this.basicInfo.getMimeType();
    }

    @Override
    public void setMimeType(MimeType mimeType) throws ScmException {
        if (isExist()) {
            BSONObject fileInfo = new BasicBSONObject();
            fileInfo.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, mimeType.getType());
            updateFileInfo(fileInfo);
        }

        this.basicInfo.setMimeType(mimeType.getType());
    }

    @Override
    public void setMimeType(String mimeType) throws ScmException {
        if (isExist()) {
            BSONObject fileInfo = new BasicBSONObject();
            fileInfo.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, mimeType);
            updateFileInfo(fileInfo);
        }

        this.basicInfo.setMimeType(mimeType);
    }

    @Override
    public ScmId getClassId() {
        return classId;
    }

    @Override
    public ScmClassProperties getClassProperties() {
        return classProperties;
    }

    @Override
    public void setClassProperty(String key, Object value) throws ScmException {
        if (null == classId) {
            throw new ScmException(ScmError.FILE_CLASS_UNDEFINED,
                    "The file does not specify a class");
        }

        checkArgNotNull(FieldName.FIELD_CLFILE_PROPERTIES + "'s key", key);
        if (isExist()) {
            BSONObject fileInfo = new BasicBSONObject();
            fileInfo.put(FieldName.FIELD_CLFILE_PROPERTIES + "." + key, value);
            updateFileInfo(fileInfo);
        }
        this.classProperties.addProperty(key, value);
    }

    @Override
    public void setClassProperties(ScmClassProperties classProperties) throws ScmException {
        if (null == classProperties) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "file class properties cannot be set null");
        }
        if (isExist()) {
            BSONObject fileInfo = new BasicBSONObject();
            BSONObject prop = new BasicBSONObject();
            String cid = null;
            if (classProperties != null) {
                cid = classProperties.getClassId();
                Set<String> keySet = classProperties.keySet();
                for (String key : keySet) {
                    checkArgNotNull(FieldName.FIELD_CLFILE_PROPERTIES + "'s key", key);
                    prop.put(key, classProperties.getProperty(key));
                }
            }
            fileInfo.put(FieldName.FIELD_CLFILE_FILE_CLASS_ID, cid);
            fileInfo.put(FieldName.FIELD_CLFILE_PROPERTIES, prop);
            updateFileInfo(fileInfo);
        }

        this.classId = new ScmId(classProperties.getClassId(), false);
        this.classProperties = classProperties;
    }

    @Override
    public ScmTags getTags() {
        return tags;
    }

    @Override
    public void setTags(ScmTags tags) throws ScmException {
        if (tags == null) {
            tags = new ScmTags();
        }
        if (isExist()) {
            BSONObject fileInfo = new BasicBSONObject();
            fileInfo.put(FieldName.FIELD_CLFILE_TAGS, tags.toSet());
            updateFileInfo(fileInfo);
        }
        this.tags = tags;
    }

    @Override
    public void addTag(String tag) throws ScmException {
        tags.addTag(tag);
        if (isExist()) {
            setTags(tags);
        }
    }

    @Override
    public void removeTag(String tag) throws ScmException {
        tags.removeTag(tag);
        if (isExist()) {
            setTags(tags);
        }
    }

    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public void setAuthor(String author) throws ScmException {
        if (isExist()) {
            if (null == author) {
                author = "";
            }
            BSONObject fileInfo = new BasicBSONObject();
            fileInfo.put(FieldName.FIELD_CLFILE_FILE_AUTHOR, author);
            updateFileInfo(fileInfo);
        }
        this.author = author;
    }

    @Override
    public String getUser() {
        return this.basicInfo.getUser();
    }

    @Override
    void setUser(String user) {
        this.basicInfo.setUser(user);
    }

    @Override
    public Date getCreateTime() {
        return this.basicInfo.getCreateDate();
    }

    @Override
    public void setCreateTime(Date createTime) throws ScmException {
        if (isExist()) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED,
                    "can't set create time when file is exist");
        }

        this.basicInfo.setCreateDate(createTime);
    }

    @Override
    public String getUpdateUser() {
        return updateUser;
    }

    @Override
    void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    @Override
    public Date getUpdateTime() {
        return updateTime;
    }

    @Override
    void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public void getContent(String outputPath, int flag) throws ScmException {
        if (outputPath == null) {
            throw new ScmInvalidArgumentException("outputPath is null");
        }

        if (outputPath.length() == 0) {
            throw new ScmInvalidArgumentException("outputPath is \"\"");
        }

        File file = null;
        OutputStream os = null;

        file = new File(outputPath);

        if (file.isDirectory()) {
            throw new ScmException(ScmError.FILE_IS_DIRECTORY,
                    outputPath + " is a directory and is not a file");
        }

        try {
            if (!file.createNewFile()) {
                throw new ScmException(ScmError.FILE_ALREADY_EXISTS, outputPath + " has existed.");
            }
        }
        catch (IOException e) {
            throw new ScmException(ScmError.FILE_IO,
                    file.getName() + " has io exception when created file", e);
        }
        catch (SecurityException e) {
            throw new ScmException(ScmError.FILE_PERMISSION,
                    file.getName() + " lacks permission to be created", e);
        }

        try {
            os = new FileOutputStream(file);
            getContent(os, flag);
        }
        catch (SecurityException se) {
            ScmHelper.closeStream(os);
            os = null;
            removeFile(file);
            throw new ScmException(ScmError.FILE_PERMISSION,
                    file.getName() + " lacks permission to be wrote", se);
        }
        catch (IOException e) {
            ScmHelper.closeStream(os);
            os = null;
            removeFile(file);
            throw new ScmException(ScmError.FILE_IO,
                    file.getName() + " has io error when opened outputStream", e);
        }
        catch (ScmException e) {
            ScmHelper.closeStream(os);
            os = null;
            removeFile(file);
            throw e;
        }
        finally {
            ScmHelper.closeStream(os);
            os = null;
        }
    }

    private void getContent(OutputStream os, int flag) throws ScmException {
        if (null == os) {
            throw new ScmInvalidArgumentException("outputStream is null");
        }
        ScmSession conn;
        CloseableFileDataEntity fileData = null;
        try {
            conn = ws.getSession();
            fileData = conn.getDispatcher().downloadFile(ws.getName(), basicInfo.getFileId().get(),
                    basicInfo.getMajorVersion(), basicInfo.getMinorVersion(), flag);
            long totalReadLen = writeContent(os, fileData);
            if (totalReadLen != fileData.getLength()) {
                throw new ScmException(ScmError.DATA_CORRUPTED,
                        "data is incomplete:workspace=" + ws.getName() + ",fileId="
                                + getFileId().get() + ",expectLen=" + getSize() + ",actual="
                                + totalReadLen);
            }
        }
        catch (IOException e) {
            throw new ScmException(ScmError.FILE_IO, "outputStream has io error when wrote data",
                    e);
        }
        finally {
            ScmHelper.closeStream(fileData);
        }
    }

    @Override
    public void getContent(String outputPath) throws ScmException {
        getContent(outputPath, CommonDefine.ReadFileFlag.SCM_READ_FILE_WITHDATA);
    }

    @Override
    public void getContent(OutputStream os) throws ScmException {
        getContent(os, CommonDefine.ReadFileFlag.SCM_READ_FILE_WITHDATA);
    }

    @Override
    public void getContentFromLocalSite(String outputPath) throws ScmException {
        getContent(outputPath, CommonDefine.ReadFileFlag.SCM_READ_FILE_WITHDATA
                | CommonDefine.ReadFileFlag.SCM_READ_FILE_LOCALSITE);
    }

    @Override
    public void getContentFromLocalSite(OutputStream os) throws ScmException {
        getContent(os, CommonDefine.ReadFileFlag.SCM_READ_FILE_WITHDATA
                | CommonDefine.ReadFileFlag.SCM_READ_FILE_LOCALSITE);
    }

    private long writeContent(OutputStream os, CloseableFileDataEntity fileData)
            throws IOException {
        int len = 0;
        long totalRecvLen = 0;
        byte[] buf = new byte[ClientDefine.File.MAX_READ_BUFFER_LEN];
        while ((len = fileData.readAsMuchAsPossible(buf)) != -1) {
            totalRecvLen += len;
            os.write(buf, 0, len);
        }
        fileData.close();
        return totalRecvLen;
    }

    @Override
    public void setContent(InputStream inputStream) {
        this.inputStream = inputStream;
        this.inputPath = null;
        this.breakpointFile = null;
    }

    @Override
    public void setContent(ScmBreakpointFile breakpointFile) {
        this.breakpointFile = breakpointFile;
        this.inputStream = null;
        this.inputPath = null;
    }

    @Override
    public void setContent(String inputPath) {
        this.inputPath = inputPath;
        this.inputStream = null;
        this.breakpointFile = null;
    }

    public void setWorkspace(ScmWorkspace ws) {
        this.ws = ws;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    void setSize(long size) {
        this.size = size;
    }

    @Override
    void setExist(boolean exist) {
        this.exist = exist;
    }

    @Override
    public boolean isExist() {
        return exist;
    }

    @Override
    public ScmId save() throws ScmException {
        return save(null);
    }

    @Override
    public ScmId save(ScmUploadConf conf) throws ScmException {
        if (conf == null) {
            conf = new ScmUploadConf(false);
        }
        if (isExist()) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED,
                    "file already exists, save operation can't be done repeatedly");
        }

        if (getFileName() == null) {
            throw new ScmInvalidArgumentException("file name is null");
        }
        BSONObject fileInfo = null;

        if (breakpointFile == null) {
            fileInfo = uploadFile(conf);
        }
        else {
            fileInfo = saveBreakpointFile(conf);
        }

        refresh(fileInfo);
        setExist(true);
        return getFileId();
    }

    private BSONObject uploadFile(ScmUploadConf uploadConf) throws ScmException {
        InputStream is = null;

        if (null != inputStream) {
            is = inputStream;
        }

        if (null != inputPath) {
            is = getInputStream(inputPath);
        }

        try {
            return ws.getSession().getDispatcher().uploadFile(ws.getName(), is, toBSONObject(),
                    uploadConf.toBsonObject());
        }
        finally {
            ScmHelper.closeStream(is);
        }
    }

    private InputStream getInputStream(String inputPath) throws ScmException {
        File file = new File(inputPath);

        if (file.isDirectory()) {
            throw new ScmException(ScmError.FILE_IS_DIRECTORY,
                    inputPath + " is a directory and is not a file");
        }

        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            return is;
        }
        catch (FileNotFoundException e) {
            throw new ScmException(ScmError.FILE_NOT_EXIST, inputPath + " is not found", e);
        }
        catch (SecurityException se) {
            throw new ScmException(ScmError.FILE_PERMISSION,
                    file.getName() + " lacks permission to be read", se);
        }
    }

    private BSONObject saveBreakpointFile(ScmUploadConf uploadConf) throws ScmException {
        if (!breakpointFile.isCompleted()) {
            throw new ScmInvalidArgumentException("BreakpointFile is uncompleted");
        }

        return ws.getSession().getDispatcher().uploadFile(ws.getName(),
                breakpointFile.getFileName(), toBSONObject(), uploadConf.toBsonObject());
    }

    @Override
    public void checkout() throws ScmException {
        throw new ScmException(ScmError.OPERATION_UNSUPPORTED, "operation unsupported");
    }

    @Override
    public void checkin(ScmType type) throws ScmException {
        throw new ScmException(ScmError.OPERATION_UNSUPPORTED, "operation unsupported");
    }

    @Override
    public void revert(int preVersion) throws ScmException {
        throw new ScmException(ScmError.OPERATION_UNSUPPORTED, "operation unsupported");
    }

    @Override
    public void delete(boolean isPhysical) throws ScmException {
        if (isExist()) {
            ScmSession conn;
            conn = ws.getSession();
            conn.getDispatcher().deleteFile(ws.getName(), basicInfo.getFileId().get(),
                    basicInfo.getMajorVersion(), basicInfo.getMinorVersion(), isPhysical);
        }
    }

    @Override
    public void delete() throws ScmException {
        delete(false);
    }

    private void removeFile(File file) throws ScmException {
        try {
            if (null != file) {
                if (!file.delete()) {
                    throw new ScmException(ScmError.FILE_DELETE_FAILED,
                            file.getName() + " removed failed when gotten it's content failed");
                }
                file = null;
            }
        }
        catch (SecurityException se) {
            throw new ScmException(ScmError.FILE_PERMISSION,
                    file.getName() + " lacks permission to be deleted");
        }
    }

    private void updateFileInfo(BSONObject updateInfo) throws ScmException {
        BSONObject newFileInfo = ws.getSession().getDispatcher().updateFileInfo(ws.getName(),
                basicInfo.getFileId().get(), basicInfo.getMajorVersion(),
                basicInfo.getMinorVersion(), updateInfo);
        refresh(newFileInfo);
    }

    private MimeType findMimeTypeByInputPath() {
        if (null == inputPath) {
            return null;
        }

        String[] pathSplits = inputPath.split("\\.");
        if (1 < pathSplits.length) {
            return MimeType.getBySuffix(pathSplits[pathSplits.length - 1]);
        }
        return null;
    }

    public static ScmFileImpl getInstanceByBSONObject(BSONObject bsonObj) throws ScmException {
        ScmFileImpl file = new ScmFileImpl();
        file.refresh(bsonObj);
        return file;
    }

    @Override
    BSONObject toBSONObject() throws ScmException {
        BSONObject fileInfo = new BasicBSONObject();
        // the validate task works on contentserver.
        if (null != basicInfo.getFileName()) {
            fileInfo.put(FieldName.FIELD_CLFILE_NAME, basicInfo.getFileName());
        }
        else {
            fileInfo.put(FieldName.FIELD_CLFILE_NAME, "");
        }

        if (null != title) {
            fileInfo.put(FieldName.FIELD_CLFILE_FILE_TITLE, title);
        }
        else {
            fileInfo.put(FieldName.FIELD_CLFILE_FILE_TITLE, "");
        }

        if (null != getMimeType()) {
            fileInfo.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, getMimeType());
        }
        else {
            MimeType mt = findMimeTypeByInputPath();
            if (null != mt) {
                fileInfo.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, mt.getType());
                setMimeType(mt.getType());
            }
            else {
                fileInfo.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, "");
            }
        }

        if (null != directoryId) {
            fileInfo.put(FieldName.FIELD_CLFILE_DIRECTORY_ID, directoryId);
        }

        if (null != getCreateTime()) {
            fileInfo.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, getCreateTime().getTime());
        }

        /*
         * TODO propertyType? if (null != propertyType) {
         * fileInfo.put(FieldName.FIELD_CLFILE_FILE_CLASS_ID,
         * propertyType.getNum()); }
         */

        // optional
        if (null != classProperties) {
            String propFieldName = FieldName.FIELD_CLFILE_PROPERTIES;
            fileInfo.put(propFieldName, convertToBson(this.classProperties.toMap(), propFieldName));
            fileInfo.put(FieldName.FIELD_CLFILE_FILE_CLASS_ID, classProperties.getClassId());
        }

        if (null != tags) {
            fileInfo.put(FieldName.FIELD_CLFILE_TAGS, tags.toSet());
        }

        if (null != batchId) {
            fileInfo.put(FieldName.FIELD_CLFILE_BATCH_ID, batchId.get());
        }

        if (null != author) {
            fileInfo.put(FieldName.FIELD_CLFILE_FILE_AUTHOR, author);
        }
        else {
            author = "";
            fileInfo.put(FieldName.FIELD_CLFILE_FILE_AUTHOR, author);
        }

        return fileInfo;
    }

    private BSONObject convertToBson(Map<String, Object> map, String fieldName)
            throws ScmException {
        BSONObject prop = new BasicBSONObject();
        Set<String> keySet = map.keySet();
        for (String key : keySet) {
            checkArgNotNull(fieldName + "'s key", key);
            prop.put(key, map.get(key));
        }
        return prop;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        buf.append("fileId : \"").append(basicInfo.getFileId()).append("\" , ");
        buf.append("fileName : \"").append(basicInfo.getFileName()).append("\" , ");
        buf.append("majorVersion : ").append(basicInfo.getMajorVersion()).append(" , ");
        buf.append("minorVersion : ").append(basicInfo.getMinorVersion()).append(" , ");
        buf.append("workspaceName : \"").append(ws.getName()).append("\" , ");
        buf.append("title : \"").append(title).append("\" , ");
        buf.append("mimeType : \"").append(getMimeType()).append("\" , ");
        buf.append("classId : \"").append(classId).append("\" , ");

        if (batchId == null) {
            buf.append("batchId : \"null\" , ");
        }
        else {
            buf.append("batchId : \"").append(batchId.get()).append("\" , ");
        }

        if (directoryId == null) {
            buf.append("directoryId : \"null\" , ");
        }
        else {
            buf.append("directoryId : \"").append(directoryId).append("\" , ");
        }

        buf.append("class_properties : ").append(classProperties).append(" , ");
        buf.append("tags : ").append(tags).append(" , ");
        buf.append("author : \"").append(author).append("\" , ");
        buf.append("user : \"").append(getUser()).append("\", ");
        buf.append("createTime : \"").append(getCreateTime()).append("\" , ");
        buf.append("updateUser : \"").append(updateUser).append("\" , ");
        buf.append("updateTime : \"").append(updateTime).append("\" , ");
        buf.append("size : ").append(size);
        buf.append("md5: ").append(md5);
        buf.append("}");
        return buf.toString();
    }

    @Override
    public boolean isDeleted() {
        return isDeleted;
    }

    void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    ScmSession getSession() {
        return ws.getSession();
    }

    @Override
    ScmWorkspace getWorkspace() {
        return ws;
    }

    @Override
    public List<ScmFileLocation> getLocationList() {
        return locationList;
    }

    @Override
    void setLocationList(List<ScmFileLocation> locationList) {
        this.locationList = locationList;
    }

    @Override
    public ScmId getDataId() {
        return dataId;
    }

    @Override
    void setDataId(ScmId dataId) {
        this.dataId = dataId;
    }

    private void checkArgNotNull(String field, Object val) throws ScmException {
        if (null == val) {
            throw new ScmException(ScmError.INVALID_ARGUMENT, "file " + field + " cannot be null");
        }
    }

    @Override
    public void updateContent(InputStream is) throws ScmException {
        updateContent(is, new ScmUpdateContentOption());
    }

    @Override
    public void updateContent(String path) throws ScmException {
        if (path == null) {
            throw new ScmInvalidArgumentException("path is null");
        }
        InputStream is = getInputStream(path);
        try {
            updateContent(is);
        }
        finally {
            ScmHelper.closeStream(is);
        }
    }

    @Override
    public void updateContent(ScmBreakpointFile breakpointFile) throws ScmException {
        updateContent(breakpointFile, new ScmUpdateContentOption());
    }

    @Override
    void refresh(BSONObject fileBSON) throws ScmException {
        Object obj;
        obj = fileBSON.get(FieldName.FIELD_CLFILE_ID);
        if (null != obj) {
            basicInfo.setFileId(new ScmId((String) obj, false));
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_NAME);
        if (null != obj) {
            basicInfo.setFileName((String) obj);
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        if (null != obj) {
            basicInfo.setMajorVersion((Integer) obj);
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_MINOR_VERSION);
        if (null != obj) {
            basicInfo.setMinorVersion((Integer) obj);
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_BATCH_ID);
        if (null != obj) {
            String batID = (String) obj;
            if (batID.length() != 0) {
                batchId = new ScmId((String) obj, false);
            }
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        if (null != obj) {
            String fdID = (String) obj;
            directoryId = fdID;
        }

        // class properties
        Object cidobj = fileBSON.get(FieldName.FIELD_CLFILE_FILE_CLASS_ID);
        Object propobj = fileBSON.get(FieldName.FIELD_CLFILE_PROPERTIES);
        if (cidobj != null && propobj != null) {
            String cid = String.valueOf(cidobj);
            BSONObject prop = (BSONObject) propobj;
            ScmClassProperties properties = new ScmClassProperties(cid);
            Set<String> proKeySet = prop.keySet();
            for (String proKey : proKeySet) {
                properties.addProperty(proKey, prop.get(proKey));
            }
            this.classId = new ScmId(properties.getClassId(), false);
            this.classProperties = properties;
        }

        // tags
        obj = fileBSON.get(FieldName.FIELD_CLFILE_TAGS);
        if (null != obj) {
            ScmTags tags = new ScmTags();
            BasicBSONList bsonList = null;
            try {
                bsonList = (BasicBSONList) obj;
                for (Object tag : bsonList) {
                    tags.addTag((String) tag);
                }
            }
            catch (ClassCastException e) {
                // tags compatibility processing : { "tagkey" : "tagvalue"} cast
                // to ["tagkey : tagvalue"]
                BSONObject bsonMap = (BSONObject) obj;
                Set<String> tagKeys = bsonMap.keySet();
                for (String tagKey : tagKeys) {
                    tags.addTag(tagKey + " : " + bsonMap.get(tagKey));
                }
                logger.warn("tags compatibility processing: old tags map={}, new tags set={}", obj,
                        tags.toString());
            }
            this.tags = tags;
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_FILE_AUTHOR);
        if (null != obj) {
            this.author = (String) obj;
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_INNER_USER);
        if (null != obj) {
            this.basicInfo.setUser((String) obj);
        }

        // createTime
        obj = fileBSON.get(FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        if (null != obj) {
            Long ts = CommonHelper.toLongValue(obj);
            this.basicInfo.setCreateDate(new Date(ts));
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_INNER_UPDATE_USER);
        if (null != obj) {
            this.updateUser = (String) obj;
        }

        // updateTime
        obj = fileBSON.get(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME);
        if (null != obj) {
            Long ts = CommonHelper.toLongValue(obj);
            this.updateTime = new Date(ts);
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_FILE_SIZE);
        if (null != obj) {
            this.size = CommonHelper.toLongValue(obj);
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_FILE_TITLE);
        if (null != obj) {
            this.title = (String) obj;
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_FILE_MIME_TYPE);
        if (null != obj) {
            this.basicInfo.setMimeType((String) obj);
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILEHISTORY_FLAG);
        if (null != obj) {
            int flag = (Integer) obj;
            if (1 == flag) {
                this.isDeleted = true;
            }
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        if (null != obj) {
            BasicBSONList siteList = (BasicBSONList) obj;
            List<ScmFileLocation> location = new ArrayList<ScmFileLocation>();
            for (Object siteBson : siteList) {
                location.add(new ScmFileLocation((BSONObject) siteBson));
            }
            this.locationList = location;
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        if (null != obj) {
            this.dataId = new ScmId((String) obj, false);
        }

        obj = fileBSON.get(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME);
        if (null != obj) {
            this.dataCreateTime = new Date(CommonHelper.toLongValue(obj));
        }

        md5 = BsonUtils.getString(fileBSON, FieldName.FIELD_CLFILE_FILE_MD5);
    }

    @Override
    public Date getDataCreateTime() {
        return dataCreateTime;
    }

    @Override
    public void setDirectory(String directoryId) throws ScmException {
        if (isExist()) {
            BSONObject fileInfo = new BasicBSONObject();
            fileInfo.put(FieldName.FIELD_CLFILE_DIRECTORY_ID, directoryId);
            updateFileInfo(fileInfo);
        }
        this.directoryId = directoryId;
    }

    @Override
    public void updateContent(InputStream is, ScmUpdateContentOption option) throws ScmException {
        checkArgNotNull("is", is);
        checkArgNotNull("option", option);
        BSONObject newFileInfo = ws.getSession().getDispatcher().updateFileContent(
                getWorkspaceName(), getFileId().get(), getMajorVersion(), getMinorVersion(), is,
                option.toBson());
        refresh(newFileInfo);
    }

    @Override
    public void updateContent(ScmBreakpointFile breakpointFile, ScmUpdateContentOption option)
            throws ScmException {
        checkArgNotNull("breakpointFile", breakpointFile);
        checkArgNotNull("option", option);
        BSONObject newFileInfo = ws.getSession().getDispatcher().updateFileContent(
                getWorkspaceName(), getFileId().get(), getMajorVersion(), getMinorVersion(),
                breakpointFile.getFileName(), option.toBson());
        refresh(newFileInfo);
    }

    @Override
    public String getMd5() {
        return md5;
    }

    @Override
    public void calcMd5() throws ScmException {
        if (md5 != null) {
            return;
        }

        md5 = ws.getSession().getDispatcher().calcScmFileMd5(ws.getName(), getFileId().get(),
                getMajorVersion(), getMinorVersion());
    }

}