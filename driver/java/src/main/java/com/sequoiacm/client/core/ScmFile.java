package com.sequoiacm.client.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.sequoiacm.client.element.ScmContentLocation;
import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.common.ScmUpdateContentOption;

/**
 * The interface of ScmFile.
 *
 * @since 2.1
 */
public abstract class ScmFile {

    abstract ScmSession getSession();

    /**
     * Returns the value of the FileId property.
     *
     * @return File id.
     * @since 2.1
     */
    public abstract ScmId getFileId();

    /**
     * Returns the value of the MajorVersion property.
     *
     * @return Major version.
     * @since 2.1
     */
    public abstract int getMajorVersion();

    /**
     * Returns the value of the MinorVersion property.
     *
     * @return Minor version.
     * @since 2.1
     */
    public abstract int getMinorVersion();

    /**
     * Returns the value of the PropertyType property.
     *
     * @return Property type.
     * @since 2.1
     */
    // public abstract PropertyType getPropertyType();

    /**
     * Sets or Updates the value of the PropertyType property.
     *
     * @param type
     *            Property type for this file.
     * @throws ScmException
     *             If error happens if error happens
     * @since 2.1
     */
    // public abstract void setPropertyType(PropertyType type) throws
    // ScmException;

    /**
     * Returns the value of the BatchId property.
     *
     * @return Batch id.
     * @since 2.1
     */
    public abstract ScmId getBatchId();

    /**
     * Returns the value of the Directory.
     *
     * @return The directory instance.
     * @since 2.1
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmDirectory getDirectory() throws ScmException;

    /**
     * Sets or Updates the value of the Directory property.
     *
     * @param directory
     *            Directory object.
     * @throws ScmException
     *             If error happens.
     * @since 2.1
     */
    public abstract void setDirectory(ScmDirectory directory) throws ScmException;

    /**
     * Returns the value of the Workspace property name.
     *
     * @return Workspace name
     * @since 2.1
     */
    public abstract String getWorkspaceName();

    /**
     * Returns the value of the FileName property.
     *
     * @return File name
     * @since 2.1
     */
    public abstract String getFileName();

    /**
     * Sets Or Updates the value of the FileName property.
     *
     * @param name
     *            File name.
     *            <dl>
     *            <dt>name can't be null,empty string,or dot(.).
     *                also,name can't contain special characters like / \\ % ; : * ? &quot; &lt; &gt; |
     *            </dt>
     *            </dl>
     * @throws ScmException
     *             If error happens If error happens.
     *
     * @since 2.1
     */
    public abstract void setFileName(String name) throws ScmException;

    /**
     * Returns the value of the Title property.
     *
     * @return File title
     * @since 2.1
     */
    public abstract String getTitle();

    /**
     * Sets Or Updates the value of the Title property.
     *
     * @param title
     *            File title.
     * @throws ScmException
     *             If error happens If error happens.
     *
     * @since 2.1
     */
    public abstract void setTitle(String title) throws ScmException;

    /**
     * Returns the enum of the MimeType.
     *
     * @return the enum of Mime type
     * @since 2.1
     */
    public abstract MimeType getMimeTypeEnum();

    /**
     * Returns the value of the MimeType property.
     *
     * @return the value of Mime Type
     * @since 2.1
     */
    public abstract String getMimeType();

    /**
     * Sets or Updates the value of the MimeType property.
     *
     * @param mimeType
     *            Mime type
     *
     * @throws ScmException
     *             If error happens If error happens
     * @since 2.1
     */
    public abstract void setMimeType(MimeType mimeType) throws ScmException;

    /**
     * Sets or Updates the value of the MimeType property.
     *
     * @param mimeType
     *            type.
     * @throws ScmException
     *             If error happens If error happens.
     * @since 2.1
     */
    public abstract void setMimeType(String mimeType) throws ScmException;

    /**
     * Returns the value of the File ClassId.
     *
     * @return The file classId.
     * @since 2.1
     */
    public abstract ScmId getClassId();

    /**
     * Returns the value of the Properties property.
     *
     * @return File class properties
     * @since 2.1
     */
    public abstract ScmClassProperties getClassProperties();

    /**
     * Set or Update the single class property.
     *
     * @param key
     *            key
     * @param value
     *            value
     * @throws ScmException
     *             If error happens If error happens.
     */
    public abstract void setClassProperty(String key, Object value) throws ScmException;

    /**
     * Sets or Updates the value of the Properties property.
     *
     * @param properties
     *            File properties.
     * @throws ScmException
     *             If error happens If error happens.
     * @since 2.1
     */
    public abstract void setClassProperties(ScmClassProperties properties) throws ScmException;

    /**
     * Returns the value of the Tags.
     *
     * @return File tags
     * @since 2.1
     */
    public abstract ScmTags getTags();

    /**
     * Sets or Updates the value of the Tags property.
     *
     * @param tags
     *            the tags.
     * @throws ScmException
     *             If error happens If error happens.
     * @since 2.1
     */
    public abstract void setTags(ScmTags tags) throws ScmException;

    /**
     * add the value of the Tags property.
     *
     * @param tag
     *            the tag
     * @throws ScmException
     *             If error happens If error happens.
     * @since 2.1
     */
    public abstract void addTag(String tag) throws ScmException;

    /**
     * remove the value of the Tags property.
     *
     * @param tag
     *            the tag
     * @throws ScmException
     *             If error happens If error happens.
     * @since 2.1
     */
    public abstract void removeTag(String tag) throws ScmException;

    /**
     * Returns the value of the Author property.
     *
     * @return File author.
     * @since 2.1
     */
    public abstract String getAuthor();

    /**
     * Sets or Updates the value of the Author property.
     *
     * @param author
     *            File author
     * @throws ScmException
     *             If error happens.
     * @since 2.1
     */
    public abstract void setAuthor(String author) throws ScmException;

    /**
     * Returns the value of the User property.
     *
     * @return User.
     * @since 2.1
     */
    public abstract String getUser();

    /**
     * Returns the value of the CreateTime property.
     *
     * @return CreateTime.
     * @since 2.1
     */
    public abstract Date getCreateTime();

    /**
     * Returns the value of the UpdateUser property.
     *
     * @return UpdateUser.
     * @since 2.1
     */
    public abstract String getUpdateUser();

    /**
     * Returns the value of the UpdateTime property.
     *
     * @return UpdateTime
     * @since 2.1
     */
    public abstract Date getUpdateTime();

    /**
     * Returns the value of Size property.
     *
     * @return File size.
     * @since 2.1
     */
    public abstract long getSize();

    /**
     * Obtains file content and saves into output path.
     *
     * @param outputPath
     *            A system full path to storage file content. It should be not
     *            existed.
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    public abstract void getContent(String outputPath) throws ScmException;

    /**
     * Obtains file content and saves into output stream.
     *
     * @param os
     *            An output stream to storage file content. It should be valid.
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    public abstract void getContent(OutputStream os) throws ScmException;

    /**
     * Obtains file content and saves into output stream.
     *
     * @param os
     *            An output stream to storage file content. It should  be valid.
     * @param readFlag
     *            the read flags. Please see the description of follow flags for more detail,
     *            and can also specify 0 to not configure.
     *            <dl>
     *            <dt>CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE
     *            :do not cache when reading file across sites
     *            </dl>
     * @throws ScmException
     *            If error happens
     * @since 3.1.2
     */
    public abstract void getContent(OutputStream os, int readFlag) throws ScmException;

    /**
     * Loads file content from input path.
     *
     * @param inputPath
     *            A system full path to read file content. It should have been
     *            existed.
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    public abstract void setContent(String inputPath) throws ScmException;

    /**
     * Loads file content from input stream.
     *
     * @param is
     *            An input stream to read file content. It should be valid.
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    public abstract void setContent(InputStream is) throws ScmException;

    /**
     * Loads file content from breakpoint file.
     *
     * @param breakpointFile
     *            An breakpoint file to transfer to file.
     * @throws ScmException
     *             If error happens
     * @since 3.0
     */
    public abstract void setContent(ScmBreakpointFile breakpointFile) throws ScmException;

    /**
     * Check out file
     *
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    @Deprecated
    public abstract void checkout() throws ScmException;

    /**
     * Check in file
     *
     * @param type
     *            file type
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    @Deprecated
    public abstract void checkin(ScmType type) throws ScmException;

    /**
     * Reverts the file version.
     *
     * @param preVersion
     *            Previous version.
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    @Deprecated
    public abstract void revert(int preVersion) throws ScmException;

    /**
     * Save file with it's content. It only be invoked by a new file instance.
     *
     * @return The value of the FileId property.
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    public abstract ScmId save() throws ScmException;

    /**
     * Save same file with it's content.
     *
     * @param config
     *            upload file config.
     * @return The value of the FileId property.
     * @throws ScmException
     *             If error happens.
     * @since 3.0
     */
    public abstract ScmId save(ScmUploadConf config) throws ScmException;

    /**
     * Deletes a file.
     *
     * @param isPhysical
     *            is physical or logical
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    public abstract void delete(boolean isPhysical) throws ScmException;

    /**
     * Deletes a file logically.
     *
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    @Deprecated
    public abstract void delete() throws ScmException;

    /**
     * Judges whether current file has been deleted.
     *
     * @return return is deleted
     * @since 2.1
     */
    public abstract boolean isDeleted();

    /**
     * Get the file's location list
     *
     * @return the location of file
     * @since 2.1
     */
    public abstract List<ScmFileLocation> getLocationList();

    /**
     * Get the file's dataId
     *
     * @return the file's dataId
     * @since 2.1
     */
    public abstract ScmId getDataId();

    /**
     * Get the file's data create time
     *
     * @return the file's data create time
     * @since 3.0
     */
    public abstract Date getDataCreateTime();

    /**
     * Obtains file content and saves into output path. only get content from
     * local site
     *
     * @param outputPath
     *            A system full path to storage file content. It should be not
     *            existed.
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    public abstract void getContentFromLocalSite(String outputPath) throws ScmException;

    /**
     * Obtains file content and saves into output stream. only get content from
     * local site
     *
     * @param os
     *            An output stream to storage file content. It should be valid.
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    public abstract void getContentFromLocalSite(OutputStream os) throws ScmException;

    /**
     * Create a new version scm file content from input stream.
     *
     * @param is
     *            An input stream to read file content. It should be valid.
     * @throws ScmException
     *             if error happens.
     */
    public abstract void updateContent(InputStream is) throws ScmException;

    /**
     * Create a new version scm file content from input stream.
     * @param is
     *              an input stream to read file content. It should be valid.
     * @param option
     *              option for update content.
     * @throws ScmException
     *            if error happens.
     */
    public abstract void updateContent(InputStream is, ScmUpdateContentOption option)
            throws ScmException;

    /**
     * Create a new version scm file content from local file path.
     *
     * @param path
     *            a system full path to read file content. It should have been
     *            existed.
     * @throws ScmException
     *             if error happens.
     */
    public abstract void updateContent(String path) throws ScmException;

    /**
     * Creates a new version scm file content from breakpoint file.
     *
     * @param breakpointFile
     *            breakpoint file to transfer to file.
     * @throws ScmException
     *             if error happens.
     */
    public abstract void updateContent(ScmBreakpointFile breakpointFile) throws ScmException;

    /**
     * Creates a new version scm file content from breakpoint file.
     * @param breakpointFile
     *          breakpoint file to transfer to file.
     * @param option
     *          option for update content.
     * @throws ScmException
     *          if error happens.
     */
    public abstract void updateContent(ScmBreakpointFile breakpointFile,
            ScmUpdateContentOption option) throws ScmException;

    /**
     * Sets file created time.
     *
     * @param date
     *            created time.
     * @throws ScmException
     *             if error happens.
     */
    public abstract void setCreateTime(Date date) throws ScmException;

    /**
     * Judges whether current file is exist.
     *
     * @return is exists
     *
     */
    abstract boolean isExist();

    /**
     * Sets or Updates the value of the Directory property.
     *
     * @param directoryId
     *            Directory id.
     * @throws ScmException
     *             If error happens
     */
    public abstract void setDirectory(String directoryId) throws ScmException;

    /**
     * Get the md5 of the file.
     * @return return null if the file has no md5.
     */
    public abstract String getMd5();

    /**
     * Calculate the md5 of the file. 
     */
    public abstract void calcMd5() throws ScmException;

    /**
     * Get the file's content location list
     *
     * @return the content locations of file
     * @since 3.1
     */
    public abstract List<ScmContentLocation> getContentLocations() throws ScmException;

    abstract void setSize(long size);

    abstract void setUpdateTime(Date date);

    abstract void setMajorVersion(int version);

    abstract void setMinorVersion(int version);

    abstract void setDataId(ScmId id);

    abstract void setLocationList(List<ScmFileLocation> list);

    abstract void setExist(boolean isExist);

    abstract void setUser(String user);

    abstract void setUpdateUser(String user);

    abstract void setFileId(ScmId fileId);

    abstract ScmWorkspace getWorkspace();

    abstract BSONObject toBSONObject() throws ScmException;

    abstract void refresh(BSONObject newFileBSON) throws ScmException;
}
