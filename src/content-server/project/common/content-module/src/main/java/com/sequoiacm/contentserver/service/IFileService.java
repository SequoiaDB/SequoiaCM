package com.sequoiacm.contentserver.service;

import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.dao.FileReaderDao;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.FileUploadConf;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.TransactionContext;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import java.io.InputStream;
import java.util.Date;

public interface IFileService {

    /*
     * void getFileList(PrintWriter writer, String workspaceName, BSONObject
     * condition, int scope) throws ScmServerException;
     */
    MetaCursor getFileList(ScmUser user, String workspaceName, BSONObject condition, int scope,
            BSONObject orderby, long skip, long limit, BSONObject selector,
            boolean isResContainsDeleteMarker) throws ScmServerException;

    MetaCursor getFileList(String workspaceName, BSONObject condition, int scope,
                           BSONObject orderby, long skip, long limit, BSONObject selector,
                           boolean isResContainsDeleteMarker) throws ScmServerException;

    MetaCursor getDirSubFileList(ScmUser user, String workspaceName, String dirId,
            BSONObject condition, int scope, BSONObject orderby, long skip, long limit,
            BSONObject selector) throws ScmServerException;

    FileReaderDao downloadFile(String sessionId, String userDetail, ScmUser user,
            String workspaceName, FileMeta fileInfo, int readflag) throws ScmServerException;

    FileReaderDao downloadFile(String sessionId, String userDetail, String workspaceName,
            FileMeta fileInfo, int readflag) throws ScmServerException;

    void deleteFile(String sessionid, String userDetail, ScmUser user, String workspaceName,
            String fileId, int majorVersion, int minorVersion, boolean isPhysical)
            throws ScmServerException;

    void deleteFile(String sessionid, String userDetail, String workspaceName, String fileId,
            int majorVersion, int minorVersion, boolean isPhysical) throws ScmServerException;

    long countFiles(ScmUser user, String workspaceName, int scope, BSONObject condition,
            boolean isResContainsDeleteMarker) throws ScmServerException;

    long countFiles(String workspaceName, int scope, BSONObject condition,
            boolean isResContainsDeleteMarker) throws ScmServerException;

    long sumFileSizes(String workspaceName, int scope, BSONObject condition)
            throws ScmServerException;

    FileMeta updateFileInfo(ScmUser user, String workspaceName, String fileId,
            BSONObject fileInfo, int majorVersion, int minorVersion) throws ScmServerException;

    void updateFileExternalData(String workspaceName, BSONObject matcher, BSONObject externalData)
            throws ScmServerException;

    boolean updateFileExternalData(String workspaceName, String fileId, int majorVersion,
            int minorVersion, BSONObject externalData, TransactionContext transactionContext)
            throws ScmServerException;

    FileMeta updateFileContent(ScmUser user, String workspaceName, String fileId,
            InputStream newFileContent, int majorVersion, int minorVersion,
            ScmUpdateContentOption option) throws ScmServerException;

    FileMeta updateFileContent(ScmUser user, String workspaceName, String fileId,
            String newBreakpointFileContent, int majorVersion, int minorVersion,
            ScmUpdateContentOption option) throws ScmServerException;

    void asyncTransferFile(ScmUser user, String workspaceName, String fileId, int majorVersion,
            int minorVersion, String targetSite) throws ScmServerException;

    void asyncCacheFile(ScmUser user, String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException;

    FileMeta getFileInfoById(ScmUser user, String workspaceName, String fileId, int majorVersion,
            int minorVersion, boolean acceptDeleteMarker) throws ScmServerException;

    FileMeta getFileInfoById(String workspaceName, String fileId, int majorVersion,
            int minorVersion, boolean acceptDeleteMarker) throws ScmServerException;

    FileMeta createFile(String workspace, FileMeta fileMeta, FileUploadConf conf,
            TransactionCallback transactionCallback)
            throws ScmServerException;

    FileMeta createFile(ScmUser user, String workspace, FileMeta fileMeta, FileUploadConf conf,
            InputStream data) throws ScmServerException;

    FileMeta createFile(String workspace, FileMeta fileMeta, FileUploadConf conf, InputStream data)
            throws ScmServerException;

    FileMeta createFile(ScmUser user, String workspace, FileMeta fileMeta, FileUploadConf conf,
            String breakpointFile) throws ScmServerException;

    FileMeta createFile(String workspace, FileMeta fileMeta, FileUploadConf conf,
            String breakpointFile) throws ScmServerException;

    String calcFileMd5(String sessionid, String userDetail, ScmUser user, String workspaceName,
            String fileId, int majorVersion, int minorVersion) throws ScmServerException;

    String generateId(Date fileCreateTime) throws ScmServerException;

    BasicBSONList getFileContentLocations(ScmUser user, FileMeta fileInfo, String workspaceName)
            throws ScmServerException;

    FileMeta deleteVersion(ScmUser user, String bucket, String fileName, int majorVersion,
            int minorVersion) throws ScmServerException;

}
