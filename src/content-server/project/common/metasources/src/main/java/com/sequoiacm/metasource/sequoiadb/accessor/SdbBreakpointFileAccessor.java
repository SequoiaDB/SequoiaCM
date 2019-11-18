package com.sequoiacm.metasource.sequoiadb.accessor;

import com.sequoiacm.metasource.MetaBreakpointFileAccessor;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class SdbBreakpointFileAccessor extends SdbMetaAccessor implements MetaBreakpointFileAccessor {
    public SdbBreakpointFileAccessor(SdbMetaSource metasource,
                                     String csName,
                                     String clName,
                                     TransactionContext context) {
        super(metasource, csName, clName, context);
    }

    public SdbBreakpointFileAccessor(SdbMetaSource metasource,
                                     String csName,
                                     String clName) {
        super(metasource, csName, clName);
    }

    @Override
    public void update(BSONObject matcher, BSONObject updater) throws SdbMetasourceException {
        super.update(matcher, updater);
    }

    @Override
    public void delete(String fileName) throws SdbMetasourceException {
        BSONObject matcher = new BasicBSONObject("file_name", fileName);
        super.delete(matcher);
    }


}
