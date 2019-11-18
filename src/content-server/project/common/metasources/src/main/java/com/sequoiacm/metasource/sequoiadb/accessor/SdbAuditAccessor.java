package com.sequoiacm.metasource.sequoiadb.accessor;

import org.bson.BSONObject;

import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;

public class SdbAuditAccessor extends SdbMetaAccessor {

    public SdbAuditAccessor(SdbMetaSource metasource, String csName, String clName) {
        super(metasource, csName, clName);
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws SdbMetasourceException {
        // TODO Auto-generated method stub
        return super.query(matcher, selector, orderBy);
    }


}
