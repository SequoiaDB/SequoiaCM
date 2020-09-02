package com.sequoiacm.client.core;

import java.util.Date;

import org.bson.BSONObject;

import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;

/**
 * SCM Directory.
 */
public abstract class ScmDirectory {
    abstract ScmWorkspace getWorkspace() throws ScmException;

    /**
     * Get the directory name.
     *
     * @return name.
     * @throws ScmException
     *             if error happens.
     */
    public abstract String getName() throws ScmException;

    /**
     * Get the parent directory.
     *
     * @return parent directory.
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmDirectory getParentDirectory() throws ScmException;

    /**
     * Get the subdirectory.
     *
     * @param name
     *            subdirectory name.
     * @return subdirectory.
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmDirectory getSubdirectory(String name) throws ScmException;

    /**
     * Get the subfile.
     *
     * @param name
     *            subfile name.
     * @return subfile.
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmFile getSubfile(String name) throws ScmException;

    /**
     * Get the user.
     *
     * @return user name.
     * @throws ScmException
     *             if error happens.
     */
    public abstract String getUser() throws ScmException;

    /**
     * Get the create time.
     *
     * @return create time.
     * @throws ScmException
     *             if error happens.
     */
    public abstract Date getCreateTime() throws ScmException;

    /**
     * Get the update time.
     *
     * @return update time.
     * @throws ScmException
     *             if error happens.
     */
    public abstract Date getUpdateTime() throws ScmException;

    /**
     * Get the update user.
     *
     * @return update user.
     * @throws ScmException
     *             if error happens.
     */
    public abstract String getUpdateUser() throws ScmException;

    /**
     * Get the workspace name.
     *
     * @return workspace name.
     * @throws ScmException
     *             if error happens.
     */
    public abstract String getWorkspaceName() throws ScmException;

    /**
     * Rename the directory.
     *
     * @param newname
     *            new name.
     * @throws ScmException
     *             if error happens.
     */
    public abstract void rename(String newname) throws ScmException;

    /**
     * Move directory
     *
     * @param newParentDir
     *            new parent directory.
     * @throws ScmException
     *             if error happens.
     */
    public abstract void move(ScmDirectory newParentDir) throws ScmException;

    /**
     * List subfiles.
     *
     * @param condition
     *            query condition.
     * @return cursor
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmCursor<ScmFileBasicInfo> listFiles(BSONObject condition) throws ScmException;

    /**
     * List subfiles.
     *
     * @param condition
     *            query condition.
     * @param skip
     *            skip the the specified amount of files, never skip if this
     *            parameter is 0.
     * @param limit
     *            return the specified amount of files, when limit is -1, return
     *            all the files.
     * @param orderby
     *            the condition for sort, include: key is a property of
     *            {@link ScmAttributeName.File}, value is -1(descending) or
     *            1(ascending)
     * @return cursor
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmCursor<ScmFileBasicInfo> listFiles(BSONObject condition, int skip, int limit,
            BSONObject orderby) throws ScmException;

    /**
     * List subdirectories.
     *
     * @param condition
     *            query condition.
     * @return cursor.
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmCursor<ScmDirectory> listDirectories(BSONObject condition)
            throws ScmException;

    /**
     * Create subdirectory.
     *
     * @param name
     *            subdirectory name.
     * @return subdirectory.
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmDirectory createSubdirectory(String name) throws ScmException;

    /**
     * Get the path of the direcotry.
     *
     * @return path direcotry path.
     * @throws ScmException
     *             if error happens.
     */
    public abstract String getPath() throws ScmException;

    /**
     * Delete the directory.
     *
     * @throws ScmException
     *             if error happens.
     */
    public abstract void delete() throws ScmException;

    /**
     * Get the directory id.
     *
     * @return id.
     */
    public abstract String getId();

}
