package com.sequoiacm.metasource;

import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import org.bson.BSONObject;

public interface MetaQuotaSyncAccessor extends MetaAccessor {
    BSONObject getQuotaSyncInfo(String type, String name, int syncRoundNumber)
            throws ScmMetasourceException;

    void updateQuotaSyncInfo(String type, String name, int syncRoundNumber, BSONObject updator)
            throws SdbMetasourceException;
}
