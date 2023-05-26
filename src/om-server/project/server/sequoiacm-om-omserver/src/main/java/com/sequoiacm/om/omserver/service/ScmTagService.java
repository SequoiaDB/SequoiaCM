package com.sequoiacm.om.omserver.service;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.tag.OmTagBasic;
import com.sequoiacm.om.omserver.module.tag.OmTagFilter;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmTagService {

    List<OmTagBasic> listTag(ScmOmSession session, String wsName, String tagType,
            OmTagFilter omTagFilter, long skip, int limit)
            throws ScmOmServerException, ScmInternalException;

    long countTag(ScmOmSession session, String wsName, String tagType, OmTagFilter omTagFilter)
            throws ScmOmServerException, ScmInternalException;

    List<String> listCustomTagKey(ScmOmSession session, String wsName, String keyMatcher, long skip,
            int limit) throws ScmOmServerException, ScmInternalException;

    List<OmFileBasic> searchFileWithTag(ScmOmSession session, String wsName, int scope,
            BSONObject tagCondition, BSONObject fileCondition, BSONObject orderBy, long skip,
            long limit) throws ScmOmServerException, ScmInternalException;

    long countFileWithTag(ScmOmSession session, String wsName, int scope, BSONObject tagCondition,
            BSONObject fileCondition) throws ScmOmServerException, ScmInternalException;
}
