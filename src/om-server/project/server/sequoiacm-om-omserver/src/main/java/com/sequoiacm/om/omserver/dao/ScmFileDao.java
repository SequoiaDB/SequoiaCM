package com.sequoiacm.om.omserver.dao;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileContent;
import com.sequoiacm.om.omserver.module.OmFileDetail;

public interface ScmFileDao {
    public long countFile(String ws) throws ScmInternalException;

    public OmFileDetail getFileDetail(String ws, String fileId, int majorVersion, int minorVersion)
            throws ScmInternalException, ScmOmServerException;

    public List<OmFileBasic> getFileList(String ws, BSONObject condition, long skip, long limit)
            throws ScmInternalException;

    public OmFileContent downloadFile(String ws, String fileId, int majorVersion, int minorVersion)
            throws ScmInternalException, ScmOmServerException;
}
