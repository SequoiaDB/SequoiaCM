package com.sequoiacm.om.omserver.service;

import java.io.InputStream;
import java.util.List;

import com.sequoiacm.om.omserver.module.*;
import org.bson.BSONObject;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmFileService {
    OmFileDetail getFileDetail(ScmOmSession session, String wsName, String id, int majorVersion,
            int minorVersion) throws ScmInternalException, ScmOmServerException;

    void uploadFile(ScmOmSession session, String wsName, String siteName, OmFileInfo fileInfo,
            BSONObject uploadConf, InputStream is)
            throws ScmInternalException, ScmOmServerException;

    OmFileContent downloadFile(ScmOmSession session, String wsName, String siteName, String id,
            int majorVersion, int minorVersion) throws ScmInternalException, ScmOmServerException;

    long getFileCount(ScmOmSession session, String wsName, int scope, BSONObject condition)
            throws ScmOmServerException, ScmInternalException;

    List<OmFileBasic> getFileList(ScmOmSession session, String wsName, int scope,
            BSONObject condition, BSONObject orderBy, long skip, long limit)
            throws ScmInternalException, ScmOmServerException;

    void deleteFiles(ScmOmSession session, String wsName, List<String> fileIdList)
            throws ScmInternalException, ScmOmServerException;

    void updateFileContent(ScmOmSession session, String wsName, String id, String siteName,
            BSONObject updateContentOption, InputStream newFileContent)
            throws ScmOmServerException, ScmInternalException;
}
