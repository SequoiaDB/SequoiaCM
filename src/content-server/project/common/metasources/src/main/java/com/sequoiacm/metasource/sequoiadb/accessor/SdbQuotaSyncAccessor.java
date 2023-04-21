package com.sequoiacm.metasource.sequoiadb.accessor;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaQuotaSyncAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class SdbQuotaSyncAccessor extends SdbMetaAccessor implements MetaQuotaSyncAccessor {

    public SdbQuotaSyncAccessor(SdbMetaSource metasource, String csName, String clName) {
        super(metasource, csName, clName);
    }

    @Override
    public BSONObject getQuotaSyncInfo(String type, String name, int syncRoundNumber)
            throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.TYPE, type);
        matcher.put(FieldName.QuotaSync.NAME, name);
        matcher.put(FieldName.QuotaSync.SYNC_ROUND_NUMBER, syncRoundNumber);
        return queryOne(matcher, null, null);
    }

    @Override
    public void updateQuotaSyncInfo(String type, String name, int syncRoundNumber,
            BSONObject updator) throws SdbMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.TYPE, type);
        matcher.put(FieldName.QuotaSync.NAME, name);
        matcher.put(FieldName.QuotaSync.SYNC_ROUND_NUMBER, syncRoundNumber);
        updator.put(FieldName.QuotaSync.UPDATE_TIME, System.currentTimeMillis());
        update(matcher, new BasicBSONObject("$set", updator));
    }
}
