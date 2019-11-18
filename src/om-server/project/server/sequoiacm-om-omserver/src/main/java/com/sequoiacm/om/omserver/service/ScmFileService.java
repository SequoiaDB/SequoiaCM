package com.sequoiacm.om.omserver.service;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileContent;
import com.sequoiacm.om.omserver.module.OmFileDetail;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmFileService {
    OmFileDetail getFileDetail(ScmOmSession session, String ws, String id, int majorVersion,
            int minorVersion) throws ScmInternalException, ScmOmServerException;

    OmFileContent downloadFile(ScmOmSession session, String ws, String id, int majorVersion,
            int minorVersion) throws ScmInternalException, ScmOmServerException;

    List<OmFileBasic> getFileList(ScmOmSession session, String ws, BSONObject condition, long skip,
            long limit) throws ScmInternalException, ScmOmServerException;

}
