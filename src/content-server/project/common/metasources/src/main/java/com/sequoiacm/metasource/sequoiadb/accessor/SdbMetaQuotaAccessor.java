package com.sequoiacm.metasource.sequoiadb.accessor;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaQuotaAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class SdbMetaQuotaAccessor extends SdbMetaAccessor implements MetaQuotaAccessor {

    public SdbMetaQuotaAccessor(SdbMetaSource metasource, String csName, String clName) {
        super(metasource, csName, clName);
    }

    @Override
    public BSONObject getQuotaInfo(String type, String name) throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.Quota.TYPE, type);
        matcher.put(FieldName.Quota.NAME, name);
        return queryOne(matcher, null, null);
    }

    @Override
    public void updateQuotaInfo(String type, String name, int quotaRoundNumber, BSONObject updator)
            throws SdbMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.Quota.TYPE, type);
        matcher.put(FieldName.Quota.NAME, name);
        matcher.put(FieldName.Quota.QUOTA_ROUND_NUMBER, quotaRoundNumber);
        updator.put(FieldName.Quota.UPDATE_TIME, System.currentTimeMillis());
        update(matcher, new BasicBSONObject("$set", updator));
    }
}
