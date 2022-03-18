package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmClassBasic;
import com.sequoiacm.om.omserver.module.OmClassDetail;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

import java.util.List;

public interface ScmMetaDataService {

    List<OmClassBasic> listClass(ScmOmSession session, String wsName, BSONObject filter,
            BSONObject orderBy, int skip, int limit)
            throws ScmOmServerException, ScmInternalException;

    long getClassCount(ScmOmSession session, String wsName, BSONObject condition)
            throws ScmOmServerException, ScmInternalException;

    OmClassDetail getClassDetail(ScmOmSession session, String wsName, String id)
            throws ScmOmServerException, ScmInternalException;
}
