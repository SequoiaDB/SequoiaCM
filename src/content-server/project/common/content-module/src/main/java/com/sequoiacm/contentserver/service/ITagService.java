package com.sequoiacm.contentserver.service;

import com.sequoiacm.contentserver.tag.syntaxtree.TagSyntaxTree;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;

public interface ITagService {
    MetaCursor searchFile(String ws, ScmUser user, TagSyntaxTree tagSyntaxTree,
            BSONObject fileCondition, BSONObject orderBy, int scope, long skip, long limit,
            boolean isResContainsDeleteMarker) throws ScmServerException;

    long countFile(String ws, ScmUser user, TagSyntaxTree tagSyntaxTree, BSONObject fileCondition,
            int scope, boolean isResContainsDeleteMarker) throws ScmServerException;


    MetaCursor queryTag(String ws, ScmUser user, BSONObject condition, BSONObject orderBy,
                        long skip, long limit) throws ScmServerException;

    MetaCursor queryCustomTagKey(String workspaceName, ScmUser user, BSONObject condition,
            boolean ascending, long skip, long limit) throws ScmServerException;

    long countTag(String workspaceName, ScmUser user, BSONObject condition)
            throws ScmServerException;
}
