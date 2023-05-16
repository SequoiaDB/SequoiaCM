package com.sequoiacm.contentserver.tag.syntaxtree;

import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;

public interface TagSyntaxTreeNode {
    BSONObject toSdbFileMatcher() throws ScmServerException;
}
