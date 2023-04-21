package com.sequoiacm.metasource;

import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import org.bson.BSONObject;

public interface MetaQuotaAccessor extends MetaAccessor {

    BSONObject getQuotaInfo(String type, String name) throws ScmMetasourceException;

    void updateQuotaInfo(String type, String name, int quotaRoundNumber, BSONObject updator)
            throws SdbMetasourceException;
}
