package com.sequoiacm.om.omserver.dao;

import java.io.InputStream;
import java.util.List;

import com.sequoiacm.om.omserver.module.*;
import org.bson.BSONObject;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;

public interface ScmFileDao {

    long countFile(String wsName, int scope, BSONObject condition) throws ScmInternalException;

    OmFileDetail getFileDetail(String wsName, String fileId, int majorVersion, int minorVersion)
            throws ScmInternalException, ScmOmServerException;

    List<OmFileBasic> getFileList(String wsName, int scope, BSONObject condition,
            BSONObject orderBy, long skip, long limit) throws ScmInternalException;

    void uploadFile(String wsName, OmFileInfo fileInfo, BSONObject uploadConf,
            InputStream inputStream) throws ScmInternalException;

    OmFileContent downloadFile(String wsName, String fileId, int majorVersion, int minorVersion)
            throws ScmInternalException, ScmOmServerException;

    void deleteFiles(String wsName, List<String> fileIdList)
            throws ScmInternalException, ScmOmServerException;

    void updateFileContent(String wsName, String id, BSONObject updateContentOption,
            InputStream newFileContent) throws ScmInternalException;
}
