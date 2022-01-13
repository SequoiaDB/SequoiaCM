package com.sequoiacm.contentserver.service;

import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.dao.FileReaderDao;
import com.sequoiacm.contentserver.model.ClientUploadConf;
import com.sequoiacm.contentserver.model.ScmDataInfoDetail;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserPasswordType;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.TransactionContext;
import org.bson.BSONObject;

import java.io.InputStream;
import java.util.Date;

public interface IFileService {

    /*
     * void getFileList(PrintWriter writer, String workspaceName, BSONObject
     * condition, int scope) throws ScmServerException;
     */
    MetaCursor getFileList(ScmUser user, String workspaceName, BSONObject condition, int scope,
            BSONObject orderby, long skip, long limit, BSONObject selector)
            throws ScmServerException;

    MetaCursor getFileList(String workspaceName, BSONObject condition, int scope,
            BSONObject orderby, long skip, long limit, BSONObject selector)
            throws ScmServerException;

    MetaCursor getDirSubFileList(ScmUser user, String workspaceName, String dirId,
            BSONObject condition, int scope, BSONObject orderby, long skip, long limit,
            BSONObject selector) throws ScmServerException;

    FileReaderDao downloadFile(String sessionId, String userDetail, ScmUser user,
            String workspaceName, BSONObject fileInfo, int readflag) throws ScmServerException;

    FileReaderDao downloadFile(String sessionId, String userDetail, String workspaceName,
            BSONObject fileInfo, int readflag) throws ScmServerException;

    void deleteFile(String sessionid, String userDetail, ScmUser user, String workspaceName,
            String fileId, int majorVersion, int minorVersion, boolean isPhysical)
            throws ScmServerException;

    void deleteFile(String sessionid, String userDetail, String workspaceName, String fileId,
            int majorVersion, int minorVersion, boolean isPhysical) throws ScmServerException;

    long countFiles(ScmUser user, String workspaceName, int scope, BSONObject condition)
            throws ScmServerException;
    
    long countFiles(String workspaceName, int scope, BSONObject condition)
            throws ScmServerException;

    long sumFileSizes(String workspaceName, int scope, BSONObject condition)
            throws ScmServerException;

    BSONObject updateFileInfo(ScmUser user, String workspaceName, String fileId,
            BSONObject fileInfo, int majorVersion, int minorVersion) throws ScmServerException;

    void updateFileExternalData(String workspaceName, BSONObject matcher, BSONObject externalData)
            throws ScmServerException;

    boolean updateFileExternalData(String workspaceName, String fileId, int majorVersion,
            int minorVersion, BSONObject externalData, TransactionContext transactionContext)
            throws ScmServerException;

    BSONObject updateFileContent(ScmUser user, String workspaceName, String fileId,
            InputStream newFileContent, int majorVersion, int minorVersion,
            ScmUpdateContentOption option) throws ScmServerException;

    BSONObject updateFileContent(ScmUser user, String workspaceName, String fileId,
            String newBreakpointFileContent, int majorVersion, int minorVersion,
            ScmUpdateContentOption option) throws ScmServerException;

    void asyncTransferFile(ScmUser user, String workspaceName, String fileId, int majorVersion,
            int minorVersion, String targetSite) throws ScmServerException;

    void asyncCacheFile(ScmUser user, String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException;

    BSONObject getFileInfoById(ScmUser user, String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException;
    
    BSONObject getFileInfoById(String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException;

    BSONObject getFileInfoByPath(ScmUser user, String workspaceName, String filePath,
            int majoVersion, int minorVersion) throws ScmServerException;

    BSONObject uploadFile(ScmUser user, String workspaceName, InputStream is, BSONObject fileInfo,
            String sessionId, String userDetail, ScmUserPasswordType passwordType,
            ClientUploadConf uploadConf) throws ScmServerException;

    BSONObject uploadFile(ScmUser user, String workspaceName, String breakpointFileName,
            BSONObject fileInfo, String sessionId, String userDetail,
            ScmUserPasswordType passwordType, ClientUploadConf uploadConfig)
            throws ScmServerException;

    String calcFileMd5(String sessionid, String userDetail, ScmUser user, String workspaceName,
            String fileId, int majorVersion, int minorVersion) throws ScmServerException;

    String generateId(Date fileCreateTime) throws ScmServerException;

    BSONObject createFileMeta(String ws, ScmUser user, BSONObject fileInfo,
            ScmDataInfoDetail dataInfoDetail, TransactionCallback callback)
            throws ScmServerException;

}
