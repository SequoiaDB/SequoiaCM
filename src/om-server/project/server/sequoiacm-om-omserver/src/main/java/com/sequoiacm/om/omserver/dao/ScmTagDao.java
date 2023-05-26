package com.sequoiacm.om.omserver.dao;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.tag.OmTagBasic;
import com.sequoiacm.om.omserver.module.tag.OmTagFilter;

public interface ScmTagDao {

    List<OmTagBasic> listTag(String wsName, String tagType, OmTagFilter omTagFilter, long skip,
            int limit) throws ScmInternalException;

    long countTag(String wsName, String tagType, OmTagFilter omTagFilter)
            throws ScmInternalException;

    List<String> listCustomTagKey(String wsName, String keyMatcher, long skip, int limit)
            throws ScmInternalException;

    List<OmFileBasic> searchFileWithTag(String wsName, int scope, BSONObject tagCondition,
            BSONObject fileCondition, BSONObject orderBy, long skip, long limit)
            throws ScmInternalException;

    long countFileWithTag(String wsName, int scope, BSONObject tagCondition,
            BSONObject fileCondition) throws ScmInternalException;
}
