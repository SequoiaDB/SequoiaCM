package com.sequoiacm.client.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType.InputStreamType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.dispatcher.BsonReader;
import com.sequoiacm.client.element.ScmClassBasicInfo;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmNodeInfo;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeMeta;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.client.util.BsonConverter;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.common.InvalidArgumentException;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.exception.ScmError;

/**
 * The class of ScmFactory
 *
 * @since 2.1
 */
public class ScmFactory {
    private ScmFactory() {

    }

    /**
     * Utility for operating file
     *
     * @since 2.1
     */
    public static class File {

        private File() {

        }

        /**
         * Constructs a new instance of the subclassable ScmFile class to be
         * persisted in the specified ScmWorkspace.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @return An object reference to a new instance of this class.
         * @throws ScmException
         *             If error happens.
         * @since 2.1
         */
        public static ScmFile createInstance(ScmWorkspace ws) throws ScmException {
            checkArgNotNull("workspace", ws);
            ScmFileImpl file = new ScmFileImpl();
            file.setWorkspace(ws);
            file.setExist(false);
            return file;
        }

        /**
         * Acquires an object of the ScmFile class by between the specified
         * fileId from the specified workspace and the latest version.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param fileId
         *            The ID(GUID) of the file instance.
         * @return An object of the ScmFile type.
         * @throws ScmException
         *             if error happens
         * @since 2.1
         */
        public static ScmFile getInstance(ScmWorkspace ws, ScmId fileId) throws ScmException {
            checkArgNotNull("fileId", fileId);
            return innerGetInstance(ws, fileId.get(), null, -1, -1);
        }

        /**
         * Acquires an object of the ScmFile class by between the specified
         * fileId from the specified workspace and the specified version.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param fileId
         *            The ID(GUID) of the file instance.
         * @param majorVersion
         *            The major version of the file.
         * @param minorVersion
         *            The minor version of the file.
         * @return An object of the ScmFile type.
         * @throws ScmException
         *             if error happens.
         * @since 2.1
         */
        public static ScmFile getInstance(ScmWorkspace ws, ScmId fileId, int majorVersion,
                int minorVersion) throws ScmException {
            checkArgNotNull("fileId", fileId);
            if (majorVersion < 0) {
                throw new ScmInvalidArgumentException(
                        "majorVersion must be a non-negative integer:" + majorVersion);
            }
            if (minorVersion < 0) {
                throw new ScmInvalidArgumentException(
                        "minorVersion must be a non-negative integer:" + minorVersion);
            }
            return innerGetInstance(ws, fileId.get(), null, majorVersion, minorVersion);
        }

        /**
         * Acquires an object of the ScmFile class by between the specified
         * workspace and the specified filePath.
         * 
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param filePath
         *            The path of the file.
         * @return An object of the ScmFile type.
         * @throws ScmException
         *             if error happens.
         * @since 3.0
         */
        public static ScmFile getInstanceByPath(ScmWorkspace ws, String filePath)
                throws ScmException {
            checkArgNotNull("filePath", filePath);
            return innerGetInstance(ws, null, filePath, -1, -1);
        }

        /**
         * Acquires an object of the ScmFile class by between the specified
         * filePath from the specified workspace and the specified version.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param filePath
         *            The path of the file.
         * @param majorVersion
         *            The major version of the file.
         * @param minorVersion
         *            The minor version of the file.
         * @return An object of the ScmFile type.
         * @throws ScmException
         *             if error happens.
         * @since 3.0
         */
        public static ScmFile getInstanceByPath(ScmWorkspace ws, String filePath, int majorVersion,
                int minorVersion) throws ScmException {
            checkArgNotNull("filePath", filePath);
            if (majorVersion < 0) {
                throw new ScmInvalidArgumentException(
                        "majorVersion must be a non-negative integer:" + majorVersion);
            }
            if (minorVersion < 0) {
                throw new ScmInvalidArgumentException(
                        "minorVersion must be a non-negative integer:" + minorVersion);
            }
            return innerGetInstance(ws, null, filePath, majorVersion, minorVersion);
        }

        private static ScmFile innerGetInstance(ScmWorkspace ws, String fileId, String filePath,
                int majorVersion, int minorVersion) throws ScmException {
            checkArgNotNull("workspace", ws);

            ScmFileImpl file;
            BSONObject fileInfo;
            ScmSession session;

            session = ws.getSession();

            fileInfo = session.getDispatcher().getFileInfo(ws.getName(), fileId, filePath,
                    majorVersion, minorVersion);

            if (null == fileInfo) {
                throw new ScmException(ScmError.FILE_NOT_FOUND,
                        "File is UnExisted:workspace=" + ws.getName() + ",fileId=" + fileId
                                + ",majorVersion=" + majorVersion + ",minorVersion="
                                + minorVersion);
            }
            // assembly
            file = ScmFileImpl.getInstanceByBSONObject(fileInfo);
            file.setWorkspace(ws);
            file.setExist(true);
            return file;
        }

        /**
         * delete a file.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param fileId
         *            The ID(GUID) of the file instance.
         * @param majorVersion
         *            The major version of the file.
         * @param minorVersion
         *            The minor version of the file.
         * @param isPhysical
         *            is physical or logical
         * @throws ScmException
         *             if error happens
         * @since 2.1
         */
        private static void deleteInstance(ScmWorkspace ws, ScmId fileId, int majorVersion,
                int minorVersion, boolean isPhysical) throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("fileId", fileId);
            ScmSession session = ws.getSession();
            session.getDispatcher().deleteFile(ws.getName(), fileId.get(), majorVersion,
                    minorVersion, isPhysical);

        }

        /**
         * delete a file.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param fileId
         *            The ID(GUID) of the file instance.
         * @param isPhysical
         *            is physical or logical
         * @throws ScmException
         *             if error happens
         * @since 2.1
         */
        public static void deleteInstance(ScmWorkspace ws, ScmId fileId, boolean isPhysical)
                throws ScmException {
            deleteInstance(ws, fileId, -1, -1, isPhysical);
        }

        /**
         * Acquires ScmFileBasicInfo instance set which matches between the
         * specified workspace, scope type and query condition.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param scope
         *            The scope of file version.
         * @param fileCondition
         *            The condition of query files.
         * @return A cursor to traverse
         * @throws ScmException
         *             if error happens
         * @since 2.1
         */
        public static ScmCursor<ScmFileBasicInfo> listInstance(ScmWorkspace ws, ScopeType scope,
                BSONObject fileCondition) throws ScmException {
            return listInstance(ws, scope, fileCondition, null, 0, -1);
        }

        /**
         * Acquires ScmFileBasicInfo instance set which matches between the
         * specified workspace, scope type and query condition.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param scope
         *            The scope of file version.
         * @param fileCondition
         *            The condition of query files.
         * @param orderby
         *            the condition for sort, include: key is a property of
         *            {@link ScmAttributeName.File}, value is -1(descending) or
         *            1(ascending)
         * @param skip
         *            skip the the specified amount of files, never skip if this
         *            parameter is 0.
         * @param limit
         *            return the specified amount of files, when limit is -1,
         *            return all the files.
         * @return A cursor to traverse
         * @throws ScmException
         *             if error happens
         * @since 3.1
         */
        public static ScmCursor<ScmFileBasicInfo> listInstance(ScmWorkspace ws, ScopeType scope,
                BSONObject fileCondition, BSONObject orderby, long skip, long limit)
                throws ScmException {
            checkArgNotNull("workspace", ws);
            if (null == scope) {
                throw new ScmInvalidArgumentException("scope is null");
            }
            if (null == fileCondition) {
                throw new ScmInvalidArgumentException("fileCondition is null");
            }
            if (skip < 0) {
                throw new ScmInvalidArgumentException(
                        "skip must be greater than or equals to 0:skip=" + skip);
            }
            if (limit < -1) {
                throw new ScmInvalidArgumentException(
                        "limit must be greater than or equals to -1:limit=" + limit);
            }
            if (scope != ScopeType.SCOPE_CURRENT) {
                try {
                    ScmArgChecker.File.checkHistoryFileMatcher(fileCondition);
                }
                catch (InvalidArgumentException e) {
                    throw new ScmInvalidArgumentException("invlid condition", e);
                }
            }
            BsonReader reader = ws.getSession().getDispatcher().getFileList(ws.getName(),
                    scope.getScope(), fileCondition, orderby, skip, limit);
            ScmCursor<ScmFileBasicInfo> fbiCursor = new ScmBsonCursor<ScmFileBasicInfo>(reader,
                    new BsonConverter<ScmFileBasicInfo>() {
                        @Override
                        public ScmFileBasicInfo convert(BSONObject obj) throws ScmException {
                            return new ScmFileBasicInfo(obj);
                        }
                    });
            return fbiCursor;
        }

        /**
         * Acquires instance's count which matches between the specified
         * workspace, scope type and query condition.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param scope
         *            The scope of file version.
         * @param fileCondition
         *            The condition of query files.
         * @return count of instance
         * @throws ScmException
         *             if error happens
         * @since 2.1
         */
        public static long countInstance(ScmWorkspace ws, ScopeType scope, BSONObject fileCondition)
                throws ScmException {
            checkArgNotNull("workspace", ws);

            if (null == scope) {
                throw new ScmInvalidArgumentException("scope is null");
            }

            if (null == fileCondition) {
                throw new ScmInvalidArgumentException("fileCondition is null");
            }

            if (scope != ScopeType.SCOPE_CURRENT) {
                try {
                    ScmArgChecker.File.checkHistoryFileMatcher(fileCondition);
                }
                catch (InvalidArgumentException e) {
                    throw new ScmInvalidArgumentException("invlid condition", e);
                }
            }

            ScmSession conn = ws.getSession();
            return conn.getDispatcher().countFile(ws.getName(), scope.getScope(), fileCondition);

        }

        /**
         * asynchronous transfer file.
         *
         * @param ws
         *            workspace.
         * @param fileId
         *            file id.
         * @throws ScmException
         *             if error happens.
         * @since 2.1
         */
        public static void asyncTransfer(ScmWorkspace ws, ScmId fileId) throws ScmException {
            _asyncTransfer(ws, fileId, -1, -1);
        }

        /**
         * asynchronous transfer file.
         *
         * @param ws
         *            workspace.
         * @param fileId
         *            file id.
         * @param majorVersion
         *            file major version.
         * @param minorVersion
         *            file minor version.
         * @throws ScmException
         *             if error happens.
         * @since 2.1
         */
        public static void asyncTransfer(ScmWorkspace ws, ScmId fileId, int majorVersion,
                int minorVersion) throws ScmException {
            if (majorVersion < 0) {
                throw new ScmInvalidArgumentException(
                        "majorVersion must be a non-negative integer:" + majorVersion);
            }
            if (minorVersion < 0) {
                throw new ScmInvalidArgumentException(
                        "minorVersion must be a non-negative integer:" + minorVersion);
            }
            _asyncTransfer(ws, fileId, majorVersion, minorVersion);
        }

        private static void _asyncTransfer(ScmWorkspace ws, ScmId fileId, int majorVersion,
                int minorVersion) throws ScmException {
            checkArgNotNull("workspace", ws);

            if (null == fileId) {
                throw new ScmInvalidArgumentException("fileId is null");
            }

            ScmSession conn = ws.getSession();
            conn.getDispatcher().asyncTransferFile(ws.getName(), fileId, majorVersion,
                    minorVersion);
        }

        /**
         * asynchronous cache file.
         *
         * @param ws
         *            workspace.
         * @param fileId
         *            file id.
         * @throws ScmException
         *             if error happens
         * @since 2.1
         */
        public static void asyncCache(ScmWorkspace ws, ScmId fileId) throws ScmException {
            _asyncCache(ws, fileId, -1, -1);
        }

        /**
         * asynchronous cache file.
         *
         * @param ws
         *            workspace.
         * @param fileId
         *            file id.
         * @param majorVersion
         *            file major version.
         * @param minorVersion
         *            file minor version.
         * @throws ScmException
         *             if error happens
         * @since 2.1
         */
        public static void asyncCache(ScmWorkspace ws, ScmId fileId, int majorVersion,
                int minorVersion) throws ScmException {
            if (majorVersion < 0) {
                throw new ScmInvalidArgumentException(
                        "majorVersion must be a non-negative integer:" + majorVersion);
            }
            if (minorVersion < 0) {
                throw new ScmInvalidArgumentException(
                        "minorVersion must be a non-negative integer:" + minorVersion);
            }
            _asyncCache(ws, fileId, majorVersion, minorVersion);
        }

        private static void _asyncCache(ScmWorkspace ws, ScmId fileId, int majorVersion,
                int minorVersion) throws ScmException {
            checkArgNotNull("workspace", ws);

            if (null == fileId) {
                throw new ScmInvalidArgumentException("fileId is null");
            }

            ScmSession conn = ws.getSession();
            conn.getDispatcher().asyncCacheFile(ws.getName(), fileId, majorVersion, minorVersion);
        }

        /**
         * create a unseekable instance of ScmInputStream.
         *
         * @param scmFile
         *            the file to be opened for reading
         * @return ScmInputStream.
         * @throws ScmException
         *             if error happens
         * @since 2.1
         *
         */
        public static ScmInputStream createInputStream(ScmFile scmFile) throws ScmException {
            return createInputStream(InputStreamType.UNSEEKABLE, scmFile);
        }

        /**
         * create a instance of the subclassable ScmInputStream class.
         *
         * @param type
         *            the type of inputstream
         * @param scmFile
         *            the file to be opened for reading
         * @return ScmInputStream.
         * @throws ScmException
         *             if error happens
         * @see InputStreamType
         * @since 2.1
         *
         */
        public static ScmInputStream createInputStream(InputStreamType type, ScmFile scmFile)
                throws ScmException {
            if (type == null) {
                throw new ScmException(ScmError.INVALID_ARGUMENT, "type is null");
            }
            if (InputStreamType.SEEKABLE == type) {
                return new ScmInputStreamImplSeekable(scmFile);
            }
            else if (InputStreamType.UNSEEKABLE == type) {
                return new ScmInputStreamImplUnseekable(scmFile);
            }
            throw new ScmException(ScmError.INVALID_ARGUMENT, "unknown type:" + type);
        }

        /**
         * Create a new instance of the subclassable ScmOutputStream class.
         *
         * @param scmFile
         *            the file to be opened for writing
         * @return ScmOutputStream
         * @throws ScmException
         *             If error happens.
         */
        public static ScmOutputStream createOutputStream(ScmFile scmFile) throws ScmException {
            if (scmFile == null) {
                throw new ScmInvalidArgumentException("scmFile is null");
            }

            if (scmFile.isExist()) {
                throw new ScmException(ScmError.OPERATION_UNSUPPORTED,
                        "file already exists, can not write data to this file");
            }
            return new ScmOutputStreamImpl(scmFile);
        }

        /**
         * Create a new instance of the subclassable ScmOutputStream class.
         *
         * @param scmFile
         *            the file to be opened for update
         * @return ScmOutputStream
         * @throws ScmException
         *             If error happens.
         */
        public static ScmOutputStream createUpdateOutputStream(ScmFile scmFile)
                throws ScmException {
            if (scmFile == null) {
                throw new ScmInvalidArgumentException("scmFile is null");
            }

            if (!scmFile.isExist()) {
                throw new ScmException(ScmError.OPERATION_UNSUPPORTED, "file is not exist");
            }
            return new ScmUpdateFileContentOsImpl(scmFile);
        }
    }

    /**
     * Utility for operating directory.
     */
    public static class Directory {
        private Directory() {

        }

        /**
         * Create a directory with specified path.
         *
         * @param ws
         *            workspace.
         * @param path
         *            directory path.
         * @return instance of directory.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmDirectory createInstance(ScmWorkspace ws, String path)
                throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("path", path);
            if (!path.startsWith("/")) {
                throw new ScmInvalidArgumentException("invalid path:path=" + path);
            }
            BSONObject dir = ws.getSession().getDispatcher().createDir(ws.getName(), null, null,
                    path);
            return new ScmDirectoryImpl(ws, dir);
        }

        /**
         * Get directory with specified path.
         *
         * @param ws
         *            workspace.
         * @param path
         *            directory path.
         * @return the directory.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmDirectory getInstance(ScmWorkspace ws, String path) throws ScmException {
            checkArgNotNull("path", path);
            if (!path.startsWith("/")) {
                throw new ScmInvalidArgumentException("invalid path:path=" + path);
            }
            return getInstance(ws, null, path);
        }

        /**
         * Judges whether the directory is exists.
         *
         * @param ws
         *            workspace.
         * @param path
         *            directory path.
         * @return return is exists.
         * @throws ScmException
         *             if error happens.
         */
        public static boolean isInstanceExist(ScmWorkspace ws, String path) throws ScmException {
            try {
                getInstance(ws, path);
                return true;
            }
            catch (ScmException e) {
                if (e.getError() == ScmError.DIR_NOT_FOUND) {
                    return false;
                }
                throw e;
            }
        }

        static ScmDirectory getInstance(ScmWorkspace ws, String id, String path)
                throws ScmException {
            checkArgNotNull("workspace", ws);
            BSONObject dir = ws.getSession().getDispatcher().getDir(ws.getName(), id, path);
            return new ScmDirectoryImpl(ws, dir);
        }

        /**
         * Delete specified directory.
         *
         * @param ws
         *            workspace.
         * @param path
         *            directory path.
         * @throws ScmException
         *             if error happens.
         */
        public static void deleteInstance(ScmWorkspace ws, String path) throws ScmException {
            checkArgNotNull("workspace", ws);
            if (path == null) {
                throw new ScmInvalidArgumentException("invalid arg:path=null");
            }
            ws.getSession().getDispatcher().deleteDir(ws.getName(), null, path);
        }

        /**
         * List directory with specified condition.
         *
         * @param ws
         *            workspace.
         * @param condition
         *            filter.
         * @return cursor.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmCursor<ScmDirectory> listInstance(final ScmWorkspace ws,
                BSONObject condition) throws ScmException {
            checkArgNotNull("workspace", ws);
            if (null == condition) {
                condition = new BasicBSONObject();
            }
            BsonReader reader = ws.getSession().getDispatcher().getDirList(ws.getName(), condition);
            ScmCursor<ScmDirectory> c = new ScmBsonCursor<ScmDirectory>(reader,
                    new BsonConverter<ScmDirectory>() {
                        @Override
                        public ScmDirectory convert(BSONObject obj) throws ScmException {
                            return new ScmDirectoryImpl(ws, obj);
                        }
                    });
            return c;
        }

        /**
         * Acquires ScmDirectory instance's count which matches between the
         * specified workspace and query condition.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param condition
         *            The condition of query directory.
         * @return count of instance
         * @throws ScmException
         *             if error happens
         * @since 3.1
         */
        public static long countInstance(ScmWorkspace ws, BSONObject condition)
                throws ScmException {
            checkArgNotNull("workspace", ws);

            if (null == condition) {
                throw new ScmInvalidArgumentException("condition is null");
            }

            ScmSession conn = ws.getSession();
            return conn.getDispatcher().countDir(ws.getName(), condition);
        }
    }

    /**
     * Utility for operating batch.
     */
    public static class Batch {
        private Batch() {

        }

        /**
         * Constructs a new instance of the subclassable ScmBatch class to be
         * persisted in the specified ScmWorkspace.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @return An object reference to a new instance of this class.
         * @throws ScmException
         *             If error happens.
         */
        public static ScmBatch createInstance(ScmWorkspace ws) throws ScmException {
            checkArgNotNull("workspace", ws);
            ScmBatchImpl batch = new ScmBatchImpl();
            batch.setWorkspace(ws);
            return batch;
        }

        /**
         * Acquires an object of the ScmBatch class by between the specified
         * batchId from the specified workspace.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param id
         *            The ID(GUID) of the batch instance.
         * @return An object of the ScmBatch type.
         * @throws ScmException
         *             if error happens
         */
        public static ScmBatch getInstance(ScmWorkspace ws, ScmId id) throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("batchId", id);
            ScmSession session = ws.getSession();
            BSONObject batchInfo = session.getDispatcher().getBatchInfo(ws.getName(), id.get());
            if (null == batchInfo) {
                throw new ScmException(ScmError.BATCH_NOT_FOUND,
                        "Batch is UnExisted:workspace=" + ws.getName() + ",batchId=" + id.get());
            }

            ScmBatchImpl batch = ScmBatchImpl.getInstance(batchInfo, ws);
            return batch;
        }

        /**
         * Acquires ScmBatchInfo instance set which matches between the
         * specified workspace and query condition.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param condition
         *            The condition of query batches.
         * @return A cursor to traverse
         * @throws ScmException
         *             if error happens
         */
        public static ScmCursor<ScmBatchInfo> listInstance(ScmWorkspace ws, BSONObject condition)
                throws ScmException {
            return listInstance(ws, condition, null, 0, -1);
        }

        /**
         * Acquires ScmBatchInfo instance set which matches between the
         * specified workspace and query condition.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param condition
         *            The condition of query batches.
         * @param orderBy
         *            the condition for sort, include: key is a property of
         *            {@link ScmAttributeName.Batch}, value is -1(descending) or
         *            1(ascending)
         * @param skip
         *            skip to the first number record
         * @param limit
         *            return the total records of query, when value is -1,
         *            return all records
         * @return A cursor to traverse
         * @throws ScmException
         *             if error happens
         * @since 3.1
         */
        public static ScmCursor<ScmBatchInfo> listInstance(ScmWorkspace ws, BSONObject condition,
                BSONObject orderBy, long skip, long limit) throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("condition", condition);
            checkSkip(skip);
            checkLimit(limit);
            BsonReader reader = ws.getSession().getDispatcher().getBatchList(ws.getName(),
                    condition, orderBy, skip, limit);
            ScmCursor<ScmBatchInfo> batchCursor = new ScmBsonCursor<ScmBatchInfo>(reader,
                    new BsonConverter<ScmBatchInfo>() {
                        @Override
                        public ScmBatchInfo convert(BSONObject obj) throws ScmException {
                            return new ScmBatchInfo(obj);
                        }
                    });
            return batchCursor;
        }

        /**
         * Delete a batch.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param id
         *            The ID(GUID) of the batch instance.
         * @throws ScmException
         *             if error happens
         */
        public static void deleteInstance(ScmWorkspace ws, ScmId id) throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("batchId", id);
            ws.getSession().getDispatcher().deleteBatch(ws.getName(), id.get());
        }

        /**
         * Acquires ScmBatchInfo instance's count which matches between the
         * specified workspace and query condition.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param condition
         *            The condition of query batch.
         * @return count of instance
         * @throws ScmException
         *             if error happens
         * @since 3.1
         */
        public static long countInstance(ScmWorkspace ws, BSONObject condition)
                throws ScmException {
            checkArgNotNull("workspace", ws);

            if (null == condition) {
                throw new ScmInvalidArgumentException("condition is null");
            }

            ScmSession conn = ws.getSession();
            return conn.getDispatcher().countBatch(ws.getName(), condition);
        }
    }

    /**
     * Utility for operating Audit.
     */
    public static class Audit {
        private Audit() {

        }

        /**
         * Acquires ScmAuditInfo instance set which matches query condition.
         *
         * @param session
         *            session
         * @param condition
         *            The condition of query ScmAuditInfo.
         * @return A cursor to traverse
         * @throws ScmException
         *             if error happens
         */
        public static ScmCursor<ScmAuditInfo> listInstance(ScmSession session, BSONObject condition)
                throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("condition", condition);

            BsonReader reader = session.getDispatcher().getAuditList(condition);
            ScmCursor<ScmAuditInfo> auditCursor = new ScmBsonCursor<ScmAuditInfo>(reader,
                    new BsonConverter<ScmAuditInfo>() {
                        @Override
                        public ScmAuditInfo convert(BSONObject obj) throws ScmException {
                            return new ScmAuditInfo(obj);
                        }
                    });
            return auditCursor;
        }

    }

    /**
     * Utility for operating breakpoint file
     */
    public static class BreakpointFile {
        private BreakpointFile() {
        }

        /**
         * Create a new breakpoint file.
         *
         * @param workspace
         *            the workspace where the breakpoint file will be created
         * @param fileName
         *            the breakpoint file name
         * @param checksumType
         *            the checksum type of the file
         * @return the breakpoint file
         * @throws ScmException
         *             if error happens
         */
        public static ScmBreakpointFile createInstance(ScmWorkspace workspace, String fileName,
                ScmChecksumType checksumType) throws ScmException {
            checkArgNotNull("workspace", workspace);
            checkStringArgNotEmpty("fileName", fileName);
            checkArgNotNull("checksumType", checksumType);
            return new ScmBreakpointFileImpl(workspace, fileName, checksumType);
        }

        /**
         * Create a new breakpoint file.
         *
         * @param workspace
         *            the workspace where the breakpoint file will be created
         * @param fileName
         *            the breakpoint file name
         * @param checksumType
         *            the checksum type of the file
         * @param breakpointSize
         *            the upload breakpoint size
         * @return the breakpoint file
         * @throws ScmException
         *             if error happens
         */
        public static ScmBreakpointFile createInstance(ScmWorkspace workspace, String fileName,
                ScmChecksumType checksumType, int breakpointSize) throws ScmException {
            checkArgNotNull("workspace", workspace);
            checkStringArgNotEmpty("fileName", fileName);
            checkArgNotNull("checksumType", checksumType);
            return new ScmBreakpointFileImpl(workspace, fileName, checksumType, breakpointSize);
        }

        /**
         * Create a new breakpoint file without checksum.
         *
         * @param workspace
         *            the workspace where the breakpoint file will be created
         * @param fileName
         *            the breakpoint file name
         * @return the breakpoint file
         * @throws ScmException
         *             if error happens
         */
        public static ScmBreakpointFile createInstance(ScmWorkspace workspace, String fileName)
                throws ScmException {
            return createInstance(workspace, fileName, ScmChecksumType.NONE);
        }

        /**
         * Get a breakpoint file.
         *
         * @param workspace
         *            the workspace where the breakpoint file is in
         * @param fileName
         *            the breakpoint file name
         * @return the breakpoint file
         * @throws ScmException
         *             if error happens
         */
        public static ScmBreakpointFile getInstance(ScmWorkspace workspace, String fileName)
                throws ScmException {
            checkArgNotNull("workspace", workspace);
            checkStringArgNotEmpty("fileName", fileName);

            BSONObject obj = workspace.getSession().getDispatcher()
                    .getBreakpointFile(workspace.getName(), fileName);
            return new ScmBreakpointFileImpl(workspace, obj);
        }

        /**
         * Get a breakpoint file.
         *
         * @param workspace
         *            the workspace where the breakpoint file is in
         * @param fileName
         *            the breakpoint file name
         * @param breakpointSize
         *            the upload breakpoint size
         * @return the breakpoint file
         * @throws ScmException
         *             if error happens
         */
        public static ScmBreakpointFile getInstance(ScmWorkspace workspace, String fileName,
                int breakpointSize) throws ScmException {
            checkArgNotNull("workspace", workspace);
            checkStringArgNotEmpty("fileName", fileName);

            BSONObject obj = workspace.getSession().getDispatcher()
                    .getBreakpointFile(workspace.getName(), fileName);
            return new ScmBreakpointFileImpl(workspace, obj, breakpointSize);
        }

        /**
         * List all the breakpoint files.
         *
         * @param workspace
         *            the workspace where the breakpoint files are in
         * @param filter
         *            the query filter
         * @return the breakpoint file cursor
         * @throws ScmException
         *             if error happens
         */
        public static ScmCursor<ScmBreakpointFile> listInstance(final ScmWorkspace workspace,
                BSONObject filter) throws ScmException {
            checkArgNotNull("workspace", workspace);

            BsonReader reader = workspace.getSession().getDispatcher()
                    .listBreakpointFiles(workspace.getName(), filter);
            ScmCursor<ScmBreakpointFile> cursor = new ScmBsonCursor<ScmBreakpointFile>(reader,
                    new BsonConverter<ScmBreakpointFile>() {
                        @Override
                        public ScmBreakpointFile convert(BSONObject obj) throws ScmException {
                            return new ScmBreakpointFileImpl(workspace, obj);
                        }
                    });
            return cursor;
        }

        /**
         * List all the breakpoint files.
         *
         * @param workspace
         *            the workspace where the breakpoint files are in
         * @return the breakpoint file cursor
         * @throws ScmException
         *             if error happens
         */
        public static ScmCursor<ScmBreakpointFile> listInstance(ScmWorkspace workspace)
                throws ScmException {
            return listInstance(workspace, null);
        }

        /**
         * Delete specified breakpoint file.
         *
         * @param workspace
         *            the workspace where the breakpoint file is in
         * @param fileName
         *            the breakpoint file name
         * @throws ScmException
         *             if error happens
         */
        public static void deleteInstance(ScmWorkspace workspace, String fileName)
                throws ScmException {
            checkArgNotNull("workspace", workspace);
            checkStringArgNotEmpty("fileName", fileName);
            workspace.getSession().getDispatcher().deleteBreakpointFile(workspace.getName(),
                    fileName);
        }
    }

    /**
     * Utility for session
     *
     * @since 2.1
     */
    public static class Session {
        private static Random random = new Random();

        private Session() {
        }

        private static final Logger logger = LoggerFactory.getLogger(Session.class);

        private static void checkConfigOption(ScmConfigOption option) throws ScmException {
            if (null == option) {
                throw new ScmInvalidArgumentException("option is null");
            }

            ScmUrlConfig urlConfig = option.getUrlConfig();
            if (null == urlConfig) {
                throw new ScmInvalidArgumentException("urlConfig is null");
            }

            List<String> urls = urlConfig.getUrl();
            if (null == urls || urls.isEmpty()) {
                throw new ScmInvalidArgumentException("urls is null or empty");
            }
        }

        /**
         * Acquires an object of the ScmSession class by specified arguments
         *
         * @param option
         *            The option contains url, username, password
         * @return the newly created session object
         * @throws ScmException
         *             if error happens
         * @since 2.1
         */
        public static ScmSession createSession(ScmConfigOption option) throws ScmException {
            checkConfigOption(option);
            return priorityAccess(SessionType.AUTH_SESSION, option.getUrlConfig(), option);

        }

        public static ScmSession createSession(SessionType sessionType, ScmConfigOption option)
                throws ScmException {
            checkConfigOption(option);
            return priorityAccess(sessionType, option.getUrlConfig(), option);
        }

        private static ScmSession priorityAccess(SessionType type, ScmUrlConfig urlConfig,
                ScmConfigOption config) throws ScmException {
            String region = config.getRegion();
            String zone = config.getZone();
            ScmException lastException = null;

            // first, check urls in the same region and zone
            List<String> urls = urlConfig.getUrl(region, zone);
            if (null != urls && !urls.isEmpty()) {
                try {
                    return randomAccess(type, urls, config);
                }
                catch (ScmException e) {
                    if (e.getError() == ScmError.INVALID_ARGUMENT
                            || e.getError() == ScmError.HTTP_UNAUTHORIZED) {
                        throw e;
                    }

                    lastException = e;
                    logger.warn("check urls failed:urls={},region={},zone={}", urls, region, zone,
                            e);
                }
            }

            // second, check urls in the same region and different the zone
            urls = urlConfig.getUrlsIncludeRegionExcludeZone(region, zone);
            if (null != urls && !urls.isEmpty()) {
                try {
                    return randomAccess(type, urls, config);
                }
                catch (ScmException e) {
                    if (e.getError() == ScmError.INVALID_ARGUMENT
                            || e.getError() == ScmError.HTTP_UNAUTHORIZED) {
                        throw e;
                    }

                    lastException = e;
                    logger.warn("check urls failed:urls={},region={},exclude zone={}", urls, region,
                            zone, e);
                }
            }

            // third, check urls in the different region
            urls = urlConfig.getUrlExclude(region);
            if (null != urls && !urls.isEmpty()) {
                try {
                    return randomAccess(type, urls, config);
                }
                catch (ScmException e) {
                    if (e.getError() == ScmError.INVALID_ARGUMENT
                            || e.getError() == ScmError.HTTP_UNAUTHORIZED) {
                        throw e;
                    }

                    lastException = e;
                    logger.warn("check urls failed:urls={},exclude region={}", urls, region, e);
                }
            }

            if (null != lastException) {
                throw lastException;
            }

            throw new ScmSystemException("check all urls failed");
        }

        private static ScmSession randomAccess(SessionType type, List<String> urls,
                ScmConfigOption config) throws ScmException {
            List<String> urlsTmp = urls;
            boolean isNeedReplace = true;
            ScmException lastException = null;
            while (urlsTmp.size() > 0) {
                int index = random.nextInt(urlsTmp.size());
                try {
                    if (type == SessionType.AUTH_SESSION) {
                        return new ScmSessionAuth(urlsTmp.get(index), config.getUser(),
                                config.getPasswd(), config.getRequestConfig());
                    }
                    else {
                        return new ScmSessionNotAuth(urlsTmp.get(index), config.getRequestConfig());
                    }
                }
                catch (ScmException e) {
                    if (e.getError() == ScmError.INVALID_ARGUMENT
                            || e.getError() == ScmError.HTTP_UNAUTHORIZED) {
                        throw e;
                    }
                    lastException = e;
                    logger.warn("connecting to server failed:server={}", urlsTmp.get(index), e);
                    if (isNeedReplace) {
                        isNeedReplace = false;
                        urlsTmp = new ArrayList<String>(urls);
                    }
                    urlsTmp.remove(index);
                }
            }
            if (lastException != null) {
                throw lastException;
            }
            else {
                // should not come to here
                throw new ScmSystemException("failed to connect:url=" + urls);
            }
        }

        /**
         * Get session information of the specified session id.
         *
         * @param session
         *            session
         * @param sessionId
         *            the session id which to be queried
         * @return ScmSessionInfo
         * @throws ScmException
         *             If error happens.
         */
        public static ScmSessionInfo getSessionInfo(ScmSession session, String sessionId)
                throws ScmException {
            checkArgNotNull("session", session);
            checkStringArgNotEmpty("sessionId", sessionId);

            BSONObject obj = session.getDispatcher().getSessionInfo(sessionId);
            return new ScmSessionInfoImpl(obj);
        }

        /**
         * List all the sessions' information created by specified user.
         *
         * @param session
         *            session
         * @param username
         *            the specified user
         * @return ScmCursor
         * @throws ScmException
         *             If error happens.
         */
        public static ScmCursor<ScmSessionInfo> listSessions(ScmSession session, String username)
                throws ScmException {
            checkArgNotNull("session", session);
            checkStringArgNotEmpty("username", username);

            BsonReader reader = session.getDispatcher().listSessions(username);
            ScmCursor<ScmSessionInfo> cursor = new ScmBsonCursor<ScmSessionInfo>(reader,
                    new BsonConverter<ScmSessionInfo>() {
                        @Override
                        public ScmSessionInfo convert(BSONObject obj) throws ScmException {
                            return new ScmSessionInfoImpl(obj);
                        }
                    });
            return cursor;
        }

        /**
         * List all the sessions' information.
         *
         * @param session
         *            session
         * @return ScmCursor
         * @throws ScmException
         *             If error happens.
         */
        public static ScmCursor<ScmSessionInfo> listSessions(ScmSession session)
                throws ScmException {
            checkArgNotNull("session", session);

            BsonReader reader = session.getDispatcher().listSessions(null);
            ScmCursor<ScmSessionInfo> cursor = new ScmBsonCursor<ScmSessionInfo>(reader,
                    new BsonConverter<ScmSessionInfo>() {
                        @Override
                        public ScmSessionInfo convert(BSONObject obj) throws ScmException {
                            return new ScmSessionInfoImpl(obj);
                        }
                    });
            return cursor;
        }

        /**
         * Delete the specified session. Note: Only the authentication
         * administrator can delete session.
         *
         * @param session
         *            session
         * @param sessionId
         *            the session id which to be deleted
         * @throws ScmException
         *             If error happens.
         */
        public static void deleteSession(ScmSession session, String sessionId) throws ScmException {
            checkArgNotNull("session", session);
            checkStringArgNotEmpty("sessionId", sessionId);

            session.getDispatcher().deleteSession(sessionId);
        }

        /**
         * Acquires the count of the ScmSession instance
         *
         * @param session
         *            the session
         * @return the count of session instance
         * @throws ScmException
         *             if error happens
         * @since 3.1
         */
        public static long countSessions(ScmSession session) throws ScmException {
            checkArgNotNull("session", session);
            long countSessions = session.getDispatcher().countSessions();
            return countSessions;
        }

        public static ScmSessionMgr createSessionMgr(ScmConfigOption config,
                long syncGatewayAddrInterval) throws ScmException {
            checkConfigOption(config);
            return new ScmSessionMgrImpl(config, syncGatewayAddrInterval);
        }
    }

    /**
     * Utility for operating privilege
     *
     * @since 3.0
     */
    public static class Privilege {
        private Privilege() {
        }

        /**
         * Get privilege's metadata
         *
         * @param session
         *            the session
         * @return ScmPrivilegeMeta
         * @throws ScmException
         *             If error happens
         */
        public static ScmPrivilegeMeta getMeta(ScmSession session) throws ScmException {
            checkArgNotNull("session", session);
            BSONObject obj = session.getDispatcher().getPrivilegeMeta();
            return new ScmPrivilegeMeta(obj);
        }

        /**
         * Get privilege with specified id.
         *
         * @param session
         *            the session
         * @param privilegeId
         *            specified privilege id
         * @return ScmPrivilege
         * @throws ScmException
         *             If error happens
         */
        public static ScmPrivilege getPrivilegeById(ScmSession session, String privilegeId)
                throws ScmException {
            checkArgNotNull("session", session);
            checkStringArgNotEmpty("privilegeId", privilegeId);

            BSONObject obj = session.getDispatcher().getPrivilegeById(privilegeId);
            return new ScmPrivilegeImpl(session, obj);
        }

        /**
         * list privilege with specified role.
         *
         * @param session
         *            the session
         * @param role
         *            specified ScmRole
         * @return ScmCursor
         * @throws ScmException
         *             If error happens
         */
        public static ScmCursor<ScmPrivilege> listPrivileges(ScmSession session, ScmRole role)
                throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("role", role);

            BsonReader reader = session.getDispatcher().listPrivilegesByRoleId(role.getRoleId());
            ScmCursor<ScmPrivilege> cursor = new ScmBsonCursor<ScmPrivilege>(reader,
                    new PrivilegeBSONConverter<ScmPrivilege>(session));
            return cursor;
        }

        /**
         * list privilege with specified resource.
         *
         * @param session
         *            the session
         * @param resource
         *            specified resource
         * @return ScmCursor
         * @throws ScmException
         *             If error happens
         */
        public static ScmCursor<ScmPrivilege> listPrivilegesByResource(ScmSession session,
                ScmResource resource) throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("resource", resource);

            BsonReader reader = session.getDispatcher().listPrivilegesByResource(resource.getType(),
                    resource.toStringFormat());
            ScmCursor<ScmPrivilege> cursor = new ScmBsonCursor<ScmPrivilege>(reader,
                    new PrivilegeBSONConverter<ScmPrivilege>(session));
            return cursor;
        }
    }

    /**
     * Utility for operating resource
     *
     * @since 3.0
     */
    public static class Resource {
        private Resource() {
        }

        /**
         * Get resource with specified id.
         *
         * @param session
         *            the session
         * @param resourceId
         *            specified resource id
         * @return ScmResource
         * @throws ScmException
         *             If error happens
         */
        public static ScmResource getResourceById(ScmSession session, String resourceId)
                throws ScmException {
            checkArgNotNull("session", session);
            checkStringArgNotEmpty("resourceId", resourceId);

            BSONObject obj = session.getDispatcher().getResourceById(resourceId);
            return ScmResourceFactory.createResource(obj);
        }

        /**
         * list resource with specified workspaceName.
         *
         * @param session
         *            the session
         * @param workspaceName
         *            specified workspaceName
         * @return ScmCursor
         * @throws ScmException
         *             If error happens
         */
        public static ScmCursor<ScmResource> listResourceByWorkspace(ScmSession session,
                String workspaceName) throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("workspaceName", workspaceName);

            BsonReader reader = session.getDispatcher().listResourceByWorkspace(workspaceName);
            ScmCursor<ScmResource> cursor = new ScmBsonCursor<ScmResource>(reader,
                    new BsonConverter<ScmResource>() {
                        @Override
                        public ScmResource convert(BSONObject obj) throws ScmException {
                            return ScmResourceFactory.createResource(obj);
                        }
                    });
            return cursor;
        }
    }

    /**
     * Utility for operating role
     *
     * @since 3.0
     */
    public static class Role {
        private Role() {
        }

        /**
         * Create a role with specified name.
         *
         * @param session
         *            the session
         * @param roleName
         *            role name
         * @param description
         *            description of the role
         * @return the created ScmRole
         * @throws ScmException
         *             If error happens
         */
        public static ScmRole createRole(ScmSession session, String roleName, String description)
                throws ScmException {
            checkArgNotNull("session", session);
            checkStringArgNotEmpty("roleName", roleName);

            BSONObject obj = session.getDispatcher().createRole(roleName, description);
            return new ScmRoleImpl(obj);
        }

        /**
         * Get a role with specified name.
         *
         * @param session
         *            the session
         * @param roleName
         *            specified role name
         * @return ScmRole
         * @throws ScmException
         *             If error happens
         */
        public static ScmRole getRole(ScmSession session, String roleName) throws ScmException {
            checkArgNotNull("session", session);
            checkStringArgNotEmpty("roleName", roleName);

            BSONObject obj = session.getDispatcher().getRole(roleName);
            return new ScmRoleImpl(obj);
        }

        /**
         * Get a role with specified id.
         *
         * @param session
         *            the session
         * @param roleId
         *            specified role id
         * @return ScmRole
         * @throws ScmException
         *             If error happens
         */
        public static ScmRole getRoleById(ScmSession session, String roleId) throws ScmException {
            checkArgNotNull("session", session);
            checkStringArgNotEmpty("roleId", roleId);

            BSONObject obj = session.getDispatcher().getRoleById(roleId);
            return new ScmRoleImpl(obj);
        }

        /**
         * List all the roles.
         *
         * @param session
         *            the session
         * @param orderBy
         *            the condition for sort, include: key is a property of
         *            {@link ScmAttributeName.Role}, value is -1(descending) or
         *            1(ascending)
         * @param skip
         *            skip to the first number record
         * @param limit
         *            return the total records of query, when value is -1,
         *            return all records
         * @return ScmCursor cursor
         * @throws ScmException
         *             If error happens
         * @since 3.1
         */
        public static ScmCursor<ScmRole> listRoles(ScmSession session, BSONObject orderBy,
                long skip, long limit) throws ScmException {
            checkArgNotNull("session", session);
            checkSkip(skip);
            checkLimit(limit);
            BsonReader reader = session.getDispatcher().listRoles(orderBy, skip, limit);
            ScmCursor<ScmRole> cursor = new ScmBsonCursor<ScmRole>(reader,
                    new BsonConverter<ScmRole>() {
                        @Override
                        public ScmRole convert(BSONObject obj) throws ScmException {
                            return new ScmRoleImpl(obj);
                        }
                    });
            return cursor;
        }

        /**
         * List all the roles.
         *
         * @param session
         *            the session
         * @return ScmCursor cursor
         * @throws ScmException
         *             If error happens
         */
        public static ScmCursor<ScmRole> listRoles(ScmSession session) throws ScmException {
            return listRoles(session, null, 0, -1);
        }

        /**
         * Delete specified role.
         *
         * @param session
         *            the session
         * @param roleName
         *            the name of the role to be deleted
         * @throws ScmException
         *             If error happens
         */
        public static void deleteRole(ScmSession session, String roleName) throws ScmException {
            checkArgNotNull("session", session);
            checkStringArgNotEmpty("roleName", roleName);

            session.getDispatcher().deleteRole(roleName);
        }

        /**
         * Delete specified role.
         *
         * @param session
         *            the session
         * @param role
         *            the role to be deleted
         * @throws ScmException
         *             If error happens
         */
        public static void deleteRole(ScmSession session, ScmRole role) throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("role", role);
            deleteRole(session, role.getRoleName());
        }

        /**
         * grant privilege to specified role.
         *
         * @param session
         *            the session
         * @param role
         *            the role to be deleted
         * @param resource
         *            the resource
         * @param privilege
         *            the privilege to grant, all available privilege is:
         *            READ,CREATE,UPDATE,DELETE,ALL
         * @throws ScmException
         *             If error happens
         */
        @Deprecated
        public static void grantPrivilege(ScmSession session, ScmRole role, ScmResource resource,
                String privilege) throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("role", role);
            checkArgNotNull("resource", resource);
            checkStringArgNotEmpty("privilege", privilege);

            session.getDispatcher().grantPrivilege(role.getRoleName(), resource.getType(),
                    resource.toStringFormat(), privilege);
        }

        /**
         * grant privilege to specified role.
         *
         * @param session
         *            the session
         * @param role
         *            the role to be deleted
         * @param resource
         *            the resource
         * @param privilege
         *            the privilege to grant
         * @throws ScmException
         *             If error happens
         */
        public static void grantPrivilege(ScmSession session, ScmRole role, ScmResource resource,
                ScmPrivilegeType privilege) throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("role", role);
            checkArgNotNull("resource", resource);
            checkArgNotNull("privilege", privilege);

            session.getDispatcher().grantPrivilege(role.getRoleName(), resource.getType(),
                    resource.toStringFormat(), privilege.getPriv());
        }

        /**
         * revoke privilege from specified role.
         *
         * @param session
         *            the session
         * @param role
         *            the role to be deleted
         * @param resource
         *            the resource
         * @param privilege
         *            the privilege to revoke, all available privilege is:
         *            READ,CREATE,UPDATE,DELETE,ALL
         * @throws ScmException
         *             If error happens
         */
        @Deprecated
        public static void revokePrivilege(ScmSession session, ScmRole role, ScmResource resource,
                String privilege) throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("role", role);
            checkArgNotNull("resource", resource);
            checkStringArgNotEmpty("privilege", privilege);

            session.getDispatcher().revokePrivilege(role.getRoleName(), resource.getType(),
                    resource.toStringFormat(), privilege);
        }

        /**
         * revoke privilege from specified role.
         *
         * @param session
         *            the session
         * @param role
         *            the role to be deleted
         * @param resource
         *            the resource
         * @param privilege
         *            the privilege to revoke
         * @throws ScmException
         *             If error happens
         */
        public static void revokePrivilege(ScmSession session, ScmRole role, ScmResource resource,
                ScmPrivilegeType privilege) throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("role", role);
            checkArgNotNull("resource", resource);
            checkArgNotNull("privilege", privilege);

            session.getDispatcher().revokePrivilege(role.getRoleName(), resource.getType(),
                    resource.toStringFormat(), privilege.getPriv());
        }
    }

    /**
     * Utility for operating user
     *
     * @since 3.0
     */
    public static class User {
        private User() {
        }

        /**
         * Create specified user.
         *
         * @param session
         *            the session
         * @param username
         *            specified username
         * @param passwordType
         *            password type of the user
         * @param password
         *            password
         * @return created ScmUser
         * @throws ScmException
         *             If error happens
         */
        public static ScmUser createUser(ScmSession session, String username,
                ScmUserPasswordType passwordType, String password) throws ScmException {
            checkArgNotNull("session", session);
            checkStringArgNotEmpty("username", username);
            checkArgNotNull("passwordType", passwordType);
            if (passwordType != ScmUserPasswordType.LDAP
                    && passwordType != ScmUserPasswordType.TOKEN) {
                checkStringArgNotEmpty("password", password);
            }
            BSONObject obj = session.getDispatcher().createUser(username, passwordType, password);
            return new ScmUserImpl(obj);
        }

        /**
         * Get specified user.
         *
         * @param session
         *            the session
         * @param username
         *            specified username
         * @return ScmUser
         * @throws ScmException
         *             If error happens
         */
        public static ScmUser getUser(ScmSession session, String username) throws ScmException {
            checkArgNotNull("session", session);
            checkStringArgNotEmpty("username", username);

            BSONObject obj = session.getDispatcher().getUser(username);
            return new ScmUserImpl(obj);
        }

        /**
         * Alter user with specified attributes.
         *
         * @param session
         *            the session
         * @param user
         *            the user to be altered
         * @param modifier
         *            the attributes to be modified
         * @return the altered ScmUser
         * @throws ScmException
         *             If error happens
         */
        public static ScmUser alterUser(ScmSession session, ScmUser user, ScmUserModifier modifier)
                throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("user", user);
            checkArgNotNull("modifier", modifier);

            BSONObject obj = session.getDispatcher().alterUser(user.getUsername(), modifier);
            return new ScmUserImpl(obj);
        }

        /**
         * List all users.
         *
         * @param session
         *            the session
         * @return ScmCursor
         * @throws ScmException
         *             If error happens
         */
        public static ScmCursor<ScmUser> listUsers(ScmSession session) throws ScmException {
            return listUsers(session, null);
        }

        /**
         * List users by condition.
         *
         * @param session
         *            the session
         * @param filter
         *            the condition for query, include:
         *            password_type,enabled,has_role
         * @return ScmCursor
         * @throws ScmException
         *             If error happens
         */
        public static ScmCursor<ScmUser> listUsers(ScmSession session, BSONObject filter)
                throws ScmException {
            return listUsers(session, filter, 0, -1);
        }

        /**
         * List users by condition.
         *
         * @param session
         *            the session
         * @param filter
         *            the condition for query, include:
         *            password_type,enabled,has_role
         * @param skip
         *            skip to the first number Record
         * @param limit
         *            return the total records of query, when value is -1,
         *            return all records
         * @return ScmCursor
         * @throws ScmException
         *             If error happens
         * @since 3.1
         */
        public static ScmCursor<ScmUser> listUsers(ScmSession session, BSONObject filter, long skip,
                long limit) throws ScmException {
            checkArgNotNull("session", session);
            checkSkip(skip);
            checkLimit(limit);
            BsonReader reader = session.getDispatcher().listUsers(filter, skip, limit);
            ScmCursor<ScmUser> cursor = new ScmBsonCursor<ScmUser>(reader,
                    new BsonConverter<ScmUser>() {
                        @Override
                        public ScmUser convert(BSONObject obj) throws ScmException {
                            return new ScmUserImpl(obj);
                        }
                    });
            return cursor;
        }

        /**
         * Delete specified user.
         *
         * @param session
         *            the session
         * @param username
         *            the name of the user to be deleted
         * @throws ScmException
         *             If error happens
         */
        public static void deleteUser(ScmSession session, String username) throws ScmException {
            checkArgNotNull("session", session);
            checkStringArgNotEmpty("username", username);

            session.getDispatcher().deleteUser(username);
        }

        /**
         * Delete specified user.
         *
         * @param session
         *            the session
         * @param user
         *            the user to be deleted
         * @throws ScmException
         *             If error happens
         */
        public static void deleteUser(ScmSession session, ScmUser user) throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("user", user);

            deleteUser(session, user.getUsername());
        }
    }

    /**
     * Utility for operating workspace
     *
     * @since 2.1
     */
    public static class Workspace {

        private Workspace() {

        }

        /**
         * Create workspace with specified config.
         *
         * @param ss
         *            session.
         * @param conf
         *            workspace config.
         * @return the workspace instance.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmWorkspace createWorkspace(ScmSession ss, ScmWorkspaceConf conf)
                throws ScmException {
            if (ss == null) {
                throw new ScmInvalidArgumentException("session is null");
            }

            if (conf == null) {
                throw new ScmInvalidArgumentException("conf is null");
            }
            BSONObject wsBSON = ss.getDispatcher().createWorkspace(conf.getName(),
                    conf.getBSONObject());
            return new ScmWorkspaceImpl(ss, wsBSON);
        }

        /**
         * Acquires an object of the ScmWorkspace class by the specified name.
         *
         * @param name
         *            The workspace name.
         * @param ss
         *            Session object.
         * @return An object of the ScmWorkspace type.
         * @throws ScmException
         *             if error happens
         * @since 2.1
         */
        public static ScmWorkspace getWorkspace(String name, ScmSession ss) throws ScmException {
            if (name == null) {
                throw new ScmInvalidArgumentException("name is null");
            }
            if (ss == null) {
                throw new ScmInvalidArgumentException("session is null");
            }

            BSONObject wsBSON = ss.getDispatcher().getWorkspace(name);

            ScmWorkspaceImpl ws = new ScmWorkspaceImpl(ss, wsBSON);
            // logger.debug("[ScmSession.getWorkspace] " + "wsID:" + ws.getId()
            // + ", " + "wsName:"
            // + ws.getName());
            return ws;
        }

        /**
         * Acquires ScmWorkspaceInfo instance list.
         *
         * @param ss
         *            session object
         * @return A cursor to traverse
         * @throws ScmException
         *             if errors happens
         * @since 2.2
         */
        public static ScmCursor<ScmWorkspaceInfo> listWorkspace(ScmSession ss) throws ScmException {
            return listWorkspace(ss, null, 0, -1);
        }

        /**
         * Acquires ScmWorkspaceInfo instance list.
         *
         * @param ss
         *            session object
         * @param orderBy
         *            the condition for sort, include: key is a property of
         *            {@link ScmAttributeName.Workspace}, value is
         *            -1(descending) or 1(ascending)
         * @param skip
         *            skip to the first number Record
         * @param limit
         *            return the total records of query, when value is -1,
         *            return all records
         * @return A cursor to traverse
         * @throws ScmException
         *             if errors happens
         * @since 3.1
         */
        public static ScmCursor<ScmWorkspaceInfo> listWorkspace(ScmSession ss, BSONObject orderBy,
                long skip, long limit) throws ScmException {
            if (null == ss) {
                throw new ScmInvalidArgumentException("session is null");
            }
            checkSkip(skip);
            checkLimit(limit);
            BsonReader reader = ss.getDispatcher().getWorkspaceList(new BasicBSONObject(), orderBy,
                    skip, limit);
            ScmCursor<ScmWorkspaceInfo> cusor = new ScmBsonCursor<ScmWorkspaceInfo>(reader,
                    new BsonConverter<ScmWorkspaceInfo>() {
                        @Override
                        public ScmWorkspaceInfo convert(BSONObject obj) throws ScmException {
                            return new ScmWorkspaceInfo(obj);
                        }
                    });
            return cusor;
        }

        /**
         * Delete specified workspace.
         *
         * @param ss
         *            session.
         * @param wsName
         *            workspace name.
         * @param isEnforced
         *            is enforced.
         * @throws ScmException
         *             if error happens.
         */
        public static void deleteWorkspace(ScmSession ss, String wsName, boolean isEnforced)
                throws ScmException {
            if (ss == null) {
                throw new ScmInvalidArgumentException("session is null");
            }

            if (wsName == null) {
                throw new ScmInvalidArgumentException("wsName is null");
            }
            ss.getDispatcher().deleteWorkspace(wsName, isEnforced);
        }

        /**
         * Delete specified workspace.
         *
         * @param ss
         *            session.
         * @param wsName
         *            workspace name.
         * @throws ScmException
         *             if error happens.
         */
        public static void deleteWorkspace(ScmSession ss, String wsName) throws ScmException {
            deleteWorkspace(ss, wsName, false);
        }

    }

    /**
     * Utility for operating site.
     *
     * @since 2.2
     */
    public static class Site {
        private Site() {
            super();
        }

        /**
         * Acquires ScmSiteInfo instance list
         *
         * @param ss
         *            session object.
         * @return A cursor to traverse
         * @throws ScmException
         *             If error happens.
         * @since 2.2
         */
        public static ScmCursor<ScmSiteInfo> listSite(ScmSession ss) throws ScmException {
            if (null == ss) {
                throw new ScmInvalidArgumentException("session is null");
            }

            BsonReader reader = ss.getDispatcher().getSiteList(new BasicBSONObject());
            ScmCursor<ScmSiteInfo> cusor = new ScmBsonCursor<ScmSiteInfo>(reader,
                    new BsonConverter<ScmSiteInfo>() {
                        @Override
                        public ScmSiteInfo convert(BSONObject obj) throws ScmException {
                            return new ScmSiteInfo(obj);
                        }
                    });
            return cusor;
        }
    }

    /**
     * Utility for operating node
     *
     * @since 2.1
     */
    public static class Node {
        private Node() {
            super();
        }

        /**
         * Acquires ScmNodeInfo instance list
         *
         * @param ss
         *            Session object
         * @return A cursor to traverse
         * @throws ScmException
         *             if error happens
         * @since 3.0
         */
        public static ScmCursor<ScmNodeInfo> listNode(ScmSession ss) throws ScmException {
            return listNode(ss, new BasicBSONObject());
        }

        /**
         * Acquires ScmNodeInfo instance list
         *
         * @param ss
         *            Session object
         * @param condition
         *            Condition
         * @return A cursor to traverse
         * @throws ScmException
         *             if error happens
         * @since 3.0
         */
        public static ScmCursor<ScmNodeInfo> listNode(ScmSession ss, BSONObject condition)
                throws ScmException {
            if (null == ss) {
                throw new ScmInvalidArgumentException("session is null");
            }

            if (null == condition) {
                throw new ScmInvalidArgumentException("condition is null");
            }

            BsonReader reader = ss.getDispatcher().getNodeList(condition);
            ScmCursor<ScmNodeInfo> cusor = new ScmBsonCursor<ScmNodeInfo>(reader,
                    new BsonConverter<ScmNodeInfo>() {
                        @Override
                        public ScmNodeInfo convert(BSONObject obj) throws ScmException {
                            return new ScmNodeInfo(obj);
                        }
                    });
            return cusor;
        }
    }

    /**
     * Utility for operating class.
     */
    public static class Class {
        private Class() {
        }

        /**
         * Create a Scm Class by specified name.
         *
         * @param ws
         *            workspace.
         * @param className
         *            class name.
         * @param description
         *            class description.
         * @return instance of ScmClass.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmClass createInstance(ScmWorkspace ws, String className, String description)
                throws ScmException {
            checkArgNotNull("workspace", ws);

            ScmClassImpl scmClass = new ScmClassImpl();
            scmClass.setName(className);
            scmClass.setDescription(description);
            scmClass.setWorkspace(ws);
            scmClass.save();

            return scmClass;
        }

        /**
         * Acquires an object of the ScmClass by the specified classId from the
         * specified workspace.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param classId
         *            The ID of the Class instance.
         * @return An object of the ScmClass type.
         * @throws ScmException
         *             if error happens
         */
        public static ScmClass getInstance(ScmWorkspace ws, ScmId classId) throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("classId", classId);

            BSONObject classInfo = ws.getSession().getDispatcher().getClassInfo(ws.getName(),
                    classId);
            return new ScmClassImpl(ws, classInfo);
        }

        /**
         * Acquires an object of the ScmClass by the specified className from
         * the specified workspace.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param className
         *            The name of the Class instance.
         * @return An object of the ScmClass type.
         * @throws ScmException
         *             if error happens
         * @since 3.1
         */
        public static ScmClass getInstanceByName(ScmWorkspace ws, String className)
                throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("className", className);

            BSONObject classInfo = ws.getSession().getDispatcher().getClassInfo(ws.getName(),
                    className);
            return new ScmClassImpl(ws, classInfo);
        }

        /**
         * Acquires ScmClass instance set which matches between the specified
         * workspace.
         *
         * @param ws
         *            The ScmWorkspace object for the workspace in which this
         *            class instance is to be located.
         * @param filter
         *            the query filter
         * @return A cursor to traverse
         * @throws ScmException
         *             if error happens
         */
        public static ScmCursor<ScmClassBasicInfo> listInstance(ScmWorkspace ws, BSONObject filter)
                throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("filter", filter);

            BsonReader reader = ws.getSession().getDispatcher().getClassList(ws.getName(), filter);
            ScmCursor<ScmClassBasicInfo> classCursor = new ScmBsonCursor<ScmClassBasicInfo>(reader,
                    new BsonConverter<ScmClassBasicInfo>() {
                        @Override
                        public ScmClassBasicInfo convert(BSONObject obj) throws ScmException {
                            return new ScmClassBasicInfo(obj);
                        }
                    });
            return classCursor;
        }

        /**
         * Delete specified scm class.
         *
         * @param ws
         *            workspace.
         * @param classId
         *            class id.
         * @throws ScmException
         *             if error happens.
         */
        public static void deleteInstance(ScmWorkspace ws, ScmId classId) throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("classId", classId);
            ws.getSession().getDispatcher().deleteClass(ws.getName(), classId);
        }

        /**
         * Delete specified scm class.
         *
         * @param ws
         *            workspace.
         * @param className
         *            class name.
         * @throws ScmException
         *             if error happens.
         * @since 3.1
         */
        public static void deleteInstanceByName(ScmWorkspace ws, String className)
                throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("className", className);
            ws.getSession().getDispatcher().deleteClass(ws.getName(), className);
        }
    }

    /**
     * Utility for operating attribute.
     *
     */
    public static class Attribute {
        private Attribute() {
        }

        /**
         * Create an attribute with specified conf.
         *
         * @param ws
         *            workspace.
         * @param conf
         *            the config of the attribute.
         * @return instance of ScmAttribute.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmAttribute createInstance(ScmWorkspace ws, ScmAttributeConf conf)
                throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("conf", conf);

            BSONObject attrInfo = ws.getSession().getDispatcher().createAttribute(ws.getName(),
                    conf.toBSONObject());

            return new ScmAttributeImpl(ws, attrInfo);
        }

        /**
         * Get a attribute with specified id.
         *
         * @param ws
         *            workspace.
         * @param attrId
         *            attribute id.
         * @return instance of ScmAttribute.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmAttribute getInstance(ScmWorkspace ws, ScmId attrId) throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("attrId", attrId);

            BSONObject attrInfo = ws.getSession().getDispatcher().getAttributeInfo(ws.getName(),
                    attrId);
            if (null == attrInfo) {
                throw new ScmException(ScmError.METADATA_ATTR_NOT_EXIST,
                        "Attribute is unexist:workspace=" + ws.getName() + ",attrId="
                                + attrId.get());
            }

            return new ScmAttributeImpl(ws, attrInfo);
        }

        /**
         * List attributes with specified filter.
         *
         * @param ws
         *            workspace.
         * @param filter
         *            filter.
         * @return cursor.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmCursor<ScmAttribute> listInstance(final ScmWorkspace ws, BSONObject filter)
                throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("filter", filter);

            BsonReader reader = ws.getSession().getDispatcher().getAttributeList(ws.getName(),
                    filter);
            ScmCursor<ScmAttribute> attrCursor = new ScmBsonCursor<ScmAttribute>(reader,
                    new BsonConverter<ScmAttribute>() {
                        @Override
                        public ScmAttribute convert(BSONObject obj) throws ScmException {
                            return new ScmAttributeImpl(ws, obj);
                        }
                    });
            return attrCursor;
        }

        /**
         * Delete specified attribute.
         *
         * @param ws
         *            workspace.
         * @param attrId
         *            attribute id.
         * @throws ScmException
         *             if error happens.
         */
        public static void deleteInstance(ScmWorkspace ws, ScmId attrId) throws ScmException {
            checkArgNotNull("workspace", ws);
            checkArgNotNull("attrId", attrId);
            ws.getSession().getDispatcher().deleteAttribute(ws.getName(), attrId);
        }
    }

    private static void checkArgNotNull(String argName, Object arg) throws ScmException {
        if (arg == null) {
            throw new ScmInvalidArgumentException(argName + " is null");
        }
    }

    private static void checkStringArgNotEmpty(String argName, String argValue)
            throws ScmException {
        if (!Strings.hasText(argValue)) {
            throw new ScmInvalidArgumentException(argName + " is null or empty");
        }
    }

    private static void checkLimit(long limit) throws ScmInvalidArgumentException {
        if (limit < -1) {
            throw new ScmInvalidArgumentException("limit can not be less than -1");
        }
    }

    private static void checkSkip(long skip) throws ScmInvalidArgumentException {
        if (skip < 0) {
            throw new ScmInvalidArgumentException("skip can not be less than 0");
        }
    }
}

class PrivilegeBSONConverter<ScmPrivilege> implements BsonConverter<ScmPrivilege> {

    private ScmSession ss;

    PrivilegeBSONConverter(ScmSession ss) {
        this.ss = ss;
    }

    @Override
    public ScmPrivilege convert(BSONObject obj) throws ScmException {
        return (ScmPrivilege) new ScmPrivilegeImpl(ss, obj);
    }
}
