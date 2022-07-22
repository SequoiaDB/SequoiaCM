package com.sequoiacm.client.element;

import java.util.Date;

import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.CommonDefine;
import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;

/**
 * The brief and partial information of File.
 *
 * @since 2.1
 */
public class ScmFileBasicInfo {
    private String fileName;
    private ScmId fileId;
    private int majorVersion;
    private int minorVersion;
    private String mimeType;
    private String user;
    private Date createDate;
    private boolean isDeleteMarker;
    private ScmVersionSerial versionSerial;

    /**
     * Create a instance of ScmFileBasicInfo.
     *
     * @param bson
     *            a bson containing basic information about scm file.
     * @throws ScmException
     *             If error happens
     * @since 2.1
     *
     */
    public ScmFileBasicInfo(BSONObject bson) throws ScmException {
        fileId = new ScmId((String) bson.get(FieldName.FIELD_CLFILE_ID), false);
        fileName = (String) bson.get(FieldName.FIELD_CLFILE_NAME);
        majorVersion = (Integer) bson.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        minorVersion = (Integer) bson.get(FieldName.FIELD_CLFILE_MINOR_VERSION);
        mimeType = (String) bson.get(FieldName.FIELD_CLFILE_FILE_MIME_TYPE);
        user = (String) bson.get(FieldName.FIELD_CLFILE_INNER_USER);
        createDate = new Date(
                CommonHelper.toLongValue(bson.get(FieldName.FIELD_CLFILE_INNER_CREATE_TIME)));
        isDeleteMarker = BsonUtils.getBooleanOrElse(bson, FieldName.FIELD_CLFILE_DELETE_MARKER,
                false);
        String versionSerialStr = BsonUtils.getString(bson, FieldName.FIELD_CLFILE_VERSION_SERIAL);
        if (versionSerialStr != null) {
            this.versionSerial = new ScmVersionSerial(versionSerialStr);
        }
        else {
            this.versionSerial = new ScmVersionSerial(majorVersion, minorVersion);
        }
    }

    /**
     * Sets the value of the MimeType property.
     *
     * @param mimeType
     *            mimeType
     * @since 3.0
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Returns the value of the MimeType property.
     *
     * @return mime type.
     * @since 3.0
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the value of the UserName property.
     *
     * @param user
     *            user
     * @since 3.0
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Returns the value of the UserName property.
     *
     * @return user name.
     * @since 3.0
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the value of the CreateDate property.
     *
     * @param createDate
     *            createDate
     * @since 3.0
     */
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    /**
     * Returns the value of the CreateDate property.
     *
     * @return create name.
     * @since 3.0
     */
    public Date getCreateDate() {
        return createDate;
    }

    /**
     * Create a instance of ScmFileBasicInfo,this instance's properties is null.
     *
     * @since 2.1
     */
    public ScmFileBasicInfo() {
    }

    /**
     * Returns the value of the FileName property.
     *
     * @return File name.
     * @since 2.1
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the value of the FileName property.
     *
     * @param fileName
     *            File name.
     * @since 2.1
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Returns the value of the FileId property.
     *
     * @return File id.
     * @since 2.1
     */
    public ScmId getFileId() {
        return fileId;
    }

    /**
     * Sets the value of the FileId property.
     *
     * @param fileId
     *            id.
     * @since 2.1
     */
    public void setFileId(ScmId fileId) {
        this.fileId = fileId;
    }

    /**
     * Returns the value of the MajorVersion property.
     *
     * @return Major version
     * @since 2.1
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Sets the value of the MajorVersion property.
     *
     * @param majorVersion
     *            Major version
     * @since 2.1
     */
    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    /**
     * Returns the value of the MinorVersion property.
     *
     * @return Minor version
     * @since 2.1
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Sets the value of the MinorVersion property.
     *
     * @param minorVersion
     *            Minor version
     * @since 2.1
     */
    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    /**
     * Returns a description of file basic information.
     *
     * @return File information.
     * @since 2.1
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("{");
        buf.append("fileName : \"" + fileName + "\" , ");
        buf.append("fileID : \"" + fileId.get() + "\" , ");
        buf.append("majorVersion : " + majorVersion + " , ");
        buf.append("minorVersion : " + minorVersion + " , ");
        buf.append("user : " + user + " , ");
        buf.append("createDate : " + createDate + " , ");
        buf.append("mimeType : " + mimeType + " , ");
        buf.append("delete_marker : " + isDeleteMarker);
        buf.append("}");
        return buf.toString();
    }

    /**
     * Return the file is null version or not
     *
     * @return return true if is null version.
     */
    public boolean isNullVersion() {
        return majorVersion == CommonDefine.File.NULL_VERSION_MAJOR
                && minorVersion == CommonDefine.File.NULL_VERSION_MINOR;
    }

    /**
     *
     * Return the file version serial, useful for fetch the version serial of null
     * version.
     *
     * @return version serial.
     */
    public ScmVersionSerial getVersionSerial() {
        return versionSerial;
    }

    /**
     * Sets the file of the version serial.
     * 
     * @param versionSerial
     *            version serial.
     */
    public void setVersionSerial(ScmVersionSerial versionSerial) {
        this.versionSerial = versionSerial;
    }

    /**
     * Judges whether current file is delete marker.
     *
     * @return return is delete marker ot not.
     */
    public boolean isDeleteMarker() {
        return isDeleteMarker;
    }
}