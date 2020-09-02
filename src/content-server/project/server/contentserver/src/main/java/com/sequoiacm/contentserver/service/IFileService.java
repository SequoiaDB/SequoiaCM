package com.sequoiacm.contentserver.service;

import java.io.InputStream;

import org.bson.BSONObject;

import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.contentserver.dao.FileReaderDao;
import com.sequoiacm.contentserver.model.ClientUploadConf;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUserPasswordType;
import com.sequoiacm.metasource.MetaCursor;

public interface IFileService {

    /*
     * void getFileList(PrintWriter writer, String workspaceName, BSONObject
     * condition, int scope) throws ScmServerException;
     */
    MetaCursor getFileList(String workspaceName, BSONObject condition, int scope,
            BSONObject orderby, long skip, long limit, BSONObject selector)
            throws ScmServerException;

    BSONObject uploadFile(String workspaceName, String username, InputStream is,
            BSONObject fileInfo) throws ScmServerException;

    BSONObject uploadFile(String workspaceName, String username, String breakpointFileName,
            BSONObject fileInfo) throws ScmServerException;

    FileReaderDao downloadFile(String sessionId, String userDetail, String workspaceName,
            BSONObject fileInfo, int readflag) throws ScmServerException;

    void deleteFile(String sessionid, String userDetail, String workspaceName, String username,
            String fileId, int majorVersion, int minorVersion, boolean isPhysical)
            throws ScmServerException;

    long countFiles(String workspaceName, int scope, BSONObject condition)
            throws ScmServerException;

    long sumFileSizes(String workspaceName, int scope, BSONObject condition)
            throws ScmServerException;

    BSONObject updateFileInfo(String workspaceName, String user, String fileId, BSONObject fileInfo,
            int majorVersion, int minorVersion) throws ScmServerException;

    void updateFileExternalData(String workspaceName, BSONObject matcher, BSONObject externalData)
            throws ScmServerException;

    boolean updateFileExternalData(String workspaceName, String fileId, int majorVersion,
            int minorVersion, BSONObject externalData) throws ScmServerException;

    BSONObject updateFileContent(String workspaceName, String user, String fileId,
            InputStream newFileContent, int majorVersion, int minorVersion,
            ScmUpdateContentOption option) throws ScmServerException;

    BSONObject updateFileContent(String workspaceName, String user, String fileId,
            String newBreakpointFileContent, int majorVersion, int minorVersion,
            ScmUpdateContentOption option) throws ScmServerException;

    void asyncTransferFile(String workspaceName, String fileId, int majorVersion, int minorVersion)
            throws ScmServerException;

    void asyncCacheFile(String workspaceName, String fileId, int majorVersion, int minorVersion)
            throws ScmServerException;

    BSONObject getFileInfoById(String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException;

    BSONObject getFileInfoByPath(String workspaceName, String filePath, int majoVersion,
            int minorVersion) throws ScmServerException;

    BSONObject uploadFile(String workspaceName, String username, InputStream is,
            BSONObject fileInfo, String sessionId, String userDetail,
            ScmUserPasswordType passwordType, ClientUploadConf uploadConf)
            throws ScmServerException;

    BSONObject uploadFile(String workspaceName, String username, String breakpointFileName,
            BSONObject fileInfo, String sessionId, String userDetail,
            ScmUserPasswordType passwordType, ClientUploadConf uploadConfig)
            throws ScmServerException;

    String calcFileMd5(String sessionid, String userDetail, String workspaceName, String fileId,
            int majorVersion, int minorVersion) throws ScmServerException;
}
